"""Build staged .mrpack files for the "Grounded" NeoForge survival pack.

Resolves the newest NeoForge file for the target Minecraft version of every mod (and its required
dependencies) from the Modrinth API, then writes one .mrpack per stage.
Each stage contains everything from the stages before it.

Usage:  python build_pack.py [26.2|1.21.1]   -> writes dist/<version>/*.mrpack and lock.json (default 26.2)
For 26.2 it also writes the public release, dist/not-so-vanilla-<RELEASE_VERSION>.mrpack (the full last stage).
Import the .mrpack into Prism Launcher or the Modrinth App.
"""
import hashlib, json, os, sys, urllib.request, urllib.parse, zipfile

# Target profiles. Default is the newest viable version; pass "1.21.1" for the fallback.
PROFILES = {
    "26.2": {
        "neoforge": "26.2.0.93",
        "stages": [
            ("1-medical", "Grounded S1 - Medical",
             ["med-system", "jei", "sodium", "ferrite-core", "entityculling", "immediatelyfast", "distanthorizons", "lithium", "enhancedvisuals"], ["tweaks"]),  # no AppleSkin; tweaks = slower walking. Chunky removed: with Distant Horizons 3.3.3 it hangs new-world creation on 26.2
            ("2-survival", "Grounded S2 - Survival",
             ["cold-sweat", "thirst-was-taken-2", "serene-seasons", "glitchcore", "crop-and-kettle", "spice-of-life-onion",
              "true-darkness-by-tia", "dungeons-and-taverns", "betterdays", "saros-fruit-trees", "towns-and-towers", "sparsestructures", "aquaculture", "ecologics", "terralith"], []),  # longer seasonal days; fruit trees (worldgen fixed in Grounded Survival)
            ("3-sound-ai", "Grounded S3 - Sound AI",
             ["attract-to-sound", "in-control", "iris", "shaderpack:complementary-reimagined",
              "minecraft-comes-alive-reborn", "naturalist", "cameraoverhaul", "3dskinlayers", "not-enough-animations", "variants-and-ventures", "naturally-trimmed", "e4mc", "sound-physics-remastered", "ambientsounds"], ["incontrol", "shaders", "addons"]),  # addons = Grounded Inventory + Grounded Bandits + Grounded Survival jars
        ],
        "alt": None,  # Hostile Tactics has no 26.2 build
    },
    "1.21.1": {
        "neoforge": "21.1.266",
        "stages": [
            ("1-medical", "Grounded S1 - Medical",
             ["med-system", "jei", "appleskin", "sodium", "ferrite-core", "modernfix", "entityculling", "immediatelyfast", "distanthorizons"], []),
            ("2-survival", "Grounded S2 - Survival",
             ["cold-sweat", "thirst-was-taken", "serene-seasons", "farmers-delight", "spice-of-life-onion"], []),
            ("3a-sound-ai", "Grounded S3a - Sound AI",
             ["attract-to-sound", "in-control", "sound-physics-remastered", "ambientsounds"], ["incontrol"]),
        ],
        # Alternative AI stage: built on stage 2, NOT on 3a (overlapping perception systems).
        "alt": ("3b-tactics-ai", "Grounded S3b - Tactics AI", ["hostile-tactics", "in-control", "sound-physics-remastered", "ambientsounds"], ["incontrol"]),
    },
}
MC = sys.argv[1] if len(sys.argv) > 1 else "26.2"
LOADER = "neoforge"
NEOFORGE = PROFILES[MC]["neoforge"]
STAGES = PROFILES[MC]["stages"]
ALT_STAGE = PROFILES[MC]["alt"]
UA = "grounded-modpack-builder/1.0"
# Public release published on Modrinth (built from the last 26.2 stage). Bump for every upload.
RELEASE_NAME = "Not So Vanilla"
RELEASE_VERSION = "1.0.2"
RELEASE_SUMMARY = "Minecraft, but you're 9 again. The night is scary, the world is huge, and you don't know how anything works."
ROOT = os.path.dirname(os.path.abspath(__file__))
DIST = os.path.join(ROOT, "dist", MC)


def api(path, **q):
    url = "https://api.modrinth.com/v2" + path
    if q:
        url += "?" + urllib.parse.urlencode({k: json.dumps(v) for k, v in q.items()})
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


_cache = {}


