# Protocolo de reliable multicast a implementar

Barrels não pedem por indexação. Esse pedido é feito pelo SearchModule que coloca os URLs na fila de URLs.

Então o algoritmo pode ser o seguinte:

- Considera-se que um `Downloader` processou o *URL-X* e obteve todas as palavras;
- O `Downloader` envia uma mensagem para o grupo multicast com o *ID-X*;
- Um `Barrel` recebe a mensagem com *ID-X* e envia um "*ACK*";
- O `Downloader` apenas para de enviar a mensagem quando receber um número de "*ACK*s" igual ao número de `Barrel`s ligados ao grupo multicast.

# Problemas

1. Como é que um downloader sabe quantos `Barrel`s estão ligados ao grupo multicast?

2. E se o `Downloader` pensar que há *N* `Barrel`s ligados e na verdade um deles estiver avariado?

## Solução para 1 e 2

- Antes de enviar a mensagem, o `Downloader` envia um "*SIN*" para o grupo multicast com o *ID-X* (o ID da mensagem a enviar);
- Os `Barrel`s recebem o "*SIN*" com o *ID-X* e enviam um "*SINACK*" para confirmar que estão no grupo multicast;
- O `Downloader` considera que tem o grupo tem *N* `Barrel`s ligados ao grupo porque recebeu *N* "*SINACK*s"

Desta maneira o problema 2 mantém-se. Para o resolver basta considerar um timeout para o pedido ser cancelado.

3. E se um `Barrel` não tiver recebido a mensagem *N* quando o `Downloader` enviar a mensagem *N+1*? O que acontece? O `Barrel` perde a mensagem ou tem de a pedir de novo? Se tiver de a pedir de novo como é que o `Downloader` sabe qual o URL que a mensagem referia?