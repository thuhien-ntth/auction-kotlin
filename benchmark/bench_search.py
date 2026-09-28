import psycopg2, random, string, time, json, statistics
c=psycopg2.connect("host=localhost port=5544 user=postgres dbname=postgres"); c.autocommit=True; cur=c.cursor()
cur.execute("DROP DATABASE IF EXISTS catalog_db"); cur.execute("CREATE DATABASE catalog_db"); c.close()
c=psycopg2.connect("host=localhost port=5544 user=postgres dbname=catalog_db"); c.autocommit=True; cur=c.cursor()
cur.execute("""CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE TABLE events(id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name text, start_time timestamptz, end_time timestamptz);
CREATE TABLE products (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), seller_id UUID NOT NULL, event_id UUID NULL,
 title VARCHAR(255) NOT NULL, description TEXT NOT NULL DEFAULT '', category VARCHAR(100) NOT NULL DEFAULT 'OTHER',
 start_price NUMERIC(14,2) NOT NULL, status VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL',
 created_at timestamptz DEFAULT now(), updated_at timestamptz DEFAULT now());
CREATE INDEX idx_products_seller ON products(seller_id);
CREATE INDEX idx_products_event ON products(event_id);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_title_trgm ON products USING gin (title gin_trgm_ops);""")
random.seed(1)
words="Leica Canon Nikon Sony Rolex Omega Seiko Casio Vintage Antique Camera Watch Painting Vase Bronze Statue Guitar Piano Violin Bicycle Motorbike Laptop Phone Lens Tripod Ceramic Silver Gold Jade Carpet Lamp Clock Coin Stamp Book Poster Sculpture".split()
cats=["ELECTRONICS","WATCHES","ART","VEHICLES","ANTIQUES"]
import io
buf=io.StringIO()
N=200000
for i in range(N):
    t=" ".join(random.choice(words) for _ in range(random.randint(3,6)))+" "+"".join(random.choices(string.ascii_uppercase+string.digits,k=5))
    st="ACTIVE" if random.random()<0.3 else random.choice(["PENDING_APPROVAL","APPROVED","REJECTED","SOLD","ENDED_NO_BID"])
    buf.write(f"{random.getrandbits(128):032x}\t{t}\t{random.choice(cats)}\t{random.randint(100000,90000000)}\t{st}\n")
buf.seek(0)
cur.execute("CREATE TEMP TABLE stg(sid text,title text,cat text,price numeric,st text)")
cur.copy_from(buf,'stg',columns=('sid','title','cat','price','st'))
cur.execute("INSERT INTO products(seller_id,title,category,start_price,status) SELECT gen_random_uuid(),title,cat,price,st FROM stg")
cur.execute("ANALYZE products")
cur.execute("SELECT count(*), count(*) FILTER (WHERE status='ACTIVE') FROM products"); print("rows",cur.fetchone())
def bench(sql,params,reps=30):
    ts=[]
    for _ in range(reps):
        t=time.perf_counter(); cur.execute(sql,params); cur.fetchall(); ts.append((time.perf_counter()-t)*1000)
    ts.sort(); return statistics.mean(ts), ts[int(.95*len(ts))-1]
def plan(sql,params):
    cur.execute("EXPLAIN (ANALYZE, BUFFERS) "+sql,params); rows=[r[0] for r in cur.fetchall()]
    return rows
kws=["leica","rolex omega","antique","zz9","bronze statue"]
Q_CUR="SELECT * FROM products p WHERE p.status='ACTIVE' AND (LOWER(p.title) LIKE LOWER(CONCAT('%%',%s,'%%'))) ORDER BY p.id LIMIT 10 OFFSET 0"
Q_CNT="SELECT count(*) FROM products p WHERE p.status='ACTIVE' AND (LOWER(p.title) LIKE LOWER(CONCAT('%%',%s,'%%')))"
Q_ILIKE="SELECT * FROM products p WHERE p.status='ACTIVE' AND p.title ILIKE CONCAT('%%',%s,'%%') ORDER BY p.id LIMIT 10 OFFSET 0"
Q_ILIKE_CNT="SELECT count(*) FROM products p WHERE p.status='ACTIVE' AND p.title ILIKE CONCAT('%%',%s,'%%')"
out={}
def measure(tag):
    r={}
    for k in kws:
        a=bench(Q_CUR,(k,)); b=bench(Q_CNT,(k,)); i=bench(Q_ILIKE,(k,)); ic=bench(Q_ILIKE_CNT,(k,))
        r[k]={"cur_page":a,"cur_count":b,"ilike_page":i,"ilike_count":ic}
    out[tag]=r
measure("A_current_index_on_title")
out["plan_A_current"]=plan(Q_CNT,("leica",))
out["plan_A_ilike"]=plan(Q_ILIKE_CNT,("leica",))
cur.execute("CREATE INDEX idx_products_title_lower_trgm ON products USING gin (LOWER(title) gin_trgm_ops)"); cur.execute("ANALYZE products")
measure("B_index_on_lower_title")
out["plan_B_current"]=plan(Q_CNT,("leica",))
cur.execute("DROP INDEX idx_products_title_lower_trgm"); cur.execute("ANALYZE products")
json.dump(out,open("res_search.json","w"),indent=1,default=str)
print("ok")
