#!/usr/bin/env python3
import argparse
import json
import os
import shutil
import tempfile
import zipfile
from collections import defaultdict
from pathlib import Path

DEF = "assets/xaerominimap/entity/icon/definition/cobblemon/pokemon.json"
SPRITE_ROOT = "assets/xaerominimap/entity/icon/sprite/"
BUILDER = "dev.cobblemonbridge.client.XaeroPokemonVariantResolver.appendVariantId"
PREFIX = "cobblemon:textures/pokemon/"


def sprite_path(value: str):
    if not isinstance(value, str):
        return None
    if value.startswith("normal_sprite:"):
        return SPRITE_ROOT + value.split(":", 1)[1].lstrip("/")
    if value.startswith("outlined_sprite:"):
        return SPRITE_ROOT + value.split(":", 1)[1].lstrip("/")
    return None


def parse_species(key: str):
    if not key.startswith(PREFIX):
        return None
    rest = key[len(PREFIX):]
    if "/" not in rest:
        return None
    folder, filename = rest.split("/", 1)
    # Ignore the compatibility aliases that use numbered folders such as
    # 0586_sawsbuck. The canonical unnumbered entries are what the resolver
    # emits and what we use to create base fallbacks.
    if folder and folder[0].isdigit():
        return None
    return folder, filename


def choose_value(items, species, shiny):
    wanted = [
        f"{species}{'_shiny' if shiny else ''}.png",
        f"{species}_spring{'_shiny' if shiny else ''}.png",
        f"{species}_normal{'_shiny' if shiny else ''}.png",
    ]
    for filename in wanted:
        for key, value, actual in items:
            if actual == filename:
                return value

    pool = []
    for key, value, filename in items:
        is_shiny = "_shiny" in filename
        if is_shiny == shiny:
            pool.append((len(filename), filename, value))
    if not pool and shiny:
        for key, value, filename in items:
            if "_shiny" not in filename:
                pool.append((1000 + len(filename), filename, value))
    if not pool:
        return None
    pool.sort()
    return pool[0][2]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--input", required=True)
    ap.add_argument("--output", required=True)
    ap.add_argument("--report", default=None)
    args = ap.parse_args()

    source = Path(args.input)
    target = Path(args.output)
    report_path = Path(args.report) if args.report else target.with_suffix(".report.txt")

    with zipfile.ZipFile(source, "r") as zin:
        names = set(zin.namelist())
        if DEF not in names:
            raise SystemExit(f"Missing {DEF}")
        root = json.loads(zin.read(DEF).decode("utf-8"))
        variants = root.get("variants")
        if not isinstance(variants, dict):
            raise SystemExit("pokemon.json has no variants object")

        before = len(variants)
        groups = defaultdict(list)
        for key, value in variants.items():
            parsed = parse_species(key)
            if parsed:
                species, filename = parsed
                groups[species].append((key, value, filename))

        added_regular = []
        added_shiny = []
        missing_species = []

        for species in sorted(groups):
            items = groups[species]
            base_key = f"{PREFIX}{species}/{species}.png"
            shiny_key = f"{PREFIX}{species}/{species}_shiny.png"

            if base_key not in variants:
                value = choose_value(items, species, False)
                if value:
                    variants[base_key] = value
                    added_regular.append((species, base_key, value))
                else:
                    missing_species.append(species)

            if shiny_key not in variants:
                value = choose_value(items, species, True)
                if value:
                    variants[shiny_key] = value
                    added_shiny.append((species, shiny_key, value))

        default_added = False
        default_target = "normal_sprite:regular/0000_substitute.png"
        if "default" not in variants and sprite_path(default_target) in names:
            variants["default"] = default_target
            default_added = True

        root["variantIdBuilderMethod"] = BUILDER

        # Validate all sprite references against files actually present in the
        # source pack. The fixed pack is not emitted if any mapping is broken.
        missing_sprites = []
        for key, value in variants.items():
            path = sprite_path(value)
            if path and path not in names:
                missing_sprites.append((key, value, path))
        if missing_sprites:
            details = "\n".join(f"{k} -> {v} ({p})" for k, v, p in missing_sprites[:100])
            raise SystemExit(f"{len(missing_sprites)} missing Xaero sprite targets:\n{details}")

        saw_keys = sorted(k for k in variants if "sawsbuck" in k.lower())
        required_saw = [
            f"{PREFIX}sawsbuck/sawsbuck_{season}{suffix}.png"
            for season in ("spring", "summer", "autumn", "winter")
            for suffix in ("", "_shiny")
        ]
        missing_saw = [k for k in required_saw if k not in variants]
        if missing_saw:
            raise SystemExit("Sawsbuck seasonal variants missing: " + ", ".join(missing_saw))

        # Also guarantee canonical base aliases for Sawsbuck after the repair.
        for k in (
            f"{PREFIX}sawsbuck/sawsbuck.png",
            f"{PREFIX}sawsbuck/sawsbuck_shiny.png",
        ):
            if k not in variants:
                raise SystemExit("Sawsbuck base fallback not generated: " + k)

        payload = json.dumps(root, ensure_ascii=False, indent=2).encode("utf-8") + b"\n"

        target.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(target, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zout:
            for info in zin.infolist():
                name = info.filename
                if name == DEF:
                    continue
                # Remove IDE metadata accidentally embedded in the old pack.
                if "/.idea/" in name or name.endswith("/.idea"):
                    continue
                data = zin.read(name)
                new_info = zipfile.ZipInfo(name, date_time=info.date_time)
                new_info.compress_type = zipfile.ZIP_DEFLATED
                new_info.external_attr = info.external_attr
                new_info.create_system = info.create_system
                zout.writestr(new_info, data)
            zout.writestr(DEF, payload)

    with zipfile.ZipFile(target, "r") as check:
        check_names = set(check.namelist())
        check_root = json.loads(check.read(DEF).decode("utf-8"))
        check_variants = check_root["variants"]
        dangling = []
        for key, value in check_variants.items():
            path = sprite_path(value)
            if path and path not in check_names:
                dangling.append((key, path))
        if dangling:
            raise SystemExit(f"Output validation failed: {len(dangling)} dangling sprites")
        if check_root.get("variantIdBuilderMethod") != BUILDER:
            raise SystemExit("Output validation failed: wrong variant builder")

    lines = [
        "Pokemon Minimap Icons HD - global repair\n",
        f"Source variants: {before}\n",
        f"Final variants: {len(variants)}\n",
        f"Canonical species detected: {len(groups)}\n",
        f"Regular base fallbacks added: {len(added_regular)}\n",
        f"Shiny base fallbacks added: {len(added_shiny)}\n",
        f"Default unknown-species fallback added: {default_added}\n",
        f"Missing sprite references after repair: 0\n",
        f"Sawsbuck mappings after repair: {len(saw_keys)}\n",
        f"Variant builder: {BUILDER}\n",
        f"Output size: {target.stat().st_size} bytes\n",
    ]
    if missing_species:
        lines.append("Species without any usable regular mapping: " + ", ".join(missing_species) + "\n")
    report_path.write_text("".join(lines), encoding="utf-8")
    print("".join(lines))


if __name__ == "__main__":
    main()
