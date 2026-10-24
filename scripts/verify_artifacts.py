"""Inspect the built public jar without running it. This is not a malware guarantee."""
import argparse
import hashlib
import json
import re
from pathlib import Path
import subprocess
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()


def require(condition, message):
    if not condition:
        raise SystemExit(message)


def archive(path):
    with zipfile.ZipFile(path) as jar:
        require(jar.testzip() is None, f"Corrupt archive: {path.name}")
        names = jar.namelist()
        require(len(names) == len(set(names)), f"Duplicate archive entries: {path.name}")
        require(all(not n.startswith("/") and ".." not in Path(n).parts for n in names), "Unsafe archive path")
        return {name: jar.read(name) for name in names if not name.endswith("/")}


def method_references(data):
    """Read class-file constant-pool references; never load or execute the class."""
    count = struct.unpack_from(">H", data, 8)[0]
    pool, offset, index = {}, 10, 1
    while index < count:
        tag = data[offset]
        offset += 1
        if tag == 1:
            size = struct.unpack_from(">H", data, offset)[0]
            offset += 2
            pool[index] = (tag, data[offset:offset + size].decode("utf-8", errors="replace"))
            offset += size
        elif tag in (7, 8, 16, 19, 20):
            pool[index] = (tag, struct.unpack_from(">H", data, offset)[0])
            offset += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            pool[index] = (tag, *struct.unpack_from(">HH", data, offset))
            offset += 4
        elif tag in (3, 4):
            offset += 4
        elif tag in (5, 6):
            offset += 8
            index += 1
        elif tag == 15:
            offset += 3
        else:
            raise SystemExit(f"Unexpected class constant-pool tag: {tag}")
        index += 1
    return {(pool[pool[value[1]][1]][1], pool[pool[value[2]][1]][1])
            for value in pool.values() if value[0] in (10, 11)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--allow-dirty", action="store_true", help="Local development check; not an exact-commit release")
    args = parser.parse_args()
    properties = dict(line.split("=", 1) for line in (ROOT / "gradle.properties").read_text().splitlines()
                      if "=" in line and not line.lstrip().startswith("#"))
    version = properties["mod_version"].strip()
    jar = ROOT / "build/libs" / f"dragsimutils-{version}.jar"
    sources = jar.with_name(f"dragsimutils-{version}-sources.jar")
    require(jar.is_file() and sources.is_file(), "Run ./gradlew build first; both runtime and sources jars are required")
    entries = archive(jar)
    metadata = json.loads(entries["fabric.mod.json"])
    require(metadata["id"] == "dragsimutils" and metadata["name"] == "Dragsim Utils", "Wrong mod identity")
    require(metadata["version"] == version and metadata["environment"] == "client", "Wrong version or environment")
    require(metadata["entrypoints"] == {"client": ["com.dragsimutils.DragsimUtilsClient"]}, "Unexpected entrypoint")
    require(not metadata.get("jars"), "Unexpected bundled dependency jar")
    require(metadata["breaks"].get("endsight") == "*", "Old mod conflict guard is missing")
    require(metadata["depends"].get("minecraft") == "~26.1.2" and metadata["depends"].get("java") == ">=25", "Wrong game requirements")
    require("fabric-api" in metadata["depends"] and "fabricloader" in metadata["depends"], "Missing Fabric requirements")
    require(metadata["icon"] in entries and metadata["accessWidener"] in entries, "Missing packaged resource")
    head = git("rev-parse", "HEAD")
    require(metadata["custom"]["dragsimutils:commit"] == head, "Jar source commit does not match HEAD; rebuild")
    dirty = bool(git("status", "--porcelain", "--untracked-files=normal"))
    require(args.allow_dirty or not dirty, "Source tree has uncommitted files; commit and rebuild for a release artifact")

    classes = {name: data for name, data in entries.items() if name.endswith(".class")}
    require(classes, "Runtime jar contains no classes")
    forbidden_paths = ("/updater/", "/dev/", "/gametest/", "TestFixtures", "VisualTest", "Checks.class", "EndsightDemo")
    forbidden_apis = (b"java/lang/ProcessBuilder", b"java/io/ObjectInputStream",
                      b"java/net/URLClassLoader", b"defineClass", b"loadLibrary", b"setAccessible",
                      b"sun/misc/Unsafe", b"jdk/internal/misc/Unsafe")
    approved_web_hosts = {b"dragsim.co", b"cdn.discordapp.com", b"github.com"}
    runtime_web_hosts = set()
    for name, data in classes.items():
        require(name.startswith("com/dragsimutils/"), f"Unexpected runtime namespace: {name}")
        require(not any(part in name for part in forbidden_paths), f"Development/updater class packaged: {name}")
        require(not any(api in data for api in forbidden_apis), f"Sensitive API requires manual investigation: {name}")
        # Match Runtime methods rather than byte prefixes such as RuntimeException.
        require(not any(owner in ("java/lang/Runtime", "java/lang/System") and method in ("exec", "load", "loadLibrary")
                        for owner, method in method_references(data)), f"Runtime execution/loading API: {name}")
        require(b"com/endsight/" not in data, f"Stale package reference: {name}")
        hosts = set(re.findall(rb"https?://([A-Za-z0-9.-]+)", data))
        require(hosts <= approved_web_hosts, f"Undocumented web host: {name}")
        runtime_web_hosts.update(hosts)
        require(b"/v1/estimate/" not in data and b"TooltipPrices" not in data,
                f"External item-pricing code packaged: {name}")
        require(b"java/net/http/HttpRequest$BodyPublishers" not in data, f"Unexpected HTTP upload body: {name}")
    for name in entries:
        require(not name.lower().endswith((".exe", ".dll", ".so", ".dylib", ".bat", ".cmd", ".ps1", ".sh", ".jar", ".java", ".key", ".pem")),
                f"Unexpected executable/source/private-key payload: {name}")
        require(not name.startswith(("com/endsight/", "com/dragsimutils/updater/")), f"Old runtime entry: {name}")
    for specification in metadata["mixins"]:
        config = json.loads(entries[specification["config"]])
        require(not config.get("plugin"), "Unexpected mixin plugin")
        for name in config.get("client", []) + config.get("mixins", []):
            require((config["package"] + "." + name).replace(".", "/") + ".class" in entries, f"Missing mixin: {name}")

    source_entries = archive(sources)
    java_files = [p for folder in ("src/main/java", "src/client/java") for p in (ROOT / folder).rglob("*.java")]
    for source in java_files:
        prefix = ROOT / ("src/client/java" if "client" in source.relative_to(ROOT).parts[:2] else "src/main/java")
        name = source.relative_to(prefix).as_posix()
        require(source_entries.get(name) == source.read_bytes(), f"Sources jar differs from checkout: {name}")
    require(not any("/updater/" in n or "TestFixtures" in n or n.endswith("Checks.java") for n in source_entries), "Test/updater source packaged")
    report = {
        "mod": metadata["id"], "version": version, "commit": head,
        "uncommitted_source": dirty, "runtime_classes": len(classes), "java_source_files": len(java_files),
        "java_source_lines": sum(len(p.read_text(encoding="utf-8").splitlines()) for p in java_files),
        "automated_archive_checks": "passed",
        "runtime_web_hosts": sorted(h.decode("ascii") for h in runtime_web_hosts),
        "scope": "Archive and source checks only; manual review and in-game validation are separate.",
        "artifacts": [{"file": p.name, "bytes": p.stat().st_size, "sha256": hashlib.sha256(p.read_bytes()).hexdigest()} for p in (jar, sources)]
    }
    output = ROOT / "build/checks"
    output.mkdir(parents=True, exist_ok=True)
    (output / "artifacts.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
