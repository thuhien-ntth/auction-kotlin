"""
Protocol-level benchmark của luồng đặt giá (BidTransactionExecutor + BiddingService) trên
đúng schema V1__init.sql của bidding-service, chạy trên PostgreSQL 16 thật.
Mô phỏng: HikariCP pool = 10 (mặc định), Tomcat worker = 200 (mặc định).
Ba chiến lược: naive (không khoá), optimistic (CAS theo version + retry<=5, backoff 5-25ms),
pessimistic (SELECT ... FOR UPDATE).
"""
import sys, time, uuid, random, threading, json, statistics
import psycopg2
from concurrent.futures import ThreadPoolExecutor

DSN = "host=localhost port=5544 user=postgres dbname=bidding_db"
SCHEMA = """
DROP TABLE IF EXISTS bids; DROP TABLE IF EXISTS auction_state;
CREATE TABLE auction_state (
 product_id UUID PRIMARY KEY, seller_id UUID NOT NULL, current_price NUMERIC(14,2) NOT NULL,
 current_bidder_id UUID NULL, auction_end_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE bids (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 product_id UUID NOT NULL REFERENCES auction_state(product_id), bidder_id UUID NOT NULL,
 amount NUMERIC(14,2) NOT NULL, accepted BOOLEAN NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE INDEX idx_bids_product ON bids(product_id, created_at DESC);
CREATE INDEX idx_bids_bidder ON bids(bidder_id);
CREATE INDEX idx_auction_state_current_bidder ON auction_state(current_bidder_id);
"""

class Pool:
    def __init__(self, size):
        self.q = [psycopg2.connect(DSN) for _ in range(size)]
        for c in self.q: c.autocommit = False
        self.sem = threading.Semaphore(size); self.lock = threading.Lock()
    def get(self):
        self.sem.acquire()
        with self.lock: return self.q.pop()
    def put(self, c):
        with self.lock: self.q.append(c)
        self.sem.release()
    def close(self):
        for c in self.q: c.close()

class Conflict(Exception): pass   # 409 nghiệp vụ
class OptLock(Exception): pass    # ObjectOptimisticLockingFailureException

def attempt(pool, mode, pid, bidder, amount):
    c = pool.get()
    try:
        cur = c.cursor()
        if mode == "pessimistic":
            cur.execute("SELECT current_price, seller_id, version FROM auction_state WHERE product_id=%s FOR UPDATE", (pid,))
        else:
            cur.execute("SELECT current_price, seller_id, version FROM auction_state WHERE product_id=%s", (pid,))
        price, seller, ver = cur.fetchone()
        valid = amount > price and bidder != str(seller)
        if not valid:
            c.rollback()                       # ResponseStatusException -> rollback (Spring mặc định)
            raise Conflict()
        cur.execute("INSERT INTO bids(product_id,bidder_id,amount,accepted) VALUES(%s,%s,%s,true)", (pid, bidder, amount))
        if mode == "optimistic":
            cur.execute("UPDATE auction_state SET current_price=%s,current_bidder_id=%s,version=version+1,updated_at=now() "
                        "WHERE product_id=%s AND version=%s", (amount, bidder, pid, ver))
            if cur.rowcount == 0:
                c.rollback(); raise OptLock()
        else:
            cur.execute("UPDATE auction_state SET current_price=%s,current_bidder_id=%s,updated_at=now() WHERE product_id=%s",
                        (amount, bidder, pid))
        c.commit()
    except (Conflict, OptLock):
        raise
    except Exception:
        c.rollback(); raise
    finally:
        pool.put(c)

def place_bid(pool, mode, pid, bidder, amount, max_retry=5):
    tries = 0
    while True:
        tries += 1
        try:
            attempt(pool, mode, pid, bidder, amount); return "ok", tries
        except Conflict:
            return "rejected", tries
        except OptLock:
            if tries >= max_retry: return "fail500", tries
            time.sleep(random.uniform(0.005, 0.025))

def run(mode, n_threads, n_products=1, pool_size=10, workers=200, seed=0, order='random'):
    random.seed(seed)
    admin = psycopg2.connect(DSN); admin.autocommit = True; a = admin.cursor()
    a.execute(SCHEMA)
    pids = [str(uuid.uuid4()) for _ in range(n_products)]
    seller = str(uuid.uuid4()); base = 1_000_000; step = 1000
    for p in pids:
        a.execute("INSERT INTO auction_state(product_id,seller_id,current_price,auction_end_at) VALUES(%s,%s,%s,now()+interval '2 hours')",
                  (p, seller, base))
    pool = Pool(pool_size)
    plan = [(pids[i % n_products], str(uuid.uuid4()), base + (i + 1) * step) for i in range(n_threads)]
    if order=='random': random.shuffle(plan)
    else: plan.sort(key=lambda x:x[2])
    start = threading.Event(); res = []; lock = threading.Lock()
    def worker(item):
        pid, bidder, amount = item
        start.wait()
        t0 = time.perf_counter()
        out, tries = place_bid(pool, mode, pid, bidder, amount)
        dt = (time.perf_counter() - t0) * 1000
        with lock: res.append((out, tries, dt))
    ex = ThreadPoolExecutor(max_workers=min(workers, n_threads))
    futs = [ex.submit(worker, it) for it in plan]
    time.sleep(0.5)
    T0 = time.perf_counter(); start.set()
    for f in futs: f.result()
    wall = time.perf_counter() - T0
    ex.shutdown()
    # đối chiếu correctness
    ok_products = 0; lost = 0
    for p in pids:
        a.execute("SELECT current_price FROM auction_state WHERE product_id=%s", (p,)); sp = a.fetchone()[0]
        a.execute("SELECT max(amount), count(*) FROM bids WHERE product_id=%s AND accepted", (p,)); mx, cnt = a.fetchone()
        mysub = max(x[2] for x in plan if x[0] == p)
        if sp == mysub: ok_products += 1
        if mx is not None and mx > sp: lost += 1
    lat = sorted(r[2] for r in res)
    pct = lambda q: lat[min(len(lat) - 1, int(q * len(lat)))]
    outc = [r[0] for r in res]
    pool.close(); admin.close()
    return dict(mode=mode, order=order, n=n_threads, products=n_products, pool=pool_size,
                ok=outc.count("ok"), rejected=outc.count("rejected"), fail500=outc.count("fail500"),
                retries_avg=statistics.mean(r[1] - 1 for r in res),
                retries_max=max(r[1] - 1 for r in res),
                avg_ms=statistics.mean(lat), p95_ms=pct(0.95), p99_ms=pct(0.99),
                throughput=len(res) / wall, wall_s=wall,
                final_price_correct=ok_products == n_products, lost_update_products=lost)

if __name__ == "__main__":
    exps = json.loads(sys.argv[1]); reps = int(sys.argv[2]); out = []
    for e in exps:
        for r in range(reps):
            m = run(seed=r, **e); m["rep"] = r; out.append(m)
            print(json.dumps(m), flush=True)
    json.dump(out, open(sys.argv[3], "w"))
