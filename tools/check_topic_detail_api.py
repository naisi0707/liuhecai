# -*- coding: utf-8 -*-
import json
import urllib.request

url = "http://127.0.0.1:8080/api/topics/3000000000000000001"
req = urllib.request.Request(url, headers={"Host": "127.0.0.1"})
with urllib.request.urlopen(req, timeout=10) as resp:
    body = json.loads(resp.read().decode("utf-8"))
data = body.get("data") or {}
print("code", body.get("code"))
print("keys", sorted(data.keys()))
print("playType", data.get("playType"))
print("viewCount", data.get("viewCount"))
print("purchaseCount", data.get("purchaseCount"))
print("previewLen", len(data.get("previewContent") or ""))
print("contentVisible", data.get("contentVisible"))
print("prev", data.get("prevTopicId"), (data.get("prevTopicTitle") or "")[:40])
print("next", data.get("nextTopicId"), (data.get("nextTopicTitle") or "")[:40])
