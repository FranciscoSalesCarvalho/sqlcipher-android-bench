# S2 — primeira medição (2026-10-18)

- Aparelho: Samsung SM-A556E, Android 16, modo avião
- 2.000 linhas x 4.096 B (8.192.000 B lógicos), journal pedido: TRUNCATE, 5 rodadas
- Log bruto: s2-first-look-device.txt

## Resultado: comparação ainda NÃO válida

Os braços rodaram com configurações diferentes:

| Braço | journal_mode | synchronous | page_size | cipher_version |
|---|---|---|---|---|
| A | truncate | 2 (FULL) | 4096 | 3.3.1 (!) |
| B | delete | 1 (NORMAL) | 4096 | 4.18.0 community |
| C | delete | 1 (NORMAL) | 4096 | 4.18.0 community |

- `setJournalMode(TRUNCATE)` do Room não fixa o journal: ele só liga/desliga WAL.
  O braço A herdou TRUNCATE do framework Android; B e C herdaram DELETE do SQLite.
- `synchronous` difere (FULL vs NORMAL): defaults diferentes de cada motor.
- O SQLite do framework neste Samsung responde a `PRAGMA cipher_version` (3.3.1):
  não é o SQLite AOSP puro. A investigar.

## Números observados (apenas indicativos)

- Setores escritos: ~18.420–18.660 nos três braços (amplificação ~1,15); outliers com mais writes = ruído.
- Duração: A ~80–105 ms, B ~86–91 ms, C ~171–229 ms (custo de CPU da cifra).
- Tamanho do arquivo: A 9.248.768 B; B e C 9.232.384 B (A tem 4 páginas a mais, provavelmente a tabela android_metadata do framework).
- Payload de 4.096 B transborda a página (overflow), então a reserva de IV/HMAC do braço C não muda o número de páginas. Payload pequeno é necessário para ver o overhead de espaço.
