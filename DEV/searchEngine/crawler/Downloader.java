package searchEngine.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import searchEngine.URLs.Url;
import searchEngine.URLs.UrlQueue;
import searchEngine.URLs.UrlQueueInterface;
import searchEngine.utils.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import java.net.URL;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.net.URLEncoder;
import java.rmi.registry.LocateRegistry;

/**
 * Um downloader analisa páginas da web e retira as palavras das mesmas
 * Envia as palavras por multicast para os barrels
 */
public class Downloader {

    private String name;
    private int queuePort;
    private String queueEndpoint;
    private Log log;
    private boolean running;

    private MulticastSocket multicastSocket;
    private int sockTimeout;
    private String multicastAddress;
    private int multicastPort;

    private InetAddress multicastGroup;

    private BlockingQueue<String> urls;

    private WordIndex wordIndex;

    /**
     * Construtor por omissão da classe Downloader
     */
    public Downloader() throws IOException {
        this.running = true;
        this.multicastSocket = new MulticastSocket();
        this.wordIndex = new WordIndex();
    }

    /**
     * 
     * Construtor da classe Downloader
     * 
     * @param queuePort        a porta da fila de URLs
     * @param queueEndpoint    o endpoint da fila de URLs
     * @param multicastAddress o endereço do grupo multicast dos Barrels
     * @param multicastPort    o porto do grupo multicast dos Barrels
     * @param name             o nome do downloader
     * @param sockTimeout          o tempo de espera do socket para receber resposta em ms
     * @throws IOException caso ocorra um erro a criar o MulticastSocket
     */
    public Downloader(int queuePort, String queueEndpoint, String multicastAddress, int multicastPort, String name,
            int sockTimeout) throws IOException {
        this.name = name;

        this.queuePort = queuePort;
        this.queueEndpoint = queueEndpoint;
        this.running = true;

        this.log = new Log();

        this.multicastSocket = new MulticastSocket(multicastPort);
        this.sockTimeout = sockTimeout;
        this.multicastSocket.setSoTimeout(sockTimeout);

        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;

        this.urls = new LinkedBlockingQueue<>();

        this.wordIndex = new WordIndex();
    }

    /**
     * Acede ao atributo running
     * 
     * @return o estado do atributo running
     */
    public boolean getRunning() {
        return this.running;
    }

    /**
     * Define o estado do atributo running
     * 
     * @param state o estado para alterar o atributo running
     */
    public void setRunning(boolean state) {
        this.running = state;
    }

    public boolean extractWords(String url, UrlQueueInterface queue) {

        try {

            // ligar ao website
            //Document doc = Jsoup.connect(url).charset("UTF-8").get();
            Document doc = Jsoup.parse(new URL(url).openStream(), "ISO-8859-1", url);

            // instanciar um string tokenizer para encontrar palavras
            StringTokenizer tokens = new StringTokenizer(doc.text());
            String aux;

            // array de tokens que vão ser separados de pontuação
            String cleanTokens[], texto = "", wordsForText[];
            int j = 0;

            while (tokens.hasMoreElements()) {

                aux = tokens.nextToken();

                // retirar pontuaçao dos tokens
                cleanTokens = aux.toLowerCase().split("[^a-zA-Z0-9]+");

                if (j++ == 0){
                    wordsForText = aux.split(" ;");
                    for (int i = 0; i < 10 && i < wordsForText.length; i++) {
                        texto += wordsForText[i];
                        if (i < 9) {
                            texto += " ";
                        }
                    }
                }

                if (cleanTokens.length == 0) {
                    continue;
                }

                // adicionar palavras ao indice
                for (int i = 0; i < cleanTokens.length; i++) {
                    this.wordIndex.put(url, cleanTokens[i]);
                }

            }

            this.wordIndex.setTexto(texto);
            this.wordIndex.setTitulo(doc.title());
            this.wordIndex.setUrl(url);

            // retirar as ligaçoes da pagina
            Elements links = doc.select("a[href]");

            // iterar por todas as ligaçoes
            for (Element link : links) {

                // Adicionar os urls a classe
                this.wordIndex.addLink(link.attr("abs:href"));
                queue.add(link.attr("abs:href"));

            }

        } catch (IllegalArgumentException e) {
            this.log.error(toString(), "O URL '" + url + "' esta num formato invalido!");
            return false;

        } catch (IOException e) {
            this.log.error(toString(), "Ocorreu um erro ao ligar a '" + url + "'!");
            return false;
        }

        return true;
    }

