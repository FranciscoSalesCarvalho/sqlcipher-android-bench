# S2 com payload de 100 B (2026-10-18)

- Aparelho: Samsung SM-A556E, Android 16, modo avião
- 10.000 linhas x 100 B (1.000.000 B lógicos), 5 rodadas
- Configuração igualada: journal_mode=truncate, synchronous=FULL, page_size=4096
- Log bruto: s2-100b-device.txt

## Previsão (escrita antes da execução)

Reserva de ~80 B por página no braço C (IV 16 B + HMAC SHA-512 64 B) → ~2% menos espaço útil → ~2% mais páginas e setores que o B.

## Tamanho do arquivo

| Braço | Bytes | Páginas | vs. B |
|---|---|---|---|
| A | 1.228.800 | 300 | +2 páginas (android_metadata) |
| B | 1.220.608 | 298 | — |
| C | 1.257.472 | 307 | +9 páginas (+3,0%) |

Resultado: +3,0%, acima dos 2% previstos. Explicação provável: as linhas são indivisíveis.
Perder 80 B por página custa uma linha inteira por página (~34 → ~33 linhas), ou seja, ~3%.

## Setores escritos

| Braço | Mínimo | Mediana | Máximo |
|---|---|---|---|
| A | 2.552 | 2.560 | 2.704 |
| B | 2.760 | 2.776 | 3.720 |
| C | 2.840 | 2.864 | 3.152 |

- C vs. B (mínimos): +80 setores (40 KiB, +2,9%). As 9 páginas a mais equivalem a 72 setores: o volume escrito acompanha o crescimento do arquivo.
- A vs. B: B escreve ~208 setores (104 KiB) a mais e faz ~2x mais requisições de escrita (≈50 vs. ≈26),
  embora os arquivos difiram em só 2 páginas. Em aberto. Hipótese a testar: o SQLite do framework
  usaria escrita atômica do F2FS (batch atomic write), evitando parte do journal.

## Duração (ms)

| Braço | Mínimo | Mediana | Máximo |
|---|---|---|---|
| A | 39 | 49 | 52 |
| B | 43 | 46 | 48 |
| C | 57 | 65 | 66 |

- C − B ≈ 19 ms para ~307 páginas ≈ 60 µs por página: o mesmo custo por página medido com payload de 4 KiB.
  O custo da cifra é proporcional ao número de páginas, não ao número de linhas.

## Amplificação (pelos mínimos)

A 1,31 · B 1,41 · C 1,45. Sobe em relação ao payload de 4 KiB porque rótulo e cabeçalhos do registro pesam mais em linhas pequenas.
