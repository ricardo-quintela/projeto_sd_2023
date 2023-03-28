# Protocolo Multicast a utilizar

## Pré-requisitos

Considera-se que um `Downloader` processa o URL-N.
Considera-se que existem $M$ `Barrel`s.

## Cenário de sucesso

1. `Downloader` envia uma query para saber quantos `Barrel`s estão ligados ao grupo com o tamanho da mensagem a enviar (`SIN_N_SIZE`)
2. `Barrel`-X recebe a query e aloca `SIZE` bytes
3. `Barrel`-X responde com uma confirmação que está pronto para receber (`SINACK_N`)
4. `Downloader` recebe $W \in [0,M]$ confirmações (`SINACK`)
5. `Downloader` envia a a mensagem $N$ até receber $W$ confirmações ou até $t$ segundos se passarem (*timeout*)
6. `Barrel`-X recebe a mensagem $N$
7. `Barrel`-X responde com uma confirmação de receção (`ACK_N`)

## Cenário em que há uma falha

Considera-se que `Barrel`-Y não recebeu a mensagem $N$.

1. `Downloader` envia uma query para saber quantos `Barrel`s estão ligados ao grupo com o tamanho da mensagem a enviar (`SIN_N+P_SIZE`)
2. `Barrel`-X recebe a query e aloca `SIZE` bytes
3. `Barrel`-X responde com uma confirmação que está pronto para receber (`SINACK_N+P`)
4. `Barrel`-Y entende que não recebeu as mensagens $m \in [N, N+P-1]$
a. `Barrel`-Y pede a outros Barrels através do mesmo grupo multicast pelas mensagens $m \in [N, N+P-1]$
b. `Barrel`-X que recebeu $m \in [N, N+P-1]$ responde com o conteúdo das mesmas
c. `Barrel`-Y atualiza a sua base de dados
6. `Barrel`-Y responde com uma confirmação que está pronto para receber (`SINACK_N+P`)
7. `Downloader` recebe $W \in [0,M]$ confirmações (`SINACK`)
8. `Downloader` envia a a mensagem $N+P$ até receber W confirmações ou até $t$ segundos se passarem (*timeout*)
9. `Barrel`-X recebe a mensagem $N+P$
10. `Barrel`-X responde com uma confirmação de receção (`ACK_N+P`)
11. `Barrel`-Y recebe a mensagem $N+P$
12. `Barrel`-Y responde com uma confirmação de receção (`ACK_N+P`)
