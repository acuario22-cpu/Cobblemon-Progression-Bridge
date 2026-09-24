import argparse, json, zipfile, hashlib, re
from pathlib import Path
from collections import Counter

GEN_FILES=[f"Generation {i}.zip" for i in range(1,10)]
ICON_PACK="Pokemon-Minimap-Icons-HD-Complete.zip"
MAPPING="assets/generations_core/textures/sprite_mapping.json"

def vals(x):
    if isinstance(x,dict):
        for v in x.values(): yield from vals(v)
    elif isinstance(x,list):
        for v in x: yield from vals(v)
    elif isinstance(x,str):
        yield x

def rpath(s):
    if ":" not in s: return s.lstrip("/")
    ns,p=s.split(":",1)
    return f"assets/{ns}/{p}"

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--input",default="input")
    ap.add_argument("--output",default="out")
    a=ap.parse_args()
    inp=Path(a.input); out=Path(a.output); out.mkdir(parents=True,exist_ok=True)
    lines=["# Xaero icon audit\n\n"]
    merged={}; origins={}; conflicts=[]; missing=[]
    for fn in GEN_FILES:
        p=inp/fn
        if not p.exists():
            lines.append(f"{fn}: MISSING\n"); continue
        with zipfile.ZipFile(p) as z:
            names=set(z.namelist())
            if MAPPING not in names:
                lines.append(f"{fn}: no mapping\n"); continue
            d=json.loads(z.read(MAPPING))
            lm=0
            for k,v in d.items():
                if k in merged and merged[k]!=v: conflicts.append((k,origins[k],fn))
                merged[k]=v; origins[k]=fn
                for s in vals(v):
                    if isinstance(s,str) and ":" in s and ("textures/" in s or s.endswith(".png")):
                        rp=rpath(s)
                        if rp not in names:
                            lm+=1; missing.append((fn,k,s,rp))
            lines.append(f"{fn}: keys={len(d)} missing_refs={lm}\n")
            if fn=="Generation 5.zip":
                saw={k:v for k,v in d.items() if "sawsbuck" in k.lower() or "deerling" in k.lower()}
                (out/"generation5-sawsbuck-deerling.json").write_text(json.dumps(saw,indent=2,ensure_ascii=False),encoding="utf-8")
    lines += [f"\nMerged unique keys: {len(merged)}\n",f"Conflicts: {len(conflicts)}\n",f"Missing mapping refs: {len(missing)}\n"]
    if missing:
        lines.append("\nMissing mapping refs detail:\n")
        for x in missing[:500]: lines.append(" | ".join(x)+"\n")
    (out/"merged-sprite_mapping.json").write_text(json.dumps(merged,indent=2,ensure_ascii=False),encoding="utf-8")

    p=inp/ICON_PACK
    if not p.exists():
        lines.append("\nICON PACK MISSING\n")
    else:
        with zipfile.ZipFile(p) as z:
            names=z.namelist(); nset=set(names)
            defs=[n for n in names if n.startswith("assets/xaerominimap/entity/icon/definition/") and n.endswith(".json")]
            sprites=[n for n in names if n.startswith("assets/xaerominimap/entity/icon/sprite/") and n.lower().endswith(".png")]
            lines += [f"\nIcon pack files: {len(names)}\n",f"Xaero definitions: {len(defs)}\n",f"Xaero sprites: {len(sprites)}\n"]
            methods=Counter(); cob=[]; refs=[]; parse_err=[]
            for n in defs:
                try:
                    raw=z.read(n).decode("utf-8"); d=json.loads(raw)
                except Exception as e:
                    parse_err.append((n,str(e))); continue
                if "cobblemon" in n.lower() or "pokemon" in n.lower() or "cobblemon" in raw.lower():
                    cob.append((n,raw))
                m=d.get("variantIdBuilderMethod")
                if m: methods[m]+=1
                for s in vals(d):
                    if "sprite:" in s:
                        refs.append((n,s.split("sprite:",1)[1]))
            lines.append(f"Cobblemon definitions: {len(cob)}\n")
            lines.append(f"Parse errors: {len(parse_err)}\n")
            lines.append("variantIdBuilderMethods:\n")
            for k,v in methods.items(): lines.append(f"{k} = {v}\n")
            for i,(n,raw) in enumerate(cob):
                lines.append(f"\nDEFINITION {n}\n{raw[:50000]}\n")
                (out/f"cobble-definition-{i}.json").write_text(raw,encoding="utf-8")
            mx=[]
            for n,rel in refs:
                expected="assets/xaerominimap/entity/icon/sprite/"+rel.lstrip("/")
                if expected not in nset: mx.append((n,rel,expected))
            lines.append(f"\nSprite refs: {len(refs)} missing={len(mx)}\n")
            for x in mx[:500]: lines.append(" | ".join(x)+"\n")
            saw=[n for n in names if "sawsbuck" in n.lower() or "deerling" in n.lower()]
            lines.append("\nSawsbuck/Deerling files:\n")
            for n in saw: lines.append(n+"\n")
            lines.append("\nDefinitions mentioning Sawsbuck/Deerling:\n")
            for n,raw in cob:
                if "sawsbuck" in raw.lower() or "deerling" in raw.lower():
                    lines.append(f"{n}\n{raw[:50000]}\n")
            # Any suspicious resolver/helper files
            lines.append("\nPotential resolver/helper files:\n")
            for n in names:
                low=n.lower()
                if any(x in low for x in ["resolver","variant","cobblemon","pokemon"]):
                    lines.append(n+"\n")
    report="".join(lines)
    (out/"audit-report.md").write_text(report,encoding="utf-8")
    print(report[:60000])

if __name__=="__main__":
    main()
