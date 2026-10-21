# S2 com configuração igualada (2026-10-18)

- Aparelho: Samsung SM-A556E, Android 16, modo avião
- 2.000 linhas x 4.096 B (8.192.000 B lógicos), 5 rodadas, ordem dos braços embaralhada
- Configuração (todos os braços): journal_mode=truncate, synchronous=FULL, page_size=4096
- SQLite: A 3.44.5 (framework Samsung, cipher_version 3.3.1); B e C 3.53.4 (SQLCipher 4.18.0 community)
- Log bruto: s2-equalized-device.txt

## Setores escritos

| Braço | Mínimo | Mediana | Máximo |
|---|---|---|---|
| A | 18.448 | 18.472 | 18.488 |
| B | 18.488 | 18.536 | 18.576 |
| C | 18.488 | 18.688 | 19.392 |

- O ruído é aditivo (só soma), então o mínimo é a estimativa mais robusta do custo real.
- B e C têm o mesmo mínimo (18.488 setores). A cifra não alterou o volume escrito neste cenário (H1).
- A vs. B: 40 setores (20 KiB), diferença de motor/versão.
- A dispersão maior do C coincide com mais operações de escrita e janelas ociosas não nulas;
  como o C demora ~2,5x mais, fica mais exposto ao ruído de fundo (viés contra o C a controlar).

## Duração (inserção + checkpoint, ms)

| Braço | Mínimo | Mediana | Máximo |
|---|---|---|---|
| A | 70 | 98 | 108 |
| B | 70 | 87 | 92 |
| C | 177 | 222 | 224 |

- C ~2,5x o B: ~135 ms a mais para ~2.254 páginas (~60 µs por página), consistente com AES + HMAC em software (LibTomCrypt).

## Amplificação

- ~1,15 em todos os braços: payload de 4.096 B transborda a página e cada linha ocupa 2 páginas.
- Arquivo: A 9.248.768 B; B e C 9.232.384 B (A tem 4 páginas a mais, provavelmente android_metadata).

## Observação

- synchronous FULL vs NORMAL quase não mudou os bytes do S2: há um único commit.
  O efeito deve aparecer no S1 (1.000 commits).
