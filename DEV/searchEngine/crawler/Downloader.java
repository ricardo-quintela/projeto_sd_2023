package searchEngine.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Um downloader analisa páginas da web e retira as palavras das mesmas
 * Envia as palavras por multicast para os barrels
 */
public class Downloader {

    private int queuePort;
    private String queueEndpoint;


    /**
     * Construtor por omissão da classe Downloader
     */
    public Downloader(){
    }

    /**
     * Construtor da classe Downloader
     * @param queuePort a porta da fila de URLs
     * @param queueEndpoint o endpoint da fila de URLs
     */
    public Downloader(int queuePort, String queueEndpoint){
        this.queuePort = queuePort;
        this.queueEndpoint = queueEndpoint;
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
    }


    /**
     * Imprime no {@code stdin} o modo de uso do programa
     */
    private static void printUsage() {
        System.out.println("Modo de uso:\nDownloader {rmi_port} {rmi_endpoint}\n- rmi_port: Porto da fila de URLs\n-rmi_endpoint: Endpoint da fila de URLs");
    }


    public static void main(String args[]) {

        if (args.length != 2){
            printUsage();
            return;
        }

        int rmi_port;
        try{
            rmi_port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e){
            printUsage();
            return;
        }

        Downloader downloader = new Downloader(rmi_port, args[1]);

        // instanciar um scanner
        Scanner sc = new Scanner(System.in);

        System.out.print("Insira um URL >>>");

        String url = sc.nextLine();

        downloader.extractWords(url);

        sc.close();

    }

}
