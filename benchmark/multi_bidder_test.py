"""
Test nhiều người cùng đấu giá 1 sản phẩm qua API thật (đi qua gateway :8080).

Cách dùng (chạy trong thư mục be, docker compose đang chạy):
    python ..\\benchmark\\multi_bidder_test.py --product <PRODUCT_ID> --users 20 --rounds 5

Script sẽ:
  1. Tạo sẵn N tài khoản test bidderN@test.local (đã xác thực, mật khẩu "Passw0rd!") thẳng vào auth_db
     qua `docker compose exec postgres psql` — bỏ qua bước xác thực email.
  2. Đăng nhập tất cả để lấy token.
  3. Cho N người cùng lúc (mỗi người 1 thread) đặt giá `--rounds` lần: đọc giá hiện tại rồi trả +step.
  4. Kiểm tra tính đúng đắn: giá cuối = bid hợp lệ cao nhất, các bid hợp lệ tăng dần, không có 2 bid hợp lệ cùng giá.

Chỉ dùng thư viện chuẩn của Python (không cần pip install).
Sản phẩm phải đang ở trạng thái ACTIVE (đang trong thời gian đấu giá) và không thuộc các tài khoản test.
"""
import argparse, json, subprocess, threading, time, urllib.request, urllib.error
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal

PASSWORD_HASH = "$2a$10$R/ItNCB1YICxXdtdwtfW6uzEKIdHlcBvdl1h/1Dtvgt2aiEHXh.Rm"  # = "Passw0rd!" (giống V2__seed.sql)


def http(method, url, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, json.loads(r.read() or b"null")
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"message": raw.decode(errors="replace")}
    except Exception as e:
        return 0, {"message": str(e)}


def create_users(n):
    values = ",".join(
        f"('bidder{i}@test.local','{PASSWORD_HASH}','Test Bidder {i}',FALSE,TRUE)" for i in range(1, n + 1)
    )
    sql = ("INSERT INTO users (email,password_hash,full_name,is_admin,is_verified) VALUES "
           + values + " ON CONFLICT (email) DO NOTHING;")
    r = subprocess.run(["docker", "compose", "exec", "-T", "postgres", "psql", "-U", "auction", "-d", "auth_db", "-c", sql],
                       capture_output=True, text=True)
    if r.returncode != 0:
        raise SystemExit("Không tạo được user test (hãy chạy script trong thư mục be):\n" + r.stderr)
    print(f"[1] Đã đảm bảo có {n} tài khoản bidder1..bidder{n}@test.local")


def login_all(base, n):
    tokens = {}
    def one(i):
        st, body = http("POST", f"{base}/auth/login", {"email": f"bidder{i}@test.local", "password": "Passw0rd!"})
        if st != 200:
            raise SystemExit(f"Login bidder{i} lỗi {st}: {body}")
        return i, body["accessToken"]
    with ThreadPoolExecutor(20) as ex:
        for i, t in ex.map(one, range(1, n + 1)):
            tokens[i] = t
    print(f"[2] Đăng nhập xong {len(tokens)} tài khoản")
    return tokens


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://localhost:8080/api")
    ap.add_argument("--product", required=True, help="ID sản phẩm đang ACTIVE")
    ap.add_argument("--users", type=int, default=20)
    ap.add_argument("--rounds", type=int, default=5, help="số lần đặt giá mỗi người")
    ap.add_argument("--step", default="10000", help="bước giá cộng thêm mỗi lần")
    a = ap.parse_args()

    create_users(a.users)
    tokens = login_all(a.base, a.users)

    st, d = http("GET", f"{a.base}/products/{a.product}")
    if st != 200:
        raise SystemExit(f"Không đọc được sản phẩm: {st} {d}")
    print(f"    Sản phẩm: {d['title']} | trạng thái {d['status']} | giá hiện tại {d['currentPrice']} {d.get('currency','')}")
    if d["status"] != "ACTIVE":
        print("    CẢNH BÁO: sản phẩm không ở trạng thái ACTIVE, mọi bid sẽ bị từ chối.")

    step = Decimal(a.step)
    results = Counter()
    messages = Counter()
    latencies = []
    lock = threading.Lock()
    start_gate = threading.Barrier(a.users)

    def bidder(i):
        tok = tokens[i]
        start_gate.wait()  # tất cả cùng xuất phát 1 lúc
        for _ in range(a.rounds):
            _, p = http("GET", f"{a.base}/products/{a.product}", token=tok)
            amount = Decimal(str(p.get("currentPrice", 0))) + step
            t0 = time.perf_counter()
            st, body = http("POST", f"{a.base}/products/{a.product}/bids", {"amount": str(amount)}, tok)
            dt = (time.perf_counter() - t0) * 1000
            with lock:
                latencies.append(dt)
                accepted = st == 200 and body.get("accepted")
                key = "accepted" if accepted else f"HTTP {st}"
                results[key] += 1
                if not accepted:
                    messages[(st, body.get("message", ""))] += 1

    print(f"[3] {a.users} người x {a.rounds} lượt đặt giá cùng lúc...")
    t0 = time.perf_counter()
    with ThreadPoolExecutor(a.users) as ex:
        list(ex.map(bidder, range(1, a.users + 1)))
    total = time.perf_counter() - t0

    lat = sorted(latencies)
    print(f"\n=== KẾT QUẢ ({total:.1f}s, {len(lat)} request, {len(lat)/total:.1f} req/s) ===")
    for k, v in results.most_common():
        print(f"  {k:12} {v}")
    for (st, msg), v in messages.most_common(8):
        print(f"    - [{st}] {msg} (x{v})")
    if lat:
        print(f"  Latency: p50={lat[len(lat)//2]:.0f}ms  p95={lat[int(len(lat)*0.95)-1]:.0f}ms  max={lat[-1]:.0f}ms")

    # ---- Kiểm tra tính đúng đắn ----
    _, d2 = http("GET", f"{a.base}/products/{a.product}")
    _, hist = http("GET", f"{a.base}/products/{a.product}/bids?size=2000")
    acc = [Decimal(str(b["amount"])) for b in hist.get("content", []) if b["accepted"]]
    acc_chrono = list(reversed(acc))  # API trả mới nhất trước
    final = Decimal(str(d2["currentPrice"]))
    ok_max = (not acc) or final == max(acc)
    ok_inc = all(x < y for x, y in zip(acc_chrono, acc_chrono[1:]))
    ok_dup = len(acc) == len(set(acc))
    print("\n=== KIỂM TRA ===")
    print(f"  Giá cuối {final} = bid hợp lệ cao nhất: {'OK' if ok_max else 'SAI'}")
    print(f"  Các bid hợp lệ tăng dần theo thời gian:   {'OK' if ok_inc else 'SAI'}")
    print(f"  Không có 2 bid hợp lệ trùng giá:          {'OK' if ok_dup else 'SAI'}")


if __name__ == "__main__":
    main()
