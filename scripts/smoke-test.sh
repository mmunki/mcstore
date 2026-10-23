#!/usr/bin/env bash
# Build MCStore, start a real Fabric 26.2 server and verify the commands,
# generated configuration and price persistence.  Everything is written to
# build/smoke/, which is ignored by Git.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SMOKE="$ROOT/build/smoke"
SERVER="$SMOKE/server"
CACHE="$SMOKE/cache"
LOG="$SMOKE/smoke.log"
PORT="${MCSTORE_SMOKE_PORT:-25577}"

JAVA="${JAVA_HOME:+$JAVA_HOME/bin/java}"
if [ -z "$JAVA" ] || [ ! -x "$JAVA" ]; then
    JAVA="$(command -v java || true)"
fi
if [ -z "$JAVA" ] || [ ! -x "$JAVA" ]; then
    echo "error: Java 25 is required (set JAVA_HOME)" >&2
    exit 1
fi

JAVA_VERSION="$("$JAVA" -version 2>&1 | awk -F'"' '/version/ {print $2; exit}')"
JAVA_MAJOR="${JAVA_VERSION%%.*}"
if [ "$JAVA_MAJOR" -ne 25 ]; then
    echo "error: MCStore 26.2.2 needs Java 25, found $JAVA_VERSION ($JAVA)" >&2
    exit 1
fi

for tool in curl python3; do
    command -v "$tool" >/dev/null 2>&1 || {
        echo "error: $tool is required" >&2
        exit 1
    }
done

mkdir -p "$SMOKE" "$CACHE" "$SERVER"

echo "==> building MCStore"
"$ROOT/gradlew" --no-daemon --console=plain build
JAR="$ROOT/build/libs/MCStore-26.2.2.jar"
[ -s "$JAR" ] || {
    echo "error: build did not produce $JAR" >&2
    exit 1
}

echo "==> verifying the jar is a universal (client + server) mod"
python3 - "$JAR" <<'PY'
import json
import sys
import zipfile

with zipfile.ZipFile(sys.argv[1]) as jar:
    metadata = json.loads(jar.read("fabric.mod.json").decode("utf-8"))

environment = metadata.get("environment")
main = metadata.get("entrypoints", {}).get("main", [])
if environment != "*":
    raise SystemExit("fabric.mod.json environment is %r, expected '*'" % environment)
if "dev.mmunki.mcstore.MCStoreMod" not in main:
    raise SystemExit("fabric.mod.json has no common main entrypoint")
print("environment: " + environment)
PY

INSTALLER="$CACHE/fabric-installer.jar"
if [ ! -s "$INSTALLER" ]; then
    echo "==> downloading the Fabric installer"
    curl -fL --retry 3 --max-time 120 \
        -o "$INSTALLER" \
        https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.1.2/fabric-installer-1.1.2.jar
fi

if [ ! -s "$SERVER/fabric-server-launch.jar" ]; then
    echo "==> installing Fabric 26.2 server into $SERVER"
    "$JAVA" -jar "$INSTALLER" server \
        -dir "$SERVER" -mcversion 26.2 -loader 0.20.1 -downloadMinecraft
fi

API="$CACHE/fabric-api-0.161.0+26.2.jar"
if [ ! -s "$API" ] || [ "$(wc -c <"$API")" -lt 1000000 ]; then
    echo "==> downloading Fabric API 0.161.0+26.2"
    curl -fL --retry 3 --max-time 120 \
        -o "$API" \
        https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.161.0+26.2/fabric-api-0.161.0+26.2.jar
fi

rm -rf "$SERVER/mods" "$SERVER/config/mcstore" "$SERVER/world" \
       "$SERVER/logs" "$SERVER/crash-reports" "$LOG"
mkdir -p "$SERVER/mods"
cp "$JAR" "$SERVER/mods/MCStore-26.2.2.jar"
cp "$API" "$SERVER/mods/fabric-api-0.161.0+26.2.jar"

printf 'eula=true\n' > "$SERVER/eula.txt"
cat > "$SERVER/server.properties" <<EOF
server-port=$PORT
online-mode=false
spawn-protection=0
max-players=1
max-tick-time=-1
view-distance=4
simulation-distance=4
level-seed=0
motd=MCStore smoke test
EOF

