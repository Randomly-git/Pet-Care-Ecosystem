# -*- coding: utf-8 -*-
import requests, json, sys

BASE = "http://localhost:9000"
USERNAME = "活了一百万次的猫"
PASSWORD = "123"

r = requests.post(f"{BASE}/api/auth/login", json={"name": USERNAME, "password": PASSWORD})
data = r.json()
if not data.get("success"):
    print("登录失败:", data.get("message"))
    sys.exit(0)

token = data["data"]["token"]
userId = data["data"]["userId"]
print(f"登录成功: userId={userId}")
headers = {"Authorization": f"Bearer {token}"}

r = requests.get(f"{BASE}/api/pets/user/{userId}", headers=headers)
pets = r.json().get("data", [])
print(f"\n用户 [{USERNAME}] 有 {len(pets)} 只宠物:\n")

for pet in pets:
    pid = pet["petId"]
    print(f"  petId={pid}, name={pet['name']}, species={pet['species']}, breed={pet['breed']}")

    r2 = requests.get(f"{BASE}/api/activities/records/pet/{pid}?page=0&size=50", headers=headers)
    if r2.status_code == 200:
        records = r2.json().get("content", [])
        print(f"    活动记录: {len(records)} 条")
        for rec in records:
            dt = rec.get("activityDate", "")
            name = rec.get("activityName", "")
            desc = rec.get("activityDescription", "")
            kind = rec.get("activityKindName", "")
            bert = rec.get("bertResult")
            bertStr = f" BERT={bert}" if bert and bert != 0 else ""
            print(f"      - [{dt}] {name}({kind}) | {desc}{bertStr}")
    else:
        print(f"    获取活动记录失败: HTTP {r2.status_code}")

    r3 = requests.get(f"{BASE}/api/activities/records/pet/{pid}/abnormal", headers=headers)
    if r3.status_code == 200:
        ab = r3.json()
        if ab:
            print(f"    异常记录: {len(ab)} 条")
            for a in ab:
                print(f"      - [{a['activityDate']}] {a['activityName']} BERT={a['bertResult']} | {a['activityDescription']}")
    print()
