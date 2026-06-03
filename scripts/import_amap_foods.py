"""
一次性脚本：从高德 POI 周边搜索接口抓取北邮西土城/沙河两个校区周边的餐饮店，
按 foods 表结构清洗后写入数据库，同时把 INSERT 备份到 scripts/import_amap_foods_backup.sql。

用法：
    pip install requests pymysql pyyaml
    python scripts/import_amap_foods.py

注意：
- Web 服务 Key 从 src/main/resources/application-local.yaml 读取 tourwise.amap.web-key
- 数据库连接：默认从 application.yaml + application-local.yaml 合并读取
- 重复运行安全：以 (name, address) 去重，不会写入重复条目
"""
from __future__ import annotations

import json
import os
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

import pymysql
import requests
import yaml

ROOT = Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "src" / "main" / "resources"
BACKUP_SQL = ROOT / "scripts" / "import_amap_foods_backup.sql"

AMAP_AROUND_URL = "https://restapi.amap.com/v3/place/around"
AMAP_TEXT_URL   = "https://restapi.amap.com/v3/place/text"
FOOD_TYPES = "050000"  # 餐饮服务大类
PAGE_SIZE = 25
MAX_PAGES = 8  # 每个校区每个半径最多翻 8 页 ≈ 200 条


@dataclass
class Campus:
    name: str         # 用于日志
    keyword: str      # 高德关键字搜索查询语
    city: str         # 高德 city 限定
    radius: int       # 抓取餐饮的半径(米)
    poi_id: int       # 数据库 pois 表对应的校区概览 POI id，导入时绑定到该 poi


# 不硬编码坐标，先用高德关键字搜索定位校区真实 POI，再围绕该坐标做周边搜索
# poi_id 关联到 pois 表的校区概览点，让前端"附近美食"接口能按 spotId/placeGroupId 匹配
CAMPUSES = [
    Campus(name="北邮西土城校区",
           keyword="北京邮电大学(西土城校区)",
           city="北京",
           radius=1500,
           poi_id=239),
    Campus(name="北邮沙河校区",
           keyword="北京邮电大学(沙河校区)",
           city="北京",
           radius=2000,
           poi_id=618),
]


def resolve_campus_location(key: str, campus: Campus) -> str | None:
    """通过高德关键字搜索获取校区 POI 的真实坐标。"""
    try:
        r = requests.get(AMAP_TEXT_URL, timeout=10, params={
            "key": key,
            "keywords": campus.keyword,
            "city": campus.city,
            "citylimit": "true",
            "offset": 5,
            "page": 1,
            "extensions": "base",
        })
        r.raise_for_status()
        data = r.json()
    except Exception as e:
        print(f"  [warn] {campus.name} 关键字搜索失败: {e}")
        return None
    if data.get("status") != "1":
        print(f"  [warn] {campus.name} 高德返回错误: {data.get('info')}")
        return None
    pois = data.get("pois") or []
    if not pois:
        print(f"  [warn] {campus.name} 关键字搜索无结果")
        return None
    # 选取第一个高校大类(类型码 141201/科教文化-高等院校)的命中；否则取首条
    chosen = next((p for p in pois if str(p.get("typecode", "")).startswith("1412")), pois[0])
    loc = chosen.get("location")
    addr = chosen.get("address") or chosen.get("adname")
    print(f"  {campus.name} -> POI 命中「{chosen.get('name')}」({addr})  坐标 {loc}")
    return loc


def load_yaml(path: Path) -> dict:
    if not path.exists():
        return {}
    with path.open("r", encoding="utf-8") as f:
        return yaml.safe_load(f) or {}


def deep_merge(base: dict, override: dict) -> dict:
    out = dict(base)
    for k, v in override.items():
        if isinstance(v, dict) and isinstance(out.get(k), dict):
            out[k] = deep_merge(out[k], v)
        else:
            out[k] = v
    return out


def load_config() -> dict:
    base = load_yaml(RESOURCES / "application.yaml")
    local = load_yaml(RESOURCES / "application-local.yaml")
    return deep_merge(base, local)


def get_amap_key(cfg: dict) -> str:
    key = (cfg.get("tourwise", {}).get("amap", {}) or {}).get("web-key")
    if not key:
        sys.exit("[ERROR] tourwise.amap.web-key 没配置，先在 application-local.yaml 里加上 Web 服务 Key")
    return key


_ENV_VAR_RE = __import__("re").compile(r"\$\{([^:}]+)(?::([^}]*))?\}")


def resolve_env(value: str) -> str:
    if not isinstance(value, str):
        return value
    return _ENV_VAR_RE.sub(lambda m: os.environ.get(m.group(1), m.group(2) or ""), value)


