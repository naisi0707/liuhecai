import base64
from pathlib import Path
out = []
for s in ["s/bK28z7", "vqvGt8z7", "yMjDxcz7", "zca89sz7"]:
    raw = base64.b64decode(s + "=" * ((4 - len(s) % 4) % 4))
    out.append(f"{s}={raw.decode('gbk')}")
Path(r"G:/part-time/liuhecai/mirror/decoded/tags.txt").write_text("\n".join(out), encoding="utf-8")
print(out)