    /**
     * Tenta enviar a mensagem fornecida para o grupo multicast
     * 
     * @param urlIndex a mensagem para enviar por multicast
     * @param urlId    o id da mensagem a enviar
     * @return true caso a mensagem seja enviada; false caso contrário
     */
    public boolean sendMessage(String urlIndex, int urlId) {

        DatagramPacket sendPacket, rcvPacket;
        String receivedMessage[];

        // ? 1 - SETUP

        // buffer que recebe mensagens default de conexao
        byte[] connectionBuffer;

        // criar a mensagem
        String message = "type | url_list; id | " + urlId + "; " + urlIndex + ";";
        byte[] messageBuffer = message.getBytes();

        // ? 2 - ENVIAR HEARTBEAT

        // criar a mensagem heartbeat
        String connectionMessage = "type | heartbeat; id | " + urlId + "; buff_size | " + messageBuffer.length + ";";
        connectionBuffer = connectionMessage.getBytes();
        sendPacket = new DatagramPacket(connectionBuffer, connectionBuffer.length, this.multicastGroup,
                this.multicastPort);

        // continuar a enviar até conseguir
        while (true) {

            // enviar heartbeat
            try {

                this.log.info(toString(), "A enviar Heartbeat...");
                this.multicastSocket.send(sendPacket);
                this.log.info(toString(), "Heartbeat enviado!");

                break;

            } catch (IOException e) {
                this.log.error(toString(), "Ocorreu um erro a iniciar conexao para '" + this.multicastAddress + "'!");
            } catch (SecurityException e) {
                this.log.error(toString(), "Um SecurityManager nao permitiu o envio de um heartbeat para '"
                        + this.multicastAddress + "'!");
                return false;
            }
        }

        // ? 3 - RECEBER CONFIRMAÇÕES DE DISPONIBILIDADE

        int multicastSubscriberCount = 0;

        // preparar um novo packet para receber confirmaçoes
        connectionBuffer = new byte[50];
        rcvPacket = new DatagramPacket(connectionBuffer, connectionBuffer.length);

        // receber confirmações durante 2 segundos
        while (true) {

            // receber uma mensagem do grupo (BLOQUEANTE)
            try {

                this.log.info(toString(), "A escutar confirmacoes de Heartbeat...");
                this.multicastSocket.receive(rcvPacket);

            } catch (SocketTimeoutException e) {
                this.log.error(toString(), "Confirmacao de Heartbeat deu timeout!");
                break;

            } catch (IOException e) {
                this.log.error(toString(), "Ocorreu um erro a receber uma confirmacao de heartbeat!");
                continue;
            }

            // separar a mensagem por ";"
            receivedMessage = (new String(rcvPacket.getData(), 0, rcvPacket.getLength())).split(" *; *");

            // verificar se a mensagem é uma confirmaçao de heartbeat
            if (receivedMessage.length > 0 && receivedMessage[0].equals("type | ready")
                    && receivedMessage[1].equals("id | " + urlId)) {
                multicastSubscriberCount++;
                this.log.info(toString(), "Recebida confirmacao de Heartbeat! Total de " + multicastSubscriberCount);
            }

        }

        // ? 4 - ENVIAR A MENSAGEM PARA O GRUPO

        // preparar um novo pacote para enviar o indice
        sendPacket = new DatagramPacket(messageBuffer, messageBuffer.length, this.multicastGroup, this.multicastPort);

        // enviar a mensagem até ter atingido todas as confirmações ou até dar timeout
        while (true) {

            // enviar o indice
            try {

                this.log.info(toString(), "A enviar mensagem " + urlId + " para o grupo...");
                this.multicastSocket.send(sendPacket);
                this.log.info(toString(), "Mensagem enviada!");

            } catch (IOException e) {
                this.log.error(toString(), "Ocorreu um erro a enviar o indice para '" + this.multicastAddress + "'!");
                continue;

            } catch (SecurityException e) {
                this.log.error(toString(),
                        "Um SecurityManager nao permitiu o envio do indice para '" + this.multicastAddress + "'!");
                return false;
            }

            connectionBuffer = new byte[50];
            rcvPacket = new DatagramPacket(connectionBuffer, connectionBuffer.length);

            while (true){
                // receber uma mensagem do grupo (BLOQUEANTE)
                try {

                    this.log.info(toString(), "A escutar confirmacoes de envio para mensagem " + urlId + "...");
                    this.multicastSocket.receive(rcvPacket);

                } catch (SocketTimeoutException e) {
                    this.log.error(toString(), "Confirmacao de envio deu timeout!");
                    return false;

                } catch (IOException e) {
                    this.log.error(toString(), "Ocorreu um erro a receber uma confirmacao de envio!");
                    continue;
                }

                // separar a mensagem por ";"
                receivedMessage = (new String(rcvPacket.getData(), 0, rcvPacket.getLength())).split("; *");
                // //TODO: DEBUG CONFIRMACAO
                // System.out.println("================\n" + (new String(rcvPacket.getData(), 0, rcvPacket.getLength())).split(" *; *")[0] + "\n===================");

                // verificar se a mensagem é uma confirmaçao de envio
                if (receivedMessage.length > 0 && receivedMessage[0].equals("type | rcvd")
                        && receivedMessage[1].equals("id | " + urlId)) {

                    // decrementar caso seja confirmaçao
                    multicastSubscriberCount--;
                    this.log.info(toString(), "Confirmacao de envio recebida! Restantes: " + multicastSubscriberCount);


                    // recebeu todas as confirmações entao sai do metodo
                    if (multicastSubscriberCount <= 0) {
                        return true;
                    }

                    // a mensagem foi aceite, mas ainda nao tinha recebido todas as confirmacoes, entao volta a enviar
                    break;
                }

                // caso a mensagem recebida nao seja aceitavel tenta ouvir de novo a porta
            }

        }

    }

