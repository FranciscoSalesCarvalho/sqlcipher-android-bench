# S5 — abertura a frio (2026-10-20)

- Aparelho: Samsung SM-A556E, Android 16, modo avião
- Banco com 1.000 linhas (100 B), WAL; medição = open + warmUp + count(); 10 rodadas, ordem embaralhada
- "A frio" = conexão nova do SQLite; cache de arquivos do kernel não foi esvaziado (exige root)
- Log bruto: s5-open-device.txt

## Configuração lida (PREP)

| Braço | kdf_iter | cipher | Cabeçalho |
|---|---|---|---|
| A | — | — | SQLite format 3 |
| B | — | — | SQLite format 3 |
| C (passphrase) | 256000 | aes-256-cbc | cifrado |
| D (chave bruta x'..') | 256000 (não usado) | aes-256-cbc | cifrado |

O count() == 1.000 passou em todos os braços: as chaves abriram os bancos.

## Tempo de abertura (ms)

| Braço | Mínimo | Mediana | Máximo | CPU/relógio (mediana) |
|---|---|---|---|---|
| A | 2,9 | 3,5 | 4,0 | 0,99 |
| B | 2,1 | 2,7 | 2,9 | 0,99 |
| C | 503,4 | 684,8 | 689,6 | 0,99 |
| D | 3,9 | 5,2 | 5,8 | 0,99 |

- C: ~0,5–0,7 s por abertura, 99% em CPU: a PBKDF2 (256.000 iterações) domina. ~190x o B.
- D: a mesma cifra sem PBKDF2 custa ~2,5 ms a mais que o B. A chave bruta elimina >99% do custo de abertura.
- H4 confirmada.

## Mudança de patamar no meio da execução

Rodadas 0–3: C ~505 ms. Rodadas 4–9: C ~686 ms (+36%).
A, B e D subiram na mesma proporção, no mesmo momento (A 3,0→3,9; B 2,2→2,8; D 4,0→5,3).
Como todos os braços mudaram juntos, a causa é o estado do aparelho, não o braço:
provavelmente redução de frequência da CPU (térmica ou de energia) ou troca de núcleo.
Para as medições finais: registrar estado térmico e frequência da CPU por linha.

## Implicação

Se a chave já tem 256 bits de entropia (aleatória, protegida pelo Keystore), passá-la como
chave bruta evita ~0,5–0,7 s em cada abertura. A PBKDF2 só faz sentido quando a chave vem
de uma senha do usuário. Não medido: o custo de desembrulhar a chave no Keystore (TEE/StrongBox).
