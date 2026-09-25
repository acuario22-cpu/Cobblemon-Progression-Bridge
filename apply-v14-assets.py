from pathlib import Path
import base64

root = Path("modern-backport/src/main/resources/assets/cobblemon_modern_backport")
assets = {
    "textures/item/blank_tm.png": "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAgklEQVR4nGNgoBAwInNYOXj+E6Pp948vcH1wBisHz38+EWmibP305incEBZ0Sc7karyav89tReFjGMDAwMBgF+2JVfOhpdsxxJjwWkcEGAYGYA1EbIFFkgHbEt2wKvaavwtDjGIvYKTEy51scEnd8l9YNSGnROq5AOYKYjQhZyaKAQA19iQ/z/EFEAAAAABJRU5ErkJggg=="
}
for rel, data in assets.items():
    path = root / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(base64.b64decode(data))
print("v1.4 textures installed:", len(assets))
