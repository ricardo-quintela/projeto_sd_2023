package searchEngine.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import searchEngine.URLs.Url;
import searchEngine.URLs.UrlQueueInterface;
import searchEngine.utils.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.time.Duration;
import java.time.Instant;


/**
 * Um downloader analisa páginas da web e retira as palavras das mesmas
 * Envia as palavras por multicast para os barrels
 */
public class Downloader {

    private String name;
    private int queuePort;
    private String queueEndpoint;
    private Log log;
    boolean running;

    private MulticastSocket multicastSocket;
    private String multicastAddress;
    private int multicastPort;

    private BlockingQueue<String> urls;

    private WordIndex wordIndex;



    /**
     * Construtor por omissão da classe Downloader
     */
    public Downloader() throws IOException{
        this.running = true;
        this.multicastSocket = new MulticastSocket();
        this.wordIndex = new WordIndex();
    }

    /**
     * 
     * Construtor da classe Downloader
     * @param queuePort a porta da fila de URLs
     * @param queueEndpoint o endpoint da fila de URLs
     * @param multicastAddress o endereço do grupo multicast dos Barrels
     * @param multicastPort o porto do grupo multicast dos Barrels
     * @param name o nome do downloader
     * @throws IOException caso ocorra um erro a criar o MulticastSocket
     */
    public Downloader(int queuePort, String queueEndpoint, String multicastAddress, int multicastPort, String name) throws IOException{
        this.name = name;

        this.queuePort = queuePort;
        this.queueEndpoint = queueEndpoint;
        this.running = true;

        this.log = new Log();

        this.multicastSocket = new MulticastSocket(multicastPort);
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;

        this.urls = new LinkedBlockingQueue<>();

        this.wordIndex = new WordIndex();
    }


    /**
     * Acede ao atributo running
     * @return o estado do atributo running
     */
    public boolean getRunning(){
        return this.running;
    }

