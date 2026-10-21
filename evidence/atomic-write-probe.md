# Escrita atômica: o framework usa journal nos commits? (2026-10-20)

- Aparelho: Samsung SM-A556E, Android 16, /data em F2FS
- journal_mode=truncate, synchronous=FULL nos três braços
- Logs: atomic-write-probe-device.txt (v1, inconclusivo para A), atomic-write-probe-v2-device.txt (v2)

## Método (v2)

1. Abrir o banco (Room cria as tabelas), fechar.
2. Apagar o arquivo -journal de 0 bytes com o banco fechado.
3. Reabrir, conferir que o journal não existe, fazer 10 commits, conferir de novo.

Em TRUNCATE, um SQLite que usa journal cria o arquivo no primeiro commit e o mantém com 0 bytes.

## Resultado

| Braço | compile_options | Journal após reabrir | Journal após 10 commits |
|---|---|---|---|
| A | 0 opções (diagnóstico omitido no build) | não existe | **não existe** |
| B | 57 opções, sem ENABLE_BATCH_ATOMIC_WRITE | não existe | existe, 0 B |
| C | idem B | não existe | existe, 0 B |

## Conclusão

O SQLite do framework fez 10 commits em modo TRUNCATE sem criar o arquivo de journal.
O SQLCipher (sem ENABLE_BATCH_ATOMIC_WRITE) usou o journal normalmente.
Isso é consistente com o uso da escrita atômica em lote do F2FS pelo framework, e explica
por que A escreve ~6,5x menos por commit no S1.

Ressalva: o compile_options do framework está oculto, então a opção não é confirmada por nome;
a evidência é comportamental (ausência do journal).

## Nota sobre a v1

Na v1, o journal do A já existia antes dos commits: o framework abre em TRUNCATE, e a criação
das tabelas pelo Room deixou o arquivo no disco. B e C abrem em DELETE (o PRAGMA TRUNCATE só roda
no onOpen), por isso o journal da criação foi apagado. O controle "antes" detectou a falha.