    /**
     * Liga à fila de URls por RMI e extrai urls para serem indexados
     * 
     * @return false caso ocorra um erro; true caso o programa seja terminado por um
     *         SIGINT
     */
    public boolean handleUrls() {
        // Ligar à fila de URls por RMI
        UrlQueueInterface queue;
        try {

            // ligar ao server registado no rmiEndpoint fornecido
            queue = (UrlQueueInterface) LocateRegistry.getRegistry(this.queuePort).lookup(this.queueEndpoint);

        } catch (NotBoundException e) {
            this.log.error(toString(), "não existe um servidor registado no endpoint '" + this.queueEndpoint + "'!");
            return false;
        } catch (AccessException e) {
            this.log.error(toString(),
                    "Esta máquina não tem permissões para ligar ao endpoint '" + this.queueEndpoint + "'!");
            return false;
        } catch (RemoteException e) {
            this.log.error(toString(), "Não foi possível encontrar o registo");
            return false;
        }

        // criar um grupo multicast
        try {
            this.multicastGroup = InetAddress.getByName(this.multicastAddress);
        } catch (UnknownHostException e) {
            this.log.error(toString(), "Nao foi possivel encontrar '" + this.multicastAddress + "'!");
            return false;
        } catch (SecurityException e) {
            this.log.error(toString(), "Um SecurityManager nao permitiu a ligacao a '" + this.multicastAddress + "'!");
            return false;
        }

        // juntar ao grupo multicast
        try {
            this.multicastSocket.joinGroup(this.multicastGroup);
        } catch (IOException e) {
            this.log.error(toString(), "Ocorreu um erro a juntar ao grupo multicast!");
            return false;
        }

        // Handeling de URLs
        Url url = null;
        try {

            while (this.getRunning()) {

                // resetar o mapa de palavras
                this.wordIndex.reset();

                // extrair um URL da fila (BLOQUEANTE)
                this.log.info(toString(),
                        "Procurando outro URL em 'localhost:" + this.queuePort + "/" + this.queueEndpoint + "'...");
                url = queue.remove(toString());
                this.log.info(toString(), "Recebido '" + url + "'. A extrair...");

                // analisar o website em URL e extrair as palavras para o indice
                if (this.extractWords(url.getHyperlink(), queue)) {
                    this.log.info(toString(), "'" + url + "'. Foi analisado.");
                }

                // enviar a mensagem para os Barrels
                System.out.println(wordIndex); // TODO: PRINT DE DEBUG

                this.log.info(toString(), "Comecando envio de indice!");

                // tentar enviar ate conseguir para nao perder o indice gerado
                while (!this.sendMessage(wordIndex.toString(), url.getId())){
                    this.log.error(toString(), "Tentando re-enviar o indice!");
                }
                this.log.info(toString(), "Indice enviado com sucesso!");

                // limpamos todos os urls encontrados
                this.urls.clear();

            }

        } catch (RemoteException e) {
            this.log.error(toString(), "Ocorreu um erro no registo da fila de URLs");
            return false;
        }

        return true;
    }

    /**
     * Fetcha a socket multicast
     */
    public void closeSocket() {
        this.multicastSocket.close();
    }

    /**
     * Imprime no {@code stdin} o modo de uso do programa
     */
    private static void printUsage() {
        System.out.println(
                "Modo de uso:\nDownloader {rmi_port} {rmi_endpoint} {multicast_ip} {multicast_port} {name}\n- rmi_port: Porto da fila de URLs\n- rmi_endpoint: Endpoint da fila de URLs\n- multicast_ip: O ip para qual vao ser transmitidas mensagens por multicast\n- multicast_port: O porto para onde as mensagens vao ser enviadas no Host de multicast\n- name: O nome do Downloader");
    }

    @Override
    public String toString() {
        return "Downloader-AKA:'" + this.name + "'";
    }

    public static void main(String args[]) {

        // tratamento de parâmetros
        if (args.length != 5) {
            printUsage();
            return;
        }

        // parsing dos parametros
        int rmiPort, multicastPort;
        try {
            rmiPort = Integer.parseInt(args[0]);
            multicastPort = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            printUsage();
            return;
        }

        // instanciar um Downloader e apanhar erros
        Downloader downloader;
        try {
            downloader = new Downloader(rmiPort, args[1], args[2], multicastPort, args[4], 2000);
        } catch (IOException e) {
            System.out.println("ERRO: Ocorreu um erro a criar o socket Multicast!");
            return;
        }

        // apanhar sinal SIGINT
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {

                synchronized (this) {
                    downloader.closeSocket();
                    downloader.setRunning(false);
                }
            }
        });

        // handeling de URLs
        if (!downloader.handleUrls()) {
            downloader.closeSocket();
        }

    }

}