    /**
     * Define o estado do atributo running
     * @param state o estado para alterar o atributo running
     */
    public void setRunning(boolean state){
        this.running = state;
    }
    
    
    public boolean extractWords(String url){

        try {
            // ligar ao website
            Document doc = Jsoup.connect(url).get();

            // instanciar um string tokenizer para encontrar palavras
            StringTokenizer tokens = new StringTokenizer(doc.text());

            // array de tokens que vão ser separados de pontuação
            String cleanTokens[];

            while (tokens.hasMoreElements()){

                // retirar pontuaçao dos tokens
                cleanTokens = tokens.nextToken().toLowerCase().split("[^a-zA-Z0-9]+");

                if (cleanTokens.length == 0){
                    continue;
                }

                // adicionar palavras ao indice
                for (int i = 0; i < cleanTokens.length; i++){
                    this.wordIndex.put(url, cleanTokens[i]);
                }

            }
            
            // retirar as ligaçoes da pagina
            Elements links = doc.select("a[href]");
            
            // iterar por todas as ligaçoes
            for (Element link : links) {

                // Voltar a pôr os links na lista para eles também serem procurados
                if (!this.urls.contains(link.attr("abs:href"))){
                    this.urls.add(link.attr("abs:href"));
                }

                // TODO: PRINT DE DEBUG
                System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
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
     * @param urlIndex a mensagem para enviar por multicast
     * @param urlId o id da mensagem a enviar
     * @return true caso a mensagem seja enviada; false caso contrário
     */
    public boolean sendMessage(String urlIndex, int urlId){
        
        InetAddress multicastGroup;
        DatagramPacket sendPacket, rcvPacket;
        Instant start, end;
        String receivedMessage[];

        //? 1 - SETUP

        // buffer que recebe mensagens default de conexao
        byte[] connectionBuffer;
        
        // criar um grupo multicast
        try {
            multicastGroup = InetAddress.getByName(this.multicastAddress);
        } catch (UnknownHostException e){
            this.log.error(toString(), "Nao foi possivel encontrar '" + this.multicastAddress + "'!");
            return false;
        } catch (SecurityException e){
            this.log.error(toString(), "Um SecurityManager nao permitiu a ligacao a '" + this.multicastAddress + "'!");
            return false;
        }

        // juntar ao grupo multicast
        try {
            this.multicastSocket.joinGroup(multicastGroup);
        } catch (IOException e){
            this.log.error(toString(), "Ocorreu um erro a juntar ao grupo multicast!");
            return false;
        }

        // criar a mensagem
        String message = "type | url_list; id | " + urlId + "; " + urlIndex;
        byte[] messageBuffer = message.getBytes();
        
        
        //? 2 - PEDIR PARA ENVIAR MENSAGEM
        
        // criar a mensagem heartbeat
        String connectionMessage = "type | heartbeat; id | " + urlId + "; buff_size | " + messageBuffer.length;
        connectionBuffer = connectionMessage.getBytes();
        sendPacket = new DatagramPacket(connectionBuffer, connectionBuffer.length, multicastGroup, this.multicastPort);
        
        // enviar heartbeat
        try {
            this.multicastSocket.send(sendPacket);

        } catch (IOException e) {
            this.log.error(toString(), "Ocorreu um erro a iniciar conexao para '" + this.multicastAddress + "'!");
        } catch (SecurityException e){
            this.log.error(toString(), "Um SecurityManager nao permitiu o envio de um heartbeat para '" + this.multicastAddress + "'!");
            return false;
        }

        //? 3 - RECEBER CONFIRMAÇÕES DE DISPONIBILIDADE

        int multicastSubscriberCount = 0;
        start = Instant.now();
        end = Instant.now();
        
        // preparar um novo packet para receber confirmaçoes
        connectionBuffer = new byte[50];
        rcvPacket = new DatagramPacket(connectionBuffer, connectionBuffer.length);

        // receber confirmações durante 2 segundos
        while (Duration.between(start, end).compareTo(Duration.ofSeconds(2)) < 0){

            // receber uma mensagem do grupo (BLOQUEANTE)
            try {
                this.multicastSocket.receive(rcvPacket);
            } catch (IOException e) {
                this.log.error(toString(), "Ocorreu um erro a receber uma confirmacao de heartbeat!");
                continue;
            }

            // separar a mensagem por ";"
            receivedMessage = (new String(rcvPacket.getData(), 0, rcvPacket.getLength())).split("; *");

            // verificar se a mensagem é uma confirmaçao de heartbeat
            if (receivedMessage.length > 0 && receivedMessage[0].equals("type | ready") && receivedMessage[1].equals("id | " + urlId)){
                multicastSubscriberCount++;
            }

        }

        //? 4 - ENVIAR A MENSAGEM PARA O GRUPO

        // preparar um novo pacote para enviar o indice
        sendPacket = new DatagramPacket(messageBuffer, messageBuffer.length, multicastGroup, this.multicastPort);
        
        start = Instant.now();
        end = Instant.now();

        // enviar a mensagem até ter atingido todas as confirmações ou ter enviado várias vezes
        while (Duration.between(start, end).compareTo(Duration.ofSeconds(2)) < 0) {

            // enviar o indice
            try {
                this.multicastSocket.send(sendPacket);
    
            } catch (IOException e) {
                this.log.error(toString(), "Ocorreu um erro a enviar o indice para '" + this.multicastAddress + "'!");
            } catch (SecurityException e){
                this.log.error(toString(), "Um SecurityManager nao permitiu o envio do indice para '" + this.multicastAddress + "'!");
                return false;
            }

            connectionBuffer = new byte[50];
            rcvPacket = new DatagramPacket(connectionBuffer, connectionBuffer.length);


            // receber uma mensagem do grupo (BLOQUEANTE)
            try {
                this.multicastSocket.receive(rcvPacket);
            } catch (IOException e) {
                this.log.error(toString(), "Ocorreu um erro a receber uma confirmacao de envio!");
                continue;
            }

            // separar a mensagem por ";"
            receivedMessage = (new String(rcvPacket.getData(), 0, rcvPacket.getLength())).split("; *");

            // verificar se a mensagem é uma confirmaçao de heartbeat
            if (receivedMessage.length > 0 && receivedMessage[0].equals("type | rcvd") && receivedMessage[1].equals("id | " + urlId)){
                
                // decrementar caso seja confirmaçao
                multicastSubscriberCount--;
            }

            // recebeu todas as confirmações
            if (multicastSubscriberCount == 0){
                break;
            }

        }

        return true;

    }


    /**
     * Liga à fila de URls por RMI e extrai urls para serem indexados
     * @return false caso ocorra um erro; true caso o programa seja terminado por um SIGINT
     */
    public boolean handleUrls(){
        // Ligar à fila de URls por RMI
        UrlQueueInterface queue;
        try{
            
            // ligar ao server registado no rmiEndpoint fornecido
            queue = (UrlQueueInterface) LocateRegistry.getRegistry(this.queuePort).lookup(this.queueEndpoint);
            
        } catch (NotBoundException e) {
            this.log.error(toString(), "não existe um servidor registado no endpoint '" + this.queueEndpoint + "'!");
            return false;
        } catch (AccessException e) {
            this.log.error(toString(), "Esta máquina não tem permissões para ligar ao endpoint '" + this.queueEndpoint + "'!");
            return false;
        } catch (RemoteException e) {
            this.log.error(toString(), "Não foi possível encontrar o registo");
            return false;
        }
        
        // Handeling de URLs
        Url url = null;
        try {

            while (this.getRunning()) {

                // resetar o mapa de palavras
                this.wordIndex.reset();

                // extrair um URL da fila (BLOQUEANTE)
                this.log.info(toString(), "Procurando outro URL em 'localhost:" + this.queuePort + "/" + this.queueEndpoint + "'...");
                url = queue.remove(toString());
                this.log.info(toString(), "Recebido '" + url + "'. A extrair...");
                
                // analisar o website em URL e extrair as palavras para o indice
                if (this.extractWords(url.getHyperlink())) {
                    this.log.info(toString(), "'" + url + "'. Foi analisado.");
                }

                // iterar por todos os novos URLs retirados do URL principal
                for (String childUrl: this.urls) {
                    this.log.info(toString(), "A analisar '" + url + "'.");

                    // analisar o website em URL e extrair as palavras para o indice
                    if (this.extractWords(childUrl)) {
                        this.log.info(toString(), "'" + url + "'. Foi analisado.");
                    }
                }

                // enviar a mensagem para os Barrels
                System.out.println(wordIndex); //TODO: PRINT DE DEBUG
                this.sendMessage(wordIndex.toString(), url.getId());

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
    public void closeSocket(){
        this.multicastSocket.close();
    }


    /**
     * Imprime no {@code stdin} o modo de uso do programa
     */
    private static void printUsage() {
        System.out.println(
            "Modo de uso:\nDownloader {rmi_port} {rmi_endpoint} {multicast_ip} {multicast_port} {name}\n- rmi_port: Porto da fila de URLs\n- rmi_endpoint: Endpoint da fila de URLs\n- multicast_ip: O ip para qual vao ser transmitidas mensagens por multicast\n- multicast_port: O porto para onde as mensagens vao ser enviadas no Host de multicast\n- name: O nome do Downloader"
            );
    }


    @Override
    public String toString() {
        return "Downloader-AKA:'" + this.name + "'";
    }


    public static void main(String args[]) {

        // tratamento de parâmetros
        if (args.length != 5){
            printUsage();
            return;
        }

        // parsing dos parametros
        int rmiPort, multicastPort;
        try{
            rmiPort = Integer.parseInt(args[0]);
            multicastPort = Integer.parseInt(args[3]);
        } catch (NumberFormatException e){
            printUsage();
            return;
        }

        // instanciar um Downloader e apanhar erros
        Downloader downloader;
        try {
            downloader = new Downloader(rmiPort, args[1], args[2], multicastPort, args[4]);
        } catch (IOException e){
            System.out.println("ERRO: Ocorreu um erro a criar o socket Multicast!");
            return;
        }

        // apanhar sinal SIGINT
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {

                synchronized (this){
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
