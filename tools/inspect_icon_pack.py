import json, zipfile
from pathlib import Path

P=Path("input/Pokemon-Minimap-Icons-HD-Complete.zip")
with zipfile.ZipFile(P) as z:
    names=z.namelist()
    defs=[n for n in names if n.startswith("assets/xaerominimap/entity/icon/definition/") and n.endswith(".json")]
    sprites=[n for n in names if n.startswith("assets/xaerominimap/entity/icon/sprite/") and n.lower().endswith(".png")]
    print("FILES",len(names),"DEFS",len(defs),"SPRITES",len(sprites))
    print("\n=== DEFINITIONS ===")
    for n in defs:
        raw=z.read(n).decode("utf-8","replace")
        print("\nFILE",n)
        print(raw)
    print("\n=== SAWSBUCK / DEERLING PATHS ===")
    for n in names:
        if "sawsbuck" in n.lower() or "deerling" in n.lower():
            print(n)
    print("\n=== XAERO NON-SPRITE PATHS ===")
    for n in names:
        if n.startswith("assets/xaerominimap/") and not n.lower().endswith(".png"):
            print(n)
    print("\n=== FIRST/LAST SPRITES ===")
    for n in sprites[:80]: print(n)
    print("...LAST...")
    for n in sprites[-80:]: print(n)
