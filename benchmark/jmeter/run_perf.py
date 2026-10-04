"""
Bộ đo hiệu năng end-to-end (JMeter qua API Gateway) cho mục 6 - Performance Evaluation.
Phiên bản đo LẶP: mỗi cấu hình chạy nhiều lần, thứ tự xen kẽ/xáo trộn, có warm-up, nghỉ giữa các lần,
tổng hợp trung bình ± SD và khoảng tin cậy 95 %.

CÁCH CHẠY (PowerShell trong thư mục C:\\Users\\thuhi\\Project\\mobile, Docker Desktop đang bật):
    python benchmark\\jmeter\\run_perf.py all                      # đo chính thức (mặc định, ~2 giờ)
    python benchmark\\jmeter\\run_perf.py all --reps 3 --bid-reps 5  # rút gọn (~1 giờ 15)
    python benchmark\\jmeter\\run_perf.py all --quick              # chạy thử 1 lần/cấu hình (~15 phút)
    python benchmark\\jmeter\\run_perf.py all --resume results\\<thư mục>   # chạy tiếp lần đo bị gián đoạn
    python benchmark\\jmeter\\run_perf.py read|gateway|bid         # chỉ 1 kịch bản
    python benchmark\\jmeter\\run_perf.py summarize results\\<thư mục>      # tổng hợp lại

KỊCH BẢN
  read    (KB1): GET /api/products/{id} qua Gateway, 25/50/100/200 thread, cache currentPrice BẬT và TẮT.
  gateway (KB2): cùng tải 100 thread, gọi thẳng catalog :8082 và qua Gateway :8080, thứ tự ABBA.
  bid     (KB3): 50/100/200 bidder đặt giá CÙNG LÚC (Synchronizing Timer) trên 1 sản phẩm mới mỗi lần,
                 sau đó đối chiếu bidding_db để kiểm tra tính đúng đắn.

KẾT QUẢ: benchmark/jmeter/results/<thời gian>/
  summary.md   bảng trung bình ± SD, CV, khoảng tin cậy 95 %, so sánh cặp (dán vào báo cáo)
  summary.csv  số liệu tổng hợp theo cấu hình;  runs.csv  từng lần chạy (kèm thứ tự, thời điểm)
  *.jtl        dữ liệu thô JMeter;  dashboards/ HTML dashboard JMeter;  commands.txt  câu lệnh chính xác
  docker_stats.csv  CPU/RAM container;  env.json  máy + tham số;  run_log.txt  nhật ký
Chỉ dùng thư viện chuẩn Python.
"""
import argparse, csv, json, math, os, platform, random, statistics, subprocess, sys, threading, time
import urllib.request, urllib.error
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from pathlib import Path

HERE = Path(__file__).resolve().parent
BE_DIR = HERE.parent.parent / "be"
DEFAULT_JMETER = r"C:\Users\thuhi\Downloads\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat"
PASSWORD = "Passw0rd!"
PASSWORD_HASH = "$2a$10$R/ItNCB1YICxXdtdwtfW6uzEKIdHlcBvdl1h/1Dtvgt2aiEHXh.Rm"  # = "Passw0rd!" (giống V2__seed.sql)
GATEWAY = "http://localhost:8080"
CATALOG_DIRECT = "http://localhost:8082"
START_PRICE = 1_000_000
BID_STEP = 1_000
TOKEN_MAX_AGE_S = 80 * 60          # JWT sống 120 phút; làm mới sớm để không hết hạn giữa chừng
ERROR_LIMIT_PCT = 1.0              # lần chạy có tỉ lệ lỗi > 1 % bị đánh dấu "bất thường" (vẫn giữ trong dữ liệu)

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

LOG_FILE = None


def log(msg):
    line = f"[{datetime.now():%H:%M:%S}] {msg}"
    print(line, flush=True)
    if LOG_FILE:
        with open(LOG_FILE, "a", encoding="utf-8") as f:
            f.write(line + "\n")


# ---------------------------------------------------------------- docker / db helpers
def compose(*args, env_extra=None, check=True):
    cmd = ["docker", "compose", "-f", "docker-compose.yml", "-f", str(HERE / "docker-compose.perf.yml"), *args]
    env = os.environ.copy()
    env.setdefault("PERF_CACHE_TTL", "3")
    if env_extra:
        env.update(env_extra)
    r = subprocess.run(cmd, cwd=BE_DIR, capture_output=True, text=True, encoding="utf-8", errors="replace", env=env)
    if check and r.returncode != 0:
        raise SystemExit(f"Lệnh lỗi: {' '.join(cmd)}\n{r.stdout}\n{r.stderr}")
    return r.stdout