def get_db_conn(cfg: dict):
    ds = cfg.get("spring", {}).get("datasource", {}) or {}
    url = resolve_env(ds.get("url", ""))
    # 解析 jdbc:mysql://host:port/dbname?... 形式
    host, port, dbname = "127.0.0.1", 3306, "tourist_system"
    if url.startswith("jdbc:mysql://"):
        body = url[len("jdbc:mysql://"):]
        hostport, _, rest = body.partition("/")
        if ":" in hostport:
            host, port_str = hostport.split(":", 1)
            port = int(port_str)
        else:
            host = hostport
        dbname = rest.split("?", 1)[0] or dbname
    return pymysql.connect(
        host=host,
        port=port,
        user=ds.get("username", "root"),
        password=str(ds.get("password", "")),
        database=dbname,
        charset="utf8mb4",
        autocommit=False,
    )


def fetch_around(key: str, campus: Campus, location: str) -> list[dict]:
    items: dict[str, dict] = {}
    for page in range(1, MAX_PAGES + 1):
        params = {
            "key": key,
            "location": location,
            "radius": campus.radius,
            "types": FOOD_TYPES,
            "offset": PAGE_SIZE,
            "page": page,
            "extensions": "all",
        }
        try:
            r = requests.get(AMAP_AROUND_URL, params=params, timeout=10)
            r.raise_for_status()
            data = r.json()
        except Exception as e:
            print(f"  [warn] {campus.name} page {page} 请求失败: {e}")
            break
        if data.get("status") != "1":
            print(f"  [warn] {campus.name} page {page} 高德返回错误: {data.get('info')}")
            break
        pois = data.get("pois") or []
        if not pois:
            break
        for poi in pois:
            pid = poi.get("id")
            if pid and pid not in items:
                items[pid] = poi
        print(f"  {campus.name} page {page} -> 累计 {len(items)} 条")
        time.sleep(0.25)  # 礼貌一点
    return list(items.values())


def coerce_str(value) -> str:
    if value is None or value == [] or value == "":
        return ""
    if isinstance(value, list):
        return " ".join(str(v) for v in value if v)
    return str(value)


def price_level_from_cost(cost_str: str) -> int:
    try:
        cost = float(cost_str) if cost_str else 0
    except ValueError:
        cost = 0
    if cost <= 0:
        return 2
    if cost < 30:
        return 1
    if cost <= 80:
        return 2
    return 3


def rating_from(biz_ext: dict) -> float:
    try:
        v = float(biz_ext.get("rating") or 0)
    except (ValueError, TypeError):
        v = 0.0
    if v <= 0:
        return 4.5  # 高德没评分时给个合理默认
    return round(min(5.0, v), 1)


def avg_price_from(biz_ext: dict):
    try:
        v = float(biz_ext.get("cost") or 0)
    except (ValueError, TypeError):
        v = 0.0
    return round(v, 2) if v > 0 else None


def first_photo_url(poi: dict) -> str:
    photos = poi.get("photos") or []
    if isinstance(photos, list):
        for p in photos:
            url = (p or {}).get("url") if isinstance(p, dict) else None
            if url:
                return url
    return ""


def parse_location(loc: str) -> tuple[float | None, float | None]:
    if not loc or "," not in loc:
        return None, None
    try:
        lng_s, lat_s = loc.split(",", 1)
        return round(float(lng_s), 6), round(float(lat_s), 6)
    except ValueError:
        return None, None


def normalize(poi: dict, campus: Campus) -> dict:
    biz_ext = poi.get("biz_ext") or {}
    name = coerce_str(poi.get("name")).strip()
    address = coerce_str(poi.get("address")).strip()
    type_name = coerce_str(poi.get("type")).split(";")[-1].strip() or "餐饮"
    type_code = coerce_str(poi.get("typecode")).strip()
    desc = f"{type_name} · {address}" if address else type_name
    lng, lat = parse_location(coerce_str(poi.get("location")))
    return {
        "poi_id": campus.poi_id,
        "name": name,
        "address": address,
        "cuisine_code": type_code[:50],
        "cuisine_name": type_name[:50],
        "description": desc[:500],
        "image_url": first_photo_url(poi)[:255],
        "price_level": price_level_from_cost(coerce_str(biz_ext.get("cost"))),
        "avg_price": avg_price_from(biz_ext),
        "rating": rating_from(biz_ext),
        "phone": coerce_str(poi.get("tel"))[:30],
        "longitude": lng,
        "latitude": lat,
    }


