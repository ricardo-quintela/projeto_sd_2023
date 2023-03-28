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

    private int queuePort;
    private String queueEndpoint;
    private Log log;

    private MulticastSocket multicastSocket;
    private String multicastAddress;
    private int multicastPort;



    /**
     * Construtor por omissão da classe Downloader
     */
    public Downloader() throws IOException{
        this.multicastSocket = new MulticastSocket();
    }

    /**
     * Construtor da classe Downloader
     * @param queuePort a porta da fila de URLs
     * @param queueEndpoint o endpoint da fila de URLs
     * @throws IOException caso ocorra um erro a criar o MulticastSocket
     */
    public Downloader(int queuePort, String queueEndpoint, String multicastAddress, int multicastPort) throws IOException{
        this.queuePort = queuePort;
        this.queueEndpoint = queueEndpoint;

        this.log = new Log();

        this.multicastSocket = new MulticastSocket();
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
    }
    
    
    public void extractWords(String url){
        
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
            "Modo de uso:\nDownloader {rmi_port} {rmi_endpoint} {multicast_ip} {multicast_port}\n- rmi_port: Porto da fila de URLs\n-rmi_endpoint: Endpoint da fila de URLs\n- multicast_ip: O ip para qual vao ser transmitidas mensagens por multicast\n- multicast_port: O porto para onde as mensagens vao ser enviadas no Host de multicast"
            );
    }


    public static void main(String args[]) {

        // tratamento de parâmetros
        if (args.length != 4){
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
            downloader = new Downloader(rmiPort, args[1], args[2], multicastPort);
        } catch (IOException e){
            System.out.println("ERRO: Ocorreu um erro a criar o socket Multicast!");
            return;
        }

        UrlQueueInterface queue;
        try{

            // ligar ao server registado no rmiEndpoint fornecido
            queue = (UrlQueueInterface) LocateRegistry.getRegistry(downloader.queuePort).lookup(downloader.queueEndpoint);

            // Para remover quando a cena do scan for arranjada
            System.out.print(queue.remove());

            while (true) {
                String url = queue.remove();
                System.out.println(url);

                downloader.extractWords(url);
            }
            
        } catch (NotBoundException e) {
            System.out.println("Erro: não existe um servidor registado no endpoint '" + downloader.queueEndpoint + "'!");
            return;
        } catch (AccessException e) {
            System.out.println("Erro: Esta máquina não tem permissões para ligar ao endpoint '" + downloader.queueEndpoint + "'!");
            return;
        } catch (RemoteException e) {
            System.out.println("Erro: Não foi possível encontrar o registo");
            return;
        }
                
        downloader.closeSocket();

    }

}
