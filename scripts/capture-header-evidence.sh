#!/usr/bin/env bash
# Roda a validação de cabeçalho dos braços e guarda a evidência em evidence/<timestamp>/.
# Uso (na raiz do projeto, com um único aparelho/emulador conectado):
#   ./scripts/capture-header-evidence.sh
set -euo pipefail

cd "$(dirname "$0")/.."

# Ordem de busca: variável ADB, PATH, sdk.dir do local.properties, ANDROID_HOME, caminho padrão do macOS.
find_adb() {
  if [ -n "${ADB:-}" ] && [ -x "$ADB" ]; then echo "$ADB"; return; fi
  if command -v adb >/dev/null 2>&1; then command -v adb; return; fi
  local sdk_dir
  sdk_dir="$(grep '^sdk.dir=' local.properties 2>/dev/null | cut -d= -f2- | tr -d '\r' | sed 's/\\//g')"
  for c in "$sdk_dir/platform-tools/adb" \
           "${ANDROID_HOME:-}/platform-tools/adb" \
           "${ANDROID_SDK_ROOT:-}/platform-tools/adb" \
           "$HOME/Library/Android/sdk/platform-tools/adb"; do
    [ -x "$c" ] && { echo "$c"; return; }
  done
}
ADB="$(find_adb || true)"
if [ -z "$ADB" ]; then
  echo "adb não encontrado. Rode no seu terminal:  type adb"
  echo "e execute o script passando o caminho:  ADB=/caminho/para/adb ./scripts/capture-header-evidence.sh"
  exit 1
fi
echo "==> Usando adb: $ADB"

DEVICES=$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')
COUNT=$(echo "$DEVICES" | grep -c . || true)
[ "$COUNT" -eq 1 ] || { echo "Conecte exatamente 1 aparelho (encontrados: $COUNT)"; exit 1; }
SERIAL="$DEVICES"

STAMP=$(date +%Y-%m-%d_%H%M%S)
OUT="evidence/header-validation_${STAMP}"
mkdir -p "$OUT"

echo "==> Limpando logcat"
"$ADB" -s "$SERIAL" logcat -c

echo "==> Rodando HeaderValidationTest"
set +e
./gradlew :storage:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.fsales.storage.HeaderValidationTest \
  > "$OUT/gradle.log" 2>&1
GRADLE_EXIT=$?
set -e

echo "==> Coletando evidências"
"$ADB" -s "$SERIAL" logcat -d -s HeaderValidation > "$OUT/logcat.txt"
cp storage/build/outputs/androidTest-results/connected/debug/*.xml "$OUT/" 2>/dev/null || true
cp -R storage/build/reports/androidTests/connected/debug "$OUT/html-report" 2>/dev/null || true

prop() { "$ADB" -s "$SERIAL" shell getprop "$1" | tr -d '\r'; }
MODEL=$(prop ro.product.model)
RELEASE=$(prop ro.build.version.release)
SDK=$(prop ro.build.version.sdk)
FINGERPRINT=$(prop ro.build.fingerprint)
QEMU=$(prop ro.kernel.qemu)
KIND="aparelho real"; [ "$QEMU" = "1" ] && KIND="EMULADOR (não usar para medições)"

ver() { grep -E "^$1 *=" gradle/libs.versions.toml | head -1 | cut -d'"' -f2; }

result_of() {
  local name="$1" xml
  xml=$(ls "$OUT"/*.xml 2>/dev/null | head -1)
  [ -n "$xml" ] || { echo "sem relatório"; return; }
  if ! grep -q "name=\"$name\"" "$xml"; then echo "não executado"
  elif grep -A1 "name=\"$name\"" "$xml" | grep -q "<failure"; then echo "FALHOU"
  else echo "passou"; fi
}

{
  echo "# Validação de cabeçalho dos braços"
  echo
  echo "- Data: $(date '+%Y-%m-%d %H:%M:%S %z')"
  echo "- Alvo: $KIND — $MODEL, Android $RELEASE (SDK $SDK), serial $SERIAL"
  echo "- Build: $FINGERPRINT"
  echo "- Versões: AGP $(ver agp), Room $(ver room), KSP $(ver ksp), SQLCipher $(ver sqlcipher)"
  echo "- Gradle: exit code $GRADLE_EXIT"
  echo
  echo "## Resultado"
  for t in armA_isPlaintext armB_isPlaintext armC_isEncrypted armB_isReadableByFrameworkSqlite; do
    echo "- $t: $(result_of $t)"
  done
  echo
  echo "## Cabeçalhos (primeiros 16 bytes, hex)"
  echo
  echo '```'
  grep 'HeaderValidation:' "$OUT/logcat.txt" | sed 's/.*HeaderValidation: //' | sort -u
  echo '```'
  echo
  echo "Esperado: A e B = \`53 51 4c 69 74 65 20 66 6f 72 6d 61 74 20 33 00\` (\"SQLite format 3\\0\"); C = salt aleatório, diferente a cada execução."
  echo
  echo "## Arquivos"
  (cd "$OUT" && ls -1 | sed 's/^/- /')
} > "$OUT/README.md"

echo "==> Evidência em $OUT"
cat "$OUT/README.md"
exit $GRADLE_EXIT
