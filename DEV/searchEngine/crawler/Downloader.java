package searchEngine.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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



    /**
     * Construtor por omissão da classe Downloader
     */
    public Downloader() throws IOException{
        this.running = true;
        this.multicastSocket = new MulticastSocket();
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

        this.multicastSocket = new MulticastSocket();
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
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
    
    
    public void extractWords(String url, UrlQueueInterface urlQueue){
        
        // criar um mapa de palavras
        WordIndex wordIndex = new WordIndex();

        try {
            // ligar ao website
            Document doc = Jsoup.connect(url).get();

            // instanciar um string tokenizer para encontrar palavras
            StringTokenizer tokens = new StringTokenizer(doc.text());

            // array de tokens que vão ser separados de pontuação
            String cleanedTokens[];

            while (tokens.hasMoreElements()){

                cleanedTokens = tokens.nextToken().toLowerCase().split("[^a-zA-Z0-9]+");

                if (cleanedTokens.length == 0){
                    continue;
                }

                for (int i = 0; i < cleanedTokens.length; i++){
                    wordIndex.put(url, cleanedTokens[i]);
                }

            }
            
            Elements links = doc.select("a[href]");
            
            for (Element link : links) {
                System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
            }
        
        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println(wordIndex);
        this.sendMessage(wordIndex.toString());
    }



    /**
     * Tenta enviar a mensagem fornecida para o grupo multicast
     * @param message a mensagem para enviar por multicast
     * @return true caso a mensagem seja enviada; false caso contrário
     */
    public boolean sendMessage(String message){

        InetAddress multicastGroup;
        byte[] buffer;
        
        try {
            multicastGroup = InetAddress.getByName(this.multicastAddress);
        } catch (UnknownHostException e){
            this.log.error(toString(), "Nao foi possivel encontrar '" + this.multicastAddress + "'!");
            return false;
        } catch (SecurityException e){
            this.log.error(toString(), "Um SecurityManager nao permitiu a ligacao a '" + this.multicastAddress + "'!");
            return false;
        }
        
        buffer = message.getBytes();
        
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, multicastGroup, this.multicastPort);
        
        
        try {
            this.multicastSocket.send(packet);
        } catch (IOException e){
            this.log.error(toString(), "Um SecurityManager nao permitiu o envio de um pacote para '" + this.multicastAddress + "'!");
            return false;
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
        String url = null;
        while (this.getRunning()) {
            try {


                this.log.info(toString(), "Procurando outro URL em 'localhost:" + this.queuePort + "/" + this.queueEndpoint + "'...");
                url = queue.remove(toString());

                this.log.info(toString(), "Recebido '" + url + "'. A extrair...");
                
                this.extractWords(url, queue);
                this.log.info(toString(), "'" + url + "'. Foi analisado.");

            } catch (IllegalArgumentException e) {
                this.log.error(toString(), "O URL '" + url + "' esta num formato invalido!");
            } catch (RemoteException e) {
                this.log.error(toString(), "Ocorreu um erro no registo da fila de URLs");
                return false;
            }
            
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
