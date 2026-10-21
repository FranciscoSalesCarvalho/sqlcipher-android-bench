# sqlcipher-android-bench

Measuring what SQLCipher really costs in a Room database on a real Android phone:
bytes written to storage, CPU time, commit latency and database open time.

Companion code for the article *Encrypting Room with SQLCipher: the crypto is cheap, the engine swap is not* (link coming soon).

## Findings (preliminary)

Samsung Galaxy A55 (SM-A556E), Android 16, `/data` on F2FS. Numbers are from preliminary runs.

1. **Encryption barely changes what reaches the disk.** 0% extra pages with 4 KiB rows, about 3% with 100-byte rows (the 80-byte per-page reserve costs one whole row per page).
2. **The CPU cost is per page, and fsync hides it.** About 60 µs per page; batch inserts were up to 2.5x slower, single-row commits showed no measurable difference.
3. **The hidden penalty is the engine, not the crypto.** In TRUNCATE mode the framework SQLite commits without a journal (F2FS batch atomic write); SQLCipher does not, and wrote ~6.5x more and was ~17x slower per commit. WAL closes most of the gap.
4. **Opening is where encryption really costs.** PBKDF2 (256,000 iterations) made each open take ~0.5–0.7 s; passing a random 256-bit key as a raw key (`x'<hex>'`) brought it to ~5 ms.

## Three arms

| Arm | Engine | Encrypted | Purpose |
| --- | --- | --- | --- |
| A | Framework SQLite (Room default) | No | Baseline |
| B | SQLCipher, empty key | No | Control: same engine as C, no encryption |
| C | SQLCipher, passphrase | Yes | Treatment |
| D | SQLCipher, raw key `x'<hex>'` | Yes | Open-time variant (skips PBKDF2) |

B vs. C isolates encryption. A vs. B isolates the engine swap.

## Project layout

```
storage/     Room entity, DAO, BenchStore facade, DatabaseFactory (opens each arm, pins PRAGMAs)
benchmark/   Instrumented tests that run on the device and log CSV lines
  metrics/   Shell (UiAutomation), DiskStats reader (/proc/diskstats), validation tests
evidence/    Raw logs and notes behind every number (notes are in Portuguese)
scripts/     capture-header-evidence.sh
app/         Template app module (not used by the measurements)
```

## How bytes are measured

`/proc/self/io` is not readable by apps on Android 16, and the shell cannot read another process's copy.
The tests read sectors written to the block device behind `/data` from `/proc/diskstats`, through
`UiAutomation.executeShellCommandRwe`, with `sync` before and after each operation.
The counter is device-wide: each run is followed by an idle window of the same length, and byte counts are reported as the minimum across rounds.

## Requirements

- A real device on API 31+ (the emulator denies `/proc/diskstats`, and its disk is a host file)
- Airplane mode, screen on, device left alone during the runs
- `adb` on the PATH; set `ANDROID_SERIAL` if more than one device is connected

## Running

Validate the arms and the byte counter first:

```shell
./gradlew :storage:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.fsales.storage.HeaderValidationTest
./gradlew :benchmark:connectedReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.fsales.benchmark.metrics.DiskStatsTest
```

Then the scenarios:

| Test class | Scenario | Log tag |
| --- | --- | --- |
| `S2FirstLookTest` | Batch insert, 4 KiB and 100 B rows; PRAGMAs per arm | `S2` |
| `S1AutocommitTest` | 1,000 single-row commits, TRUNCATE and WAL | `S1` |
| `AtomicWriteProbeTest` | Does each engine create a `-journal` file? | `Atomic` |
| `S5OpenTest` | Cold open: open, derive key, read 1,000 rows | `S5` |

```shell
adb logcat -c
./gradlew :benchmark:connectedReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.fsales.benchmark.S1AutocommitTest
adb logcat -d -s S1 > evidence/s1-my-device.txt
```

## Limits

One device and one filesystem; SQLCipher 4.18.0 community with its default crypto provider (LibTomCrypt);
device-wide byte counter; no root, so the page cache is not dropped; 5 rounds per scenario (10 for open);
Keystore unwrap cost not measured. The framework's use of F2FS batch atomic writes is inferred from behaviour
(the missing `-journal` file), not from its compile options, which it does not expose.

Results from other devices, especially Pixels or devices with ext4, are welcome as issues.

## License

MIT. See [LICENSE](LICENSE).