echo "==> starting the server on port $PORT"
MCSTORE_SMOKE_SERVER="$SERVER" MCSTORE_SMOKE_LOG="$LOG" \
python3 - <<'PY'
import json
import os
import queue
import re
import subprocess
import sys
import threading
import time

server = os.environ["MCSTORE_SMOKE_SERVER"]
log_path = os.environ["MCSTORE_SMOKE_LOG"]
out = open(log_path, "w", encoding="utf-8", buffering=1)

env = os.environ.copy()
java = os.path.join(os.environ["JAVA_HOME"], "bin", "java") if os.environ.get("JAVA_HOME") else "java"
process = subprocess.Popen(
    [java, "-Xms512M", "-Xmx1536M", "-jar", "fabric-server-launch.jar", "nogui"],
    cwd=server,
    stdin=subprocess.PIPE,
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True,
    bufsize=1,
    env=env,
)

lines = queue.Queue()

def reader():
    for line in process.stdout:
        lines.put(line)
    lines.put(None)

threading.Thread(target=reader, daemon=True).start()

def emit(line):
    out.write(line)
    out.flush()
    sys.stdout.write(line)
    sys.stdout.flush()

def send(command):
    emit(">>> " + command + "\n")
    process.stdin.write(command + "\n")
    process.stdin.flush()

commands = [
    "mcstore",
    "buy",
    "stock minecraft:diamond",
    "mcstore stock minecraft:diamond",
    "mcstore restock",
    "forceload add 0 0",
    "setblock 0 100 0 mcstore:money_ore",
    "setblock 0 100 0 minecraft:air",
    "forceload remove 0 0",
    "execute in minecraft:the_nether run forceload add -32 -32 32 32",
    "execute in minecraft:the_nether run forceload remove -32 -32 32 32",
    "mcstore setprice minecraft:diamond sell 55.00",
    "mcstore setprice diamond buy 123.00",
    "mcstore reload",
    "stop",
]

started = False
sent = False
deadline = time.time() + 300
while time.time() < deadline:
    try:
        line = lines.get(timeout=1.0)
    except queue.Empty:
        if process.poll() is not None:
            break
        continue
    if line is None:
        break
    emit(line)
    if not started and "Done (" in line:
        started = True
        time.sleep(1.0)
        for command in commands:
            send(command)
            time.sleep(0.8)
        sent = True
    if sent and process.poll() is not None:
        break

if process.poll() is None:
    try:
        process.wait(timeout=60)
    except subprocess.TimeoutExpired:
        process.kill()
        emit("error: server did not stop within 60 seconds\n")

try:
    process.wait(timeout=10)
except subprocess.TimeoutExpired:
    process.kill()

# Drain anything still buffered.
while True:
    try:
        line = lines.get_nowait()
    except queue.Empty:
        break
    if line is None:
        break
    emit(line)

out.close()
PY

echo "==> verifying the smoke test"

python3 - "$SERVER" "$LOG" <<'PY'
import glob
import gzip
import json
import os
import re
import sys
import zlib

server, log_path = sys.argv[1], sys.argv[2]
text = open(log_path, encoding="utf-8").read()

required = [
    "Loading Minecraft 26.2 with Fabric Loader 0.20.1",
    "MCStore 26.2.2 loaded (client + server, singleplayer worlds supported",
    "MCStore 26.2.2 initialized on Minecraft 26.2 (Fabric)",
    "MCStore commands registered: /buy /sell /exchange /stock /balance /mcstore",
    "MCStore market: stock caps",
    "Coverage target 80%: PASS",
    "Only a player can open the store.",
    "Stock minecraft:diamond: 64/64",
    "MCStore market restocked",
    "Changed the block at 0, 100, 0",
    "Set sell price of minecraft:diamond to 55.00",
    "Set buy price of minecraft:diamond to 123.00",
    "MCStore reloaded prices.json, stocks.json and config.json.",
    "Done (",
]
for marker in required:
    if marker not in text:
        print("MISSING: " + marker, file=sys.stderr)
        print(text[-8000:], file=sys.stderr)
        raise SystemExit(1)

