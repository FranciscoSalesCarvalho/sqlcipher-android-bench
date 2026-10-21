# Validação do leitor de /proc/diskstats

- Data: 2026-10-18
- Aparelho: Samsung SM-A556E, Android 16 (SDK 36), modo avião
- Dispositivo de /data: dm-65 (F2FS sobre sda53)
- Método: sync → foto → operação → sync → foto
- Log bruto: diskstats-validation-device.txt

## Escrita conhecida (4 MiB + fsync, 5 repetições)

Esperado: >= 8.192 setores (4 MiB / 512 B).

| Repetição | Setores | Excesso (setores) | Excesso |
|---|---|---|---|
| 0 | 8.384 | 192 | 96 KiB (2,3%) |
| 1 | 8.384 | 192 | 96 KiB (2,3%) |
| 2 | 8.736 | 544 | 272 KiB (6,6%) |
| 3 | 8.392 | 200 | 100 KiB (2,4%) |
| 4 | 8.384 | 192 | 96 KiB (2,3%) |

Mediana: 8.384 setores. Excesso estável de 192 setores (96 KiB) em 3 de 5 repetições:
provável sobrecarga fixa do F2FS (blocos de nó e metadados). A repetição 2 mostra ruído de fundo.

## Ruído com o aparelho ocioso (janelas de 2 s, 10 repetições)

Setores: 2208, 848, 120, 1040, 240, 576, 152, 0, 336, 248

- Mediana: 292 setores (~146 KiB por 2 s, ~73 KiB/s)
- Mínimo: 0; máximo: 2.208 setores (~1,1 MB), na primeira janela (provável aquecimento)

## Observações

- flushes = 0 em todas as medições, inclusive com fsync. Coerente com a montagem
  `fsync_mode=nobarrier`: o fsync envia os dados ao dispositivo, mas não emite comando de flush do cache.
- O ruído cresce com a duração da janela, não com a operação. Operações curtas são pouco afetadas;
  cenários longos (S1) e payloads pequenos precisam de controle pareado.
