# Validação de cabeçalho dos braços

- Data: 2026-10-18 16:03:16 -0400
- Alvo: EMULADOR (não usar para medições) — sdk_gphone64_arm64, Android 16 (SDK 36), serial emulator-5554
- Build: google/sdk_gphone64_arm64/emu64a:16/BE2A.250530.026.D1/13818094:user/release-keys
- Versões: AGP 9.4.1, Room 2.8.5, KSP 2.3.11, SQLCipher 4.18.0
- Gradle: exit code 0

## Resultado
- armA_isPlaintext: passou
- armB_isPlaintext: passou
- armC_isEncrypted: passou
- armB_isReadableByFrameworkSqlite: passou

## Cabeçalhos (primeiros 16 bytes, hex)

```
A_FRAMEWORK: 53 51 4c 69 74 65 20 66 6f 72 6d 61 74 20 33 00
B_SQLCIPHER_PLAIN: 53 51 4c 69 74 65 20 66 6f 72 6d 61 74 20 33 00
C_SQLCIPHER_KEYED: 37 e8 56 54 bf 1f 9c 45 4b 89 8c 4f 5d f4 7e 09
```

Esperado: A e B = `53 51 4c 69 74 65 20 66 6f 72 6d 61 74 20 33 00` ("SQLite format 3\0"); C = salt aleatório, diferente a cada execução.

## Arquivos
- README.md
- TEST-Medium_Phone_API_36.0(AVD) - 16.xml
- gradle.log
- html-report
- logcat.txt