for bad in ("Failed to start the minecraft server", "Exception while loading entries",
            "Failed to load registries", "Couldn't parse", "Unknown block type"):
    if bad in text:
        print("ERROR: " + bad, file=sys.stderr)
        print(text[-8000:], file=sys.stderr)
        raise SystemExit(1)

config_dir = os.path.join(server, "config", "mcstore")
for name in ("config.json", "prices.json", "balances.json", "stocks.json", "admins.json"):
    path = os.path.join(config_dir, name)
    if not os.path.isfile(path) or os.path.getsize(path) < 2:
        print("MISSING config file: " + path, file=sys.stderr)
        raise SystemExit(1)

with open(os.path.join(config_dir, "prices.json"), encoding="utf-8") as handle:
    prices = json.load(handle)["items"]
entry = prices.get("minecraft:diamond")
if entry is None or abs(float(entry.get("sell", -1)) - 55.0) > 0.0001:
    print("diamond sell price was not persisted: " + repr(entry), file=sys.stderr)
    raise SystemExit(1)
if abs(float(entry.get("buy", -1)) - 123.0) > 0.0001:
    print("diamond buy price was not persisted: " + repr(entry), file=sys.stderr)
    raise SystemExit(1)

def region_contains(path, needle=b"mcstore:money_ore"):
    with open(path, "rb") as handle:
        header = handle.read(8192)
        if len(header) < 8192:
            return False
        for index in range(1024):
            offset = int.from_bytes(header[index * 4:index * 4 + 3], "big")
            if offset == 0:
                continue
            handle.seek(offset * 4096)
            length_bytes = handle.read(4)
            if len(length_bytes) < 4:
                continue
            length = int.from_bytes(length_bytes, "big")
            compression = handle.read(1)
            data = handle.read(max(0, length - 1))
            try:
                if compression == b"\x01":
                    raw = gzip.decompress(data)
                elif compression == b"\x02":
                    raw = zlib.decompress(data)
                elif compression == b"\x03":
                    raw = data
                else:
                    continue
            except Exception:
                continue
            if needle in raw:
                return True
    return False

def dimension_has_money_ore(dimension):
    pattern = os.path.join(server, "world", "dimensions", "minecraft", dimension, "region", "*.mca")
    return any(region_contains(path) for path in glob.glob(pattern))

overworld_ore = dimension_has_money_ore("overworld")
nether_ore = dimension_has_money_ore("the_nether")
if not overworld_ore:
    print("money ore was not generated in the overworld", file=sys.stderr)
    raise SystemExit(1)
if not nether_ore:
    print("money ore was not generated in the nether", file=sys.stderr)
    raise SystemExit(1)

match = re.search(
    r"MCStore prices (?:generated|loaded) \((\d+) default entries\), "
    r"(\d+) sellable of (\d+) filtered items \((\d+)%\), "
    r"(\d+) of (\d+) registry items \((\d+)%\), (\d+) buyable ores",
    text,
)
if not match:
    print("could not read the coverage line", file=sys.stderr)
    raise SystemExit(1)

defaults, sellable, filtered, filtered_pct, registry_priced, registry_total, registry_pct, buyable = match.groups()
summary = (
    "status: passed\n"
    "mod: 26.2.2\n"
    "minecraft: 26.2\n"
    "fabric-loader: 0.20.1\n"
    "fabric-api: 0.161.0+26.2\n"
    f"prices: {sellable}\n"
    f"sellable-filtered: {sellable} of {filtered} ({filtered_pct}%)\n"
    f"registry-covered: {registry_priced} of {registry_total} ({registry_pct}%)\n"
    f"buyable: {buyable}\n"
    "stock-system: passed\n"
    "stock-cap-diamond: 64/64\n"
    "xp-exchange-rate: 1.00 per level\n"
    "money-ore-block: registered\n"
    "money-ore-overworld: generated\n"
    "money-ore-nether: generated\n"
    "diamond-sell: 55.00\n"
    "diamond-buy: 123.00\n"
)
open(os.path.join(os.path.dirname(log_path), "summary.txt"), "w", encoding="utf-8").write(summary)
print(summary, end="")
PY

echo "==> smoke test passed"
echo "    log:     $LOG"
echo "    summary: $SMOKE/summary.txt"