def resolve(slug_or_id, out, chain=()):
    """Add the best version of a project (and its required deps) to `out`."""
    if slug_or_id in _cache:
        pid = _cache[slug_or_id]
        if pid in out:
            return
    folder = "mods"
    if slug_or_id.startswith("shaderpack:"):
        slug_or_id, folder = slug_or_id.split(":", 1)[1], "shaderpacks"
        versions = api(f"/project/{slug_or_id}/version", game_versions=[MC])
    else:
        versions = api(f"/project/{slug_or_id}/version", loaders=[LOADER], game_versions=[MC])
    if not versions:
        sys.exit(f"ERROR: {slug_or_id} has no {LOADER} {MC} build (needed by {' -> '.join(chain) or 'pack'})")
    releases = [v for v in versions if v["version_type"] == "release"]
    v = (releases or versions)[0]
    pid = v["project_id"]
    _cache[slug_or_id] = pid
    if pid in out:
        return
    proj = api(f"/project/{pid}")
    f = next((x for x in v["files"] if x["primary"]), v["files"][0])
    out[pid] = {
        "slug": proj["slug"], "title": proj["title"], "version": v["version_number"],
        "type": v["version_type"], "published": v["date_published"][:10],
        "file": f["filename"], "url": f["url"], "size": f["size"], "hashes": f["hashes"],
        "required_by": chain[-1] if chain else None, "folder": folder,
    }
    for d in v["dependencies"]:
        if d["dependency_type"] == "required" and d.get("project_id"):
            resolve(d["project_id"], out, chain + (proj["slug"],))


def write_pack(stem, name, mods, override_dirs, path=None, summary=None):
    os.makedirs(DIST, exist_ok=True)
    index = {
        "formatVersion": 1, "game": "minecraft", "versionId": stem, "name": name,
        "summary": summary or "Grounded, Tarkov-inspired survival for NeoForge " + MC + " (staged build)",
        "files": [{
            "path": f"{m.get('folder', 'mods')}/{m['file']}",
            "hashes": {"sha1": m["hashes"]["sha1"], "sha512": m["hashes"]["sha512"]},
            # Singleplayer runs an integrated server, so server-only mods are needed client-side too.
            "env": {"client": "required", "server": "unsupported" if m.get("folder") == "shaderpacks" else "required"},
            "downloads": [m["url"]], "fileSize": m["size"],
        } for m in mods.values()],
        "dependencies": {"minecraft": MC, "neoforge": NEOFORGE},
    }
    path = path or os.path.join(DIST, f"grounded-{stem}.mrpack")
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("modrinth.index.json", json.dumps(index, indent=2))
        for od in override_dirs:
            base = os.path.join(ROOT, "overrides", od)
            for dp, _, fns in os.walk(base):
                for fn in fns:
                    full = os.path.join(dp, fn)
                    z.write(full, "overrides/" + os.path.relpath(full, base).replace(os.sep, "/"))
    print(f"  wrote {os.path.relpath(path, ROOT)}  ({len(mods)} jars)")


def main():
    lock, mods, overrides = {}, {}, []
    for stem, name, slugs, ods in STAGES:
        for s in slugs:
            resolve(s, mods)
        overrides += ods
        write_pack(stem, name, dict(mods), list(overrides))
        lock[stem] = [m["slug"] for m in mods.values()]
        if stem == "2-survival":
            after_s2, ov_s2 = dict(mods), list(overrides)
    if MC == "26.2":
        write_pack(RELEASE_VERSION, RELEASE_NAME, dict(mods), list(overrides),
                   path=os.path.join(ROOT, "dist", f"not-so-vanilla-{RELEASE_VERSION}.mrpack"), summary=RELEASE_SUMMARY)
    alt = {}
    if ALT_STAGE:
        stem, name, slugs, ods = ALT_STAGE
        alt = dict(after_s2)
        for s in slugs:
            resolve(s, alt)
        write_pack(stem, name, alt, ov_s2 + ods)
        lock[stem] = [m["slug"] for m in alt.values()]
    allmods = {**mods, **alt}
    with open(os.path.join(DIST, "lock.json"), "w") as fh:
        json.dump({"minecraft": MC, "neoforge": NEOFORGE, "stages": lock,
                   "mods": {m["slug"]: {k: m[k] for k in ("title", "version", "type", "published", "file", "required_by")}
                            for m in allmods.values()}}, fh, indent=2)
    print("\nResolved mods:")
    for m in sorted(allmods.values(), key=lambda m: m["slug"]):
        dep = f"  (dependency of {m['required_by']})" if m["required_by"] else ""
        print(f"  {m['title']:<40} {m['version']:<28} {m['type']:<8}{dep}")


if __name__ == "__main__":
    main()