INSERT_SQL = """
INSERT INTO foods (
  poi_id, name, cuisine_code, cuisine_name, description, image_url,
  price_level, avg_price, rating, hotness, address, longitude, latitude, phone, status
) VALUES (
  %(poi_id)s, %(name)s, %(cuisine_code)s, %(cuisine_name)s, %(description)s, %(image_url)s,
  %(price_level)s, %(avg_price)s, %(rating)s, 0, %(address)s, %(longitude)s, %(latitude)s, %(phone)s, 1
)
"""

UPDATE_COORDS_SQL = """
UPDATE foods
SET longitude = %(longitude)s,
    latitude  = %(latitude)s,
    poi_id    = COALESCE(poi_id, %(poi_id)s)
WHERE name = %(name)s AND IFNULL(address,'') = %(address)s AND (longitude IS NULL OR latitude IS NULL)
"""


def main():
    cfg = load_config()
    key = get_amap_key(cfg)
    conn = get_db_conn(cfg)

    all_records: list[dict] = []
    print("=== 第一步：用高德关键字搜索定位校区坐标 ===")
    located: list[tuple[Campus, str]] = []
    for campus in CAMPUSES:
        loc = resolve_campus_location(key, campus)
        if not loc:
            print(f"  [skip] {campus.name} 未能解析到坐标，跳过")
            continue
        located.append((campus, loc))

    if not located:
        sys.exit("[ERROR] 所有校区都未解析到坐标，终止")

    print("\n=== 校区坐标确认 ===")
    for campus, loc in located:
        print(f"  {campus.name}: {loc}  (radius={campus.radius})")
    print("如果坐标看起来不对，请 Ctrl+C 终止并检查 keyword 设置。5 秒后开始抓取餐饮…")
    time.sleep(5)

    print("\n=== 第二步：抓取餐饮 ===")
    for campus, loc in located:
        print(f"- {campus.name} @ {loc}")
        pois = fetch_around(key, campus, loc)
        normalized = [normalize(p, campus) for p in pois]
        normalized = [r for r in normalized if r["name"]]
        print(f"  -> 清洗后 {len(normalized)} 条")
        all_records.extend(normalized)

    # 跨校区去重 (name, address)
    seen = set()
    deduped = []
    for r in all_records:
        key2 = (r["name"], r["address"])
        if key2 in seen:
            continue
        seen.add(key2)
        deduped.append(r)
    print(f"\n=== 跨校区去重后 {len(deduped)} 条待写入 ===")

    if not deduped:
        print("没有可写入数据，结束")
        return

    inserted = 0
    skipped = 0
    updated = 0
    backup_lines = ["-- 高德 POI 周边搜索导入备份\n", "USE tourist_system;\n\n"]
    with conn.cursor() as cur:
        for r in deduped:
            r["address"] = r["address"] or ""
            cur.execute(
                "SELECT longitude, latitude FROM foods WHERE name=%s AND IFNULL(address,'')=%s LIMIT 1",
                (r["name"], r["address"]),
            )
            existing = cur.fetchone()
            if existing:
                old_lng, old_lat = existing
                if (old_lng is None or old_lat is None) and r["longitude"] and r["latitude"]:
                    cur.execute(UPDATE_COORDS_SQL, r)
                    updated += cur.rowcount
                else:
                    skipped += 1
                continue
            cur.execute(INSERT_SQL, r)
            inserted += 1
            backup_lines.append(
                "INSERT INTO foods (poi_id,name,cuisine_code,cuisine_name,description,image_url,price_level,avg_price,rating,hotness,address,phone,status) "
                f"VALUES ({r['poi_id']}, {json.dumps(r['name'], ensure_ascii=False)}, "
                f"{json.dumps(r['cuisine_code'], ensure_ascii=False)}, "
                f"{json.dumps(r['cuisine_name'], ensure_ascii=False)}, "
                f"{json.dumps(r['description'], ensure_ascii=False)}, "
                f"{json.dumps(r['image_url'], ensure_ascii=False)}, "
                f"{r['price_level']}, "
                f"{'NULL' if r['avg_price'] is None else r['avg_price']}, "
                f"{r['rating']}, 0, "
                f"{json.dumps(r['address'], ensure_ascii=False)}, "
                f"{json.dumps(r['phone'], ensure_ascii=False)}, 1);\n"
            )
    conn.commit()
    conn.close()

    BACKUP_SQL.write_text("".join(backup_lines), encoding="utf-8")
    print(f"\n=== 完成：新增 {inserted} 条，回填坐标 {updated} 条，已存在跳过 {skipped} 条 ===")
    print(f"备份 SQL 已写入: {BACKUP_SQL}")


if __name__ == "__main__":
    main()