def psql(db, sql):
    return compose("exec", "-T", "postgres", "psql", "-U", "auction", "-d", db,
                   "-qtA", "-v", "ON_ERROR_STOP=1", "-c", sql).strip()


def http(method, url, body=None, token=None, timeout=15):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            raw = r.read()
            return r.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        return e.code, None
    except Exception:
        return 0, None


def wait_ok(url, token=None, timeout=600, ok=(200,)):
    t0 = time.time()
    while time.time() - t0 < timeout:
        st, _ = http("GET", url, token=token, timeout=5)
        if st in ok:
            return
        time.sleep(3)
    raise SystemExit(f"Hết {timeout}s mà {url} vẫn chưa sẵn sàng. Kiểm tra `docker compose ps` / logs.")


# ---------------------------------------------------------------- seed data / tokens
def seed_users(n):
    values = ",".join(f"('bidder{i}@test.local','{PASSWORD_HASH}','Test Bidder {i}',FALSE,TRUE)" for i in range(1, n + 1))
    psql("auth_db", "INSERT INTO users (email,password_hash,full_name,is_admin,is_verified) VALUES "
         + values + " ON CONFLICT (email) DO NOTHING;")
    log(f"Đã có {n} tài khoản bidder1..bidder{n}@test.local")


class Tokens:
    """Đăng nhập n tài khoản -> tokens.csv; tự làm mới khi token sắp hết hạn."""
    def __init__(self, n, path):
        self.n, self.path, self.at, self.first = n, path, 0.0, None

    def ensure(self):
        if time.time() - self.at < TOKEN_MAX_AGE_S:
            return

        def one(i):
            st = 0
            for _ in range(5):
                st, body = http("POST", f"{GATEWAY}/api/auth/login",
                                {"email": f"bidder{i}@test.local", "password": PASSWORD}, timeout=30)
                if st == 200:
                    return body["accessToken"]
                time.sleep(2)
            raise SystemExit(f"Login bidder{i} thất bại (HTTP {st})")
        log(f"Đăng nhập {self.n} tài khoản lấy JWT...")
        with ThreadPoolExecutor(8) as ex:
            toks = list(ex.map(one, range(1, self.n + 1)))
        self.path.write_text("\n".join(toks) + "\n", encoding="utf-8")
        self.at, self.first = time.time(), toks[0]
        log(f"   xong -> {self.path.name}")


def new_product(tag):
    pid = psql("catalog_db",
               "INSERT INTO products (seller_id,title,description,category,start_price,status,auction_start_at,auction_end_at) "
               f"VALUES (gen_random_uuid(),'PERF {tag}','perf test','OTHER',{START_PRICE},'ACTIVE',"
               "now() - interval '1 minute', now() + interval '6 hours') RETURNING id;")
    return pid.splitlines()[0].strip()


def set_cache(args, enabled, tokens, pid):
    ttl = "3" if enabled else "0"
    log(f"Khởi động lại catalog-service với cache {'BẬT (TTL 3s)' if enabled else 'TẮT'}...")
    compose("up", "-d", "catalog-service", env_extra={"PERF_CACHE_TTL": ttl})
    time.sleep(5)
    tokens.ensure()
    wait_ok(f"{CATALOG_DIRECT}/products/{pid}", tokens.first)
    wait_ok(f"{GATEWAY}/api/products/{pid}", tokens.first)


# ---------------------------------------------------------------- docker stats sampler
class StatsSampler:
    def __init__(self, path):
        self.label, self.stop_ev = "idle", threading.Event()
        new = not path.exists()
        self.f = open(path, "a", newline="", encoding="utf-8")
        self.w = csv.writer(self.f)
        if new:
            self.w.writerow(["time", "run", "container", "cpu_percent", "mem_usage"])
        self.t = threading.Thread(target=self.loop, daemon=True)
        self.t.start()

    def loop(self):
        while not self.stop_ev.is_set():
            r = subprocess.run(["docker", "stats", "--no-stream", "--format", "{{.Name}};{{.CPUPerc}};{{.MemUsage}}"],
                               capture_output=True, text=True, encoding="utf-8", errors="replace")
            now = datetime.now().strftime("%H:%M:%S")
            for line in r.stdout.splitlines():
                p = line.split(";")
                if len(p) == 3:
                    self.w.writerow([now, self.label, p[0], p[1].rstrip("%"), p[2]])
            self.f.flush()
            self.stop_ev.wait(3)

    def close(self):
        self.stop_ev.set()
        self.t.join(timeout=15)
        self.f.close()


