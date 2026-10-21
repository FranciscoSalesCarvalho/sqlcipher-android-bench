# S1 em WAL — 1.000 commits de 100 B (2026-10-20)

- Aparelho: Samsung SM-A556E, Android 16, F2FS, modo avião
- journal pedido WAL; journal_actual=wal e synchronous=2 (FULL) confirmados em todas as linhas
- Medição inclui o checkpoint final (cópia do -wal para o banco)
- Log bruto: s1-100b-wal-device.txt

## Resultados (mínimo / mediana / máximo)

| Braço | Setores | KiB por commit (mín.) | Requisições | ms por commit | Amplificação (mín.) |
|---|---|---|---|---|---|
| A | 26.744 / 26.904 / 27.880 | 13,4 | 1.283 / 1.298 / 1.344 | 0,74 / 3,08 / 5,04 | 137x |
| B | 34.416 / 34.488 / 35.216 | 17,2 | 3.052 / 3.061 / 3.114 | 1,41 / 1,96 / 3,58 | 176x |
| C | 34.344 / 35.104 / 40.328 | 17,2 | 3.045 / 3.082 / 3.090 | 2,05 / 2,17 / 3,69 | 176x |

## TRUNCATE vs. WAL (mínimos de bytes, medianas de tempo)

| Braço | KiB/commit TRUNCATE | KiB/commit WAL | ms/commit TRUNCATE | ms/commit WAL |
|---|---|---|---|---|
| A | 16,4 | 13,4 | 1,97 | 3,08 |
| B | 106,9 | 17,2 (6,2x menos) | 35,6 | 1,96 (18x menos) |
| C | 106,3 | 17,2 (6,2x menos) | 33,6 | 2,17 (15x menos) |

## Leitura

- O custo enorme de B e C em TRUNCATE vem do journal de rollback: em WAL cai para ~17 KiB/commit,
  perto da estimativa ingênua de ~3-4 páginas. Mecanismo exato no F2FS (checkpoint após truncate) segue como hipótese.
- A vs. B em WAL: B escreve ~29% a mais (era 6,5x em TRUNCATE). A escrita atômica do framework
  só ajuda no modo com journal de rollback.
- C vs. B em WAL: mesmos bytes; C ~0,2 ms/commit mais lento na mediana, coerente com ~4 páginas x 60 µs.
  A diferença está dentro da dispersão.
- A em WAL tem alta variância de tempo (0,74-5,04 ms/commit).

## Implicação prática

Com SQLCipher num aparelho F2FS, WAL é ~18x mais rápido que TRUNCATE em commits pequenos.
