# S1 — 1.000 commits de 100 B (2026-10-18)

- Aparelho: Samsung SM-A556E, Android 16, F2FS, modo avião
- 1.000 inserts, um commit cada; journal_mode=truncate, synchronous=FULL; 5 rodadas
- Log bruto: s1-100b-device.txt

## Resultados (mínimo / mediana / máximo)

| Braço | Setores | KiB por commit (mín.) | Requisições de escrita | ms por commit | Amplificação (mín.) |
|---|---|---|---|---|---|
| A | 32.752 / 32.848 / 34.488 | 16,4 | 5.009 / 5.019 / 5.095 | 0,93 / 1,97 / 2,20 | 168x |
| B | 213.872 / 219.400 / 224.592 | 106,9 | 23.596 / 23.790 / 26.038 | 32,4 / 35,6 / 38,5 | 1.095x |
| C | 212.576 / 214.568 / 218.400 | 106,3 | 23.277 / 23.368 / 23.504 | 27,1 / 33,6 / 43,2 | 1.088x |

flushes = 0 em todas as medições (fsync_mode=nobarrier).
Arquivo final: A 147.456 B (36 páginas); B e C 139.264 B (34 páginas).

## Previsões vs. resultado

1. H3 (amplificação explosiva no S1): confirmada e maior que o previsto. S2 com 100 B: ~1,3–1,4x; S1: 168x (A) e ~1.090x (B, C).
2. Escrita atômica no framework: consistente. A escreve ~6,5x menos que B e faz ~5 requisições por commit contra ~24.
   Previsão era "talvez metade"; a diferença foi muito maior. Ainda não comprovado diretamente.
3. Cifra perde peso relativo: confirmada. C não é mais lento que B (mediana 33,6 vs. 35,6 ms/commit);
   o custo esperado da cifra (~0,2 ms/commit) fica dentro da variação.
4. Bytes de B e C iguais: confirmada (mínimos 213.872 vs. 212.576 setores).

## Erro de previsão

Previ ~12 KiB por commit (3 páginas). B e C escreveram ~107 KiB por commit, ~9x mais.
Hipótese: no F2FS, truncar o journal e fazer fsync força um checkpoint do sistema de arquivos
(metadados extras a cada commit). A, sem journal, escapa disso. A testar com journal WAL.

## Próximas verificações

- `PRAGMA compile_options` em cada braço: procurar ENABLE_BATCH_ATOMIC_WRITE.
- Repetir S1 com journal WAL nos três braços.