# ---------------------------------------------------------------- jmeter
def run_jmeter(args, jmx, jtl, props, dashboard=False):
    jtl.unlink(missing_ok=True)
    cmd = [args.jmeter, "-n", "-t", str(HERE / jmx), "-l", str(jtl), "-j", str(jtl.with_suffix(".log")),
           "-Jjmeter.save.saveservice.output_format=csv"] + [f"-J{k}={v}" for k, v in props.items()]
    if dashboard:  # HTML dashboard chuẩn của JMeter (evidence)
        dash = jtl.parent / "dashboards" / jtl.stem
        dash.parent.mkdir(exist_ok=True)
        if dash.exists():
            import shutil
            shutil.rmtree(dash)
        cmd += ["-e", "-o", str(dash)]
    if not jtl.name.startswith("_"):
        with open(jtl.parent / "commands.txt", "a", encoding="utf-8") as f:
            f.write(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] " + subprocess.list2cmdline(cmd) + "\n")
    r = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r.returncode != 0 or not jtl.exists():
        raise SystemExit(f"JMeter lỗi ({jtl.name}):\n{r.stdout[-2000:]}\n{r.stderr[-2000:]}")


def read_jtl(path, skip_ms=0):
    with open(path, newline="", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    if not rows:
        return []
    t0 = min(int(r["timeStamp"]) for r in rows)
    return [r for r in rows if int(r["timeStamp"]) >= t0 + skip_ms]


def pct(sorted_vals, p):
    if not sorted_vals:
        return float("nan")
    k = max(0, math.ceil(p / 100 * len(sorted_vals)) - 1)
    return sorted_vals[k]


def jtl_stats(rows):
    if not rows:
        return {"samples": 0}
    el = sorted(int(r["elapsed"]) for r in rows)
    start = min(int(r["timeStamp"]) for r in rows)
    end = max(int(r["timeStamp"]) + int(r["elapsed"]) for r in rows)
    wall = max(end - start, 1) / 1000
    errors = sum(1 for r in rows if r["success"] != "true")
    return {"samples": len(rows), "avg_ms": round(statistics.mean(el), 1), "p50_ms": pct(el, 50),
            "p95_ms": pct(el, 95), "p99_ms": pct(el, 99), "max_ms": el[-1],
            "throughput_rps": round(len(rows) / wall, 1), "error_pct": round(100 * errors / len(rows), 2),
            "wall_s": round(wall, 2)}


class Ctx:
    def __init__(self, args, out, runs, sampler, tokens):
        self.args, self.out, self.runs, self.sampler, self.tokens = args, out, runs, sampler, tokens
        self.done = {r["file"] for r in runs}
        self.seq = len(runs)
        self.dash_done = {r.get("group") for r in runs if r.get("dashboard")}

    def record(self, row):
        self.seq += 1
        row["seq"] = self.seq
        self.runs.append(row)
        (self.out / "runs.json").write_text(json.dumps(self.runs, ensure_ascii=False, indent=1), encoding="utf-8")

    def want_dash(self, group):
        if self.args.dashboards == "all":
            return True
        if self.args.dashboards == "none" or group in self.dash_done:
            return False
        self.dash_done.add(group)
        return True

    def cooldown(self):
        if self.args.cooldown > 0:
            time.sleep(self.args.cooldown)


# ---------------------------------------------------------------- scenarios
def read_warmup(c, pid, port=8080, basepath="/api/products"):
    wt = c.args.warmup_threads or max(c.args.read_threads + [c.args.gw_threads])
    log(f"   warm-up đọc {c.args.warmup}s ở {wt} thread (không ghi kết quả)")
    run_jmeter(c.args, "product_read.jmx", c.out / "_warmup.jtl",
               {"port": port, "basepath": basepath, "productId": pid, "tokens": c.out / "tokens.csv",
                "threads": wt, "rampup": 10, "duration": c.args.warmup})


def one_read_run(c, name, group, cfg, t, rep, pid, port, basepath, rampup, hold, scenario):
    if name + ".jtl" in c.done:
        return
    c.tokens.ensure()
    log(f"{name}: {t} thread, ramp {rampup}s + giữ tải {hold}s")
    c.sampler.label = name
    jtl = c.out / f"{name}.jtl"
    dash = c.want_dash(group)
    started = datetime.now().isoformat(timespec="seconds")
    run_jmeter(c.args, "product_read.jmx", jtl,
               {"port": port, "basepath": basepath, "productId": pid, "tokens": c.out / "tokens.csv",
                "threads": t, "rampup": rampup, "duration": rampup + hold}, dashboard=dash)
    c.sampler.label = "idle"
    s = jtl_stats(read_jtl(jtl, skip_ms=rampup * 1000))
    c.record({"scenario": scenario, "group": group, "config": cfg, "threads": t, "rep": rep, "file": jtl.name,
              "started": started, "dashboard": dash, "anomaly": s.get("error_pct", 100) > ERROR_LIMIT_PCT, **s})
    log(f"   -> {s['throughput_rps']} req/s, avg {s['avg_ms']} ms, p95 {s['p95_ms']} ms, lỗi {s['error_pct']}%"
        + ("  [BẤT THƯỜNG]" if s.get("error_pct", 100) > ERROR_LIMIT_PCT else ""))
    c.cooldown()


def scenario_read(c):
    a = c.args
    rng = random.Random(a.seed)
    for cache_on in a.cache_modes:
        cfg = f"cache {'on' if cache_on else 'off'}"
        pending = [f"read_cache{'On' if cache_on else 'Off'}_t{t}_r{rep}" for t in a.read_threads for rep in range(1, a.reps + 1)]
        if all(p + ".jtl" in c.done for p in pending):
            continue
        pid = new_product(f"read {cfg}")
        set_cache(a, cache_on, c.tokens, pid)
        read_warmup(c, pid)
        for rep in range(1, a.reps + 1):                 # lặp ngoài = lần lặp; trong mỗi lần: các mức tải xáo trộn
            levels = list(a.read_threads)
            rng.shuffle(levels)
            for t in levels:
                name = f"read_cache{'On' if cache_on else 'Off'}_t{t}_r{rep}"
                one_read_run(c, name, f"read|{cfg}|{t}", cfg, t, rep, pid, 8080, "/api/products",
                             a.read_rampup, a.read_hold, "read")
    if False in a.cache_modes:
        set_cache(a, True, c.tokens, new_product("restore"))  # trả về cấu hình mặc định


def scenario_gateway(c):
    a = c.args
    pid = new_product("gateway")
    c.tokens.ensure()
    wait_ok(f"{CATALOG_DIRECT}/products/{pid}", c.tokens.first)
    read_warmup(c, pid)
    read_warmup(c, pid, port=8082, basepath="/products")
    t = a.gw_threads
    for rep in range(1, a.gw_reps + 1):
        order = [("direct", 8082, "/products"), ("gateway", 8080, "/api/products")]
        if rep % 2 == 0:
            order.reverse()                              # ABBA: đảo thứ tự mỗi lần lặp
        for label, port, bp in order:
            one_read_run(c, f"gw_{label}_t{t}_r{rep}", f"gateway|{label}|{t}", label, t, rep, pid, port, bp,
                         10, a.gw_hold, "gateway")


def check_bid_correctness(pid, rows, n):
    codes = {}
    for r in rows:
        codes[r["responseCode"]] = codes.get(r["responseCode"], 0) + 1
    res = psql("bidding_db",
               "SELECT s.current_price, "
               f"(SELECT max(amount) FROM bids WHERE product_id='{pid}' AND accepted), "
               f"(SELECT count(*) FROM bids WHERE product_id='{pid}' AND accepted), "
               f"(SELECT count(*) FROM bids WHERE product_id='{pid}' AND NOT accepted) "
               f"FROM auction_state s WHERE s.product_id='{pid}';")
    ok200, ok409 = codes.get("200", 0), codes.get("409", 0)
    other = len(rows) - ok200 - ok409
    if not res:
        return {"http_200": ok200, "http_409": ok409, "http_other": other, "correct": False}
    price, max_acc, n_acc, n_rej = res.split("|")
    expected = START_PRICE + n * BID_STEP
    correct = (float(price) == float(max_acc or 0)
               and int(n_acc) == ok200 and int(n_rej) == ok409
               and (other > 0 or float(price) == expected))
    return {"http_200": ok200, "http_409": ok409, "http_other": other, "final_price": price,
            "expected_price": expected, "db_accepted": int(n_acc), "db_rejected": int(n_rej), "correct": correct}


def bid_once(c, t, jtl, dashboard=False):
    pid = new_product(f"bid {jtl.stem}")
    c.tokens.ensure()
    http("GET", f"{GATEWAY}/api/products/{pid}", token=c.tokens.first)
    run_jmeter(c.args, "bid_concurrent.jmx", jtl,
               {"port": 8080, "productId": pid, "tokens": c.out / "tokens.csv", "threads": t,
                "base": START_PRICE, "step": BID_STEP}, dashboard=dashboard)
    return pid


def scenario_bid(c):
    a = c.args
    rng = random.Random(a.seed + 1)
    log(f"   warm-up đường đặt giá: {a.bid_warmup} đợt x 50 bidder (không ghi kết quả)")
    for _ in range(a.bid_warmup):
        bid_once(c, 50, c.out / "_warmup_bid.jtl")
        time.sleep(2)
    for rep in range(1, a.bid_reps + 1):
        levels = list(a.bid_threads)
        rng.shuffle(levels)
        for t in levels:
            name = f"bid_t{t}_r{rep}"
            if name + ".jtl" in c.done:
                continue
            log(f"{name}: {t} bidder đặt giá cùng lúc")
            c.sampler.label = name
            jtl = c.out / f"{name}.jtl"
            group = f"bid|optimistic|{t}"
            dash = c.want_dash(group)
            started = datetime.now().isoformat(timespec="seconds")
            pid = bid_once(c, t, jtl, dashboard=dash)
            c.sampler.label = "idle"
            rows = read_jtl(jtl)
            s = jtl_stats(rows)
            chk = check_bid_correctness(pid, rows, t)
            c.record({"scenario": "bid", "group": group, "config": "optimistic", "threads": t, "rep": rep,
                      "file": jtl.name, "started": started, "dashboard": dash,
                      "anomaly": chk["http_other"] > 0, **s, **chk})
            log(f"   -> đợt xử lý xong trong {s['wall_s']} s, avg {s['avg_ms']} ms, p95 {s['p95_ms']} ms | "
                f"200={chk['http_200']} 409={chk['http_409']} khác={chk['http_other']} | "
                f"đúng đắn: {'OK' if chk['correct'] else 'SAI'}")
            time.sleep(max(3, a.cooldown // 2))


# ---------------------------------------------------------------- statistics / summary
T95 = {1: 12.706, 2: 4.303, 3: 3.182, 4: 2.776, 5: 2.571, 6: 2.447, 7: 2.365, 8: 2.306, 9: 2.262, 10: 2.228,
       11: 2.201, 12: 2.179, 13: 2.160, 14: 2.145, 15: 2.131, 16: 2.120, 17: 2.110, 18: 2.101, 19: 2.093,
       20: 2.086, 25: 2.060, 30: 2.042}


def t95(df):
    if df <= 0:
        return float("nan")
    if df in T95:
        return T95[df]
    return 2.042 if df < 60 else 1.96


def describe(vals):
    vals = [v for v in vals if v is not None and not (isinstance(v, float) and math.isnan(v))]
    n = len(vals)
    if n == 0:
        return {}
    m = statistics.mean(vals)
    sd = statistics.stdev(vals) if n > 1 else 0.0
    ci = t95(n - 1) * sd / math.sqrt(n) if n > 1 else float("nan")
    return {"n": n, "mean": m, "sd": sd, "min": min(vals), "max": max(vals),
            "cv_pct": (100 * sd / m) if m else float("nan"), "ci95": ci}


def f1(x, nd=1):
    return "–" if x is None or (isinstance(x, float) and math.isnan(x)) else f"{x:,.{nd}f}".replace(",", " ")


def summarize(out):
    runs = json.loads((out / "runs.json").read_text(encoding="utf-8"))
    keys = []
    for r in runs:
        for k in r:
            if k not in keys:
                keys.append(k)
    with open(out / "runs.csv", "w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=keys)
        w.writeheader()
        w.writerows(runs)

    groups = {}
    for r in runs:
        groups.setdefault((r["scenario"], r["config"], r["threads"]), []).append(r)
    metrics = ("throughput_rps", "avg_ms", "p50_ms", "p95_ms", "p99_ms", "max_ms", "error_pct", "wall_s")
    agg = []
    for (sc, cfg, t), rs in sorted(groups.items(), key=lambda kv: (kv[0][0], kv[0][1], kv[0][2])):
        row = {"scenario": sc, "config": cfg, "threads": t, "runs": len(rs),
               "anomalies": sum(1 for r in rs if r.get("anomaly"))}
        for k in metrics:
            d = describe([r.get(k) for r in rs])
            for s in ("mean", "sd", "ci95", "cv_pct", "min", "max"):
                row[f"{k}_{s}"] = round(d[s], 2) if d and not math.isnan(d[s]) else ""
        if sc == "bid":
            row["accepted_mean"] = round(statistics.mean(r.get("http_200", 0) for r in rs), 2)
            row["rejected_mean"] = round(statistics.mean(r.get("http_409", 0) for r in rs), 2)
            row["other_total"] = sum(r.get("http_other", 0) for r in rs)
            row["correct_runs"] = sum(1 for r in rs if r.get("correct"))
        agg.append(row)
    fields = []
    for r in agg:
        for k in r:
            if k not in fields:
                fields.append(k)
    with open(out / "summary.csv", "w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        w.writerows(agg)

    env = json.loads((out / "env.json").read_text(encoding="utf-8")) if (out / "env.json").exists() else {}
    L = [f"# Kết quả đo hiệu năng end-to-end ({out.name})", "",
         f"Môi trường: {env.get('host_cpu', '?')} | {env.get('host_logical_cpus', '?')} CPU logic | "
         f"Docker: {env.get('docker_cpus', '?')} CPU, {env.get('docker_mem_gb', '?')} GB RAM. JMeter chạy cùng máy.",
         "Ký hiệu: TB ± SD (n lần chạy); CI95 = nửa độ rộng khoảng tin cậy 95 % của trung bình (phân phối t); "
         "CV = SD/TB. Hai cấu hình được coi là khác nhau có ý nghĩa khi khoảng TB ± CI95 của chúng không chồng nhau.", ""]

    def mstr(r, k, nd=1):
        return f"{f1(r.get(k + '_mean'), nd)} ± {f1(r.get(k + '_sd'), nd)}"

    rs = [r for r in agg if r["scenario"] == "read"]
    if rs:
        L += ["## KB1 — Đọc chi tiết sản phẩm qua Gateway (vùng tải ổn định)", "",
              "| Cache | Thread | n | Throughput (req/s) | CI95 | CV | Avg (ms) | p95 (ms) | CI95 p95 | p99 (ms) | Lỗi TB (%) | Lần bất thường |",
              "|---|---|---|---|---|---|---|---|---|---|---|---|"]
        for r in sorted(rs, key=lambda r: (r["threads"], r["config"])):
            L.append(f"| {r['config']} | {r['threads']} | {r['runs']} | {mstr(r, 'throughput_rps')} | ±{f1(r['throughput_rps_ci95'])} | "
                     f"{f1(r['throughput_rps_cv_pct'])} % | {mstr(r, 'avg_ms')} | {mstr(r, 'p95_ms', 0)} | ±{f1(r['p95_ms_ci95'], 0)} | "
                     f"{mstr(r, 'p99_ms', 0)} | {f1(r['error_pct_mean'], 2)} | {r['anomalies']} |")
        by = {(r["config"], r["threads"]): r for r in rs}
        lines = []
        for t in sorted({r["threads"] for r in rs}):
            on, off = by.get(("cache on", t)), by.get(("cache off", t))
            if on and off and off["throughput_rps_mean"]:
                ci = lambda r: r["throughput_rps_ci95"] or 0
                lines.append(f"| {t} | ×{on['throughput_rps_mean'] / off['throughput_rps_mean']:.2f} | "
                             f"{100 * (on['p95_ms_mean'] / off['p95_ms_mean'] - 1):+.0f} % | "
                             f"{'có' if on['runs'] > 1 and off['runs'] > 1 and on['throughput_rps_mean'] - ci(on) > off['throughput_rps_mean'] + ci(off) else 'CHƯA (cần ≥ 2 lần/cấu hình)' if min(on['runs'], off['runs']) < 2 else 'CHƯA'} |")
        if lines:
            L += ["", "So sánh cache BẬT / TẮT:", "", "| Thread | Throughput BẬT/TẮT | p95 BẬT so với TẮT | Khác biệt có ý nghĩa (CI95 không chồng) |",
                  "|---|---|---|---|"] + lines
        L.append("")

    rs = [r for r in agg if r["scenario"] == "gateway"]
    if rs:
        L += ["## KB2 — Overhead API Gateway (cùng tải, thứ tự ABBA)", "",
              "| Đường đi | Thread | n | Throughput (req/s) | CI95 | Avg (ms) | CI95 | p95 (ms) | Lỗi TB (%) |",
              "|---|---|---|---|---|---|---|---|---|"]
        for r in rs:
            L.append(f"| {r['config']} | {r['threads']} | {r['runs']} | {mstr(r, 'throughput_rps')} | ±{f1(r['throughput_rps_ci95'])} | "
                     f"{mstr(r, 'avg_ms')} | ±{f1(r['avg_ms_ci95'])} | {mstr(r, 'p95_ms', 0)} | {f1(r['error_pct_mean'], 2)} |")
        # so sánh cặp theo từng lần lặp (cùng rep = cùng điều kiện máy)
        g = [r for r in runs if r["scenario"] == "gateway"]
        pairs = {}
        for r in g:
            pairs.setdefault(r["rep"], {})[r["config"]] = r
        d_avg = [p["gateway"]["avg_ms"] - p["direct"]["avg_ms"] for p in pairs.values() if "gateway" in p and "direct" in p]
        d_tp = [100 * (p["gateway"]["throughput_rps"] / p["direct"]["throughput_rps"] - 1) for p in pairs.values()
                if "gateway" in p and "direct" in p and p["direct"]["throughput_rps"]]
        if len(d_avg) >= 2:
            da, dt = describe(d_avg), describe(d_tp)
            sig_a = abs(da["mean"]) > da["ci95"]
            sig_t = abs(dt["mean"]) > dt["ci95"]
            L += ["", f"So sánh cặp theo từng lần lặp (n = {da['n']}):", "",
                  f"- Chênh lệch avg (Gateway − thẳng): **{da['mean']:+.1f} ms**, CI95 ±{da['ci95']:.1f} ms → "
                  + ("có ý nghĩa thống kê." if sig_a else "KHÔNG có ý nghĩa thống kê (khoảng tin cậy chứa 0)."),
                  f"- Chênh lệch throughput: **{dt['mean']:+.1f} %**, CI95 ±{dt['ci95']:.1f} % → "
                  + ("có ý nghĩa thống kê." if sig_t else "KHÔNG có ý nghĩa thống kê (khoảng tin cậy chứa 0).")]
        L.append("")

    rs = [r for r in agg if r["scenario"] == "bid"]
    if rs:
        L += ["## KB3 — Đặt giá đồng thời trên 1 sản phẩm (mỗi lần 1 sản phẩm mới)", "",
              "| Bidder | n | Thời gian xử lý cả đợt (s) | Avg (ms) | CI95 | p95 (ms) | Max (ms) | Chấp nhận TB | Từ chối TB | Lỗi khác (tổng) | Đúng đắn |",
              "|---|---|---|---|---|---|---|---|---|---|---|"]
        for r in rs:
            L.append(f"| {r['threads']} | {r['runs']} | {mstr(r, 'wall_s', 2)} | {mstr(r, 'avg_ms', 0)} | ±{f1(r['avg_ms_ci95'], 0)} | "
                     f"{mstr(r, 'p95_ms', 0)} | {f1(r['max_ms_max'], 0)} | {f1(r['accepted_mean'])} | {f1(r['rejected_mean'])} | "
                     f"{r['other_total']} | {r['correct_runs']}/{r['runs']} |")
        L.append("")
    (out / "summary.md").write_text("\n".join(L), encoding="utf-8")
    log(f"Tổng hợp xong: {out / 'summary.md'}")
    print("\n" + "\n".join(L))


# ---------------------------------------------------------------- main
def write_env(out, args):
    info = {"time": datetime.now().isoformat(timespec="seconds"), "host_cpu": platform.processor(),
            "host_os": platform.platform(), "host_logical_cpus": os.cpu_count(),
            "args": {k: v for k, v in vars(args).items()}}
    r = subprocess.run(["docker", "info", "--format", "{{.NCPU}};{{.MemTotal}}"], capture_output=True, text=True)
    if r.returncode == 0 and ";" in r.stdout:
        c, m = r.stdout.strip().split(";")
        info["docker_cpus"], info["docker_mem_gb"] = int(c), round(int(m) / 2**30, 1)
    info["mem_limit_services"] = os.environ.get("PERF_MEM_LIMIT", "384m")
    (out / "env.json").write_text(json.dumps(info, ensure_ascii=False, indent=2, default=str), encoding="utf-8")


def ints(s):
    return [int(x) for x in s.split(",") if x.strip()]


def estimate_min(a, todo):
    m = 0
    if "read" in todo:
        per = a.read_rampup + a.read_hold + 20 + a.cooldown
        m += len(a.cache_modes) * (a.reps * len(a.read_threads) * per + 240 + a.warmup)
    if "gateway" in todo:
        m += a.gw_reps * 2 * (10 + a.gw_hold + 20 + a.cooldown) + 2 * a.warmup
    if "bid" in todo:
        m += a.bid_reps * len(a.bid_threads) * 25 + a.bid_warmup * 20
    return round(m / 60)


def main():
    global LOG_FILE
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("what", choices=["all", "read", "gateway", "bid", "summarize"])
    ap.add_argument("dir", nargs="?", help="(summarize) thư mục kết quả")
    ap.add_argument("--jmeter", default=os.environ.get("JMETER", DEFAULT_JMETER))
    ap.add_argument("--build", action="store_true", help="build lại image trước khi đo")
    ap.add_argument("--resume", help="chạy tiếp vào thư mục kết quả đã có (bỏ qua các lần đã xong)")
    ap.add_argument("--quick", action="store_true", help="chạy thử: 1 lần/cấu hình, ít mức tải")
    ap.add_argument("--reps", type=int, default=5, help="số lần lặp mỗi cấu hình KB1 (mặc định 5)")
    ap.add_argument("--gw-reps", type=int, default=None, help="số lần lặp KB2 (mặc định = --reps)")
    ap.add_argument("--bid-reps", type=int, default=10, help="số lần lặp mỗi mức KB3 (mặc định 10)")
    ap.add_argument("--read-threads", type=ints, default=[25, 50, 100, 200])
    ap.add_argument("--read-rampup", type=int, default=20)
    ap.add_argument("--read-hold", type=int, default=60)
    ap.add_argument("--gw-threads", type=int, default=100)
    ap.add_argument("--gw-hold", type=int, default=60)
    ap.add_argument("--bid-threads", type=ints, default=[50, 100, 200])
    ap.add_argument("--warmup", type=int, default=180,
                    help="giây warm-up tải đọc sau mỗi lần khởi động lại (mặc định 180; đo 30/09 cho thấy 60 s là chưa đủ)")
    ap.add_argument("--warmup-threads", type=int, default=0, help="số thread warm-up (mặc định = mức tải cao nhất)")
    ap.add_argument("--bid-warmup", type=int, default=3, help="số đợt warm-up đặt giá (không ghi kết quả)")
    ap.add_argument("--cooldown", type=int, default=10, help="giây nghỉ giữa 2 lần chạy")
    ap.add_argument("--seed", type=int, default=2026, help="hạt giống xáo trộn thứ tự (để tái lập)")
    ap.add_argument("--dashboards", choices=["first", "all", "none"], default="first",
                    help="tạo HTML dashboard JMeter: first = lần đầu mỗi cấu hình (mặc định)")
    ap.add_argument("--cache-modes", type=lambda s: [m.strip() == "on" for m in s.split(",")], default=[True, False],
                    help="on,off (mặc định) | on")
    args = ap.parse_args()

    if args.what == "summarize":
        if not args.dir:
            raise SystemExit("Cần đường dẫn thư mục kết quả")
        return summarize(Path(args.dir).resolve())

    if args.quick:
        args.reps, args.bid_reps, args.read_threads, args.read_hold, args.gw_hold = 1, 1, [50, 200], 30, 30
        args.bid_threads, args.warmup, args.bid_warmup, args.cooldown = [50, 200], 20, 1, 5
    if args.gw_reps is None:
        args.gw_reps = max(args.reps, 2) if not args.quick else 1
    if not Path(args.jmeter).exists():
        raise SystemExit(f"Không thấy JMeter ở {args.jmeter} (dùng --jmeter <đường dẫn jmeter.bat>)")

    if args.resume:
        out = Path(args.resume).resolve()
        if not out.exists():
            raise SystemExit(f"Không có thư mục {out}")
    else:
        out = HERE / "results" / datetime.now().strftime("%Y%m%d_%H%M%S")
        out.mkdir(parents=True)
        write_env(out, args)
    LOG_FILE = out / "run_log.txt"
    todo = ["read", "gateway", "bid"] if args.what == "all" else [args.what]
    log(f"Thư mục kết quả: {out}")
    log(f"Kịch bản: {', '.join(todo)} | KB1 {args.reps} lần x {len(args.read_threads)} mức x {len(args.cache_modes)} cấu hình cache | "
        f"KB2 {args.gw_reps} cặp | KB3 {args.bid_reps} lần x {len(args.bid_threads)} mức | ước tính ~{estimate_min(args, todo)} phút")

    log("Khởi động hệ thống (docker compose up -d)" + (" --build" if args.build else ""))
    compose("up", "-d", *(["--build"] if args.build else []))
    wait_ok(f"{GATEWAY}/api/products?size=1", ok=(200, 401, 403))

    n_users = max(max(args.bid_threads), 50)
    seed_users(n_users)
    tokens = Tokens(n_users, out / "tokens.csv")
    tokens.ensure()

    runs = json.loads((out / "runs.json").read_text(encoding="utf-8")) if (out / "runs.json").exists() else []
    if runs:
        log(f"Chạy tiếp: đã có {len(runs)} lần chạy, sẽ bỏ qua các lần này")
    sampler = StatsSampler(out / "docker_stats.csv")
    ctx = Ctx(args, out, runs, sampler, tokens)
    t0 = time.time()
    try:
        for sc in todo:
            log(f"===== Kịch bản {sc} =====")
            {"read": scenario_read, "gateway": scenario_gateway, "bid": scenario_bid}[sc](ctx)
    finally:
        sampler.close()
        for p in ("_warmup.jtl", "_warmup.log", "_warmup_bid.jtl", "_warmup_bid.log"):
            (out / p).unlink(missing_ok=True)
    log(f"Đo xong sau {round((time.time() - t0) / 60, 1)} phút")
    if runs:
        summarize(out)


if __name__ == "__main__":
    main()
