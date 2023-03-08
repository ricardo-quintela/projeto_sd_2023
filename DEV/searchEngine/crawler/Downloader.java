package searchEngine.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import searchEngine.ThreadSlave;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Um {@code Downloader} é uma thread criada por um {@code Crawler}.
 * Esta corre em paralelo com o {@code Crawler} para fazer o download de
 * páginas web cujos links estão numa fila de espera num grupo de multicast.
 * 
 * Os links são adicionados à fila por um {@code Barrel}
 */
public class Downloader extends ThreadSlave{

    /**
     * Construtor da classe {@code Downloader}
     * 
     * @param id o id da thread
     * @param parentName o nome do objeto que criou esta thread
     */
    public Downloader(int id, String parentName) {
        super(id, parentName);
    }

    /**
     * Construtor por omissão da classe {@code Downloader}
     */
    public Downloader() {}

    @Override
    public void run() {
        printState();

        // fim
        System.out.println(this + " terminated!");
    }

    private void collectFromURL(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            StringTokenizer tokens = new StringTokenizer(doc.text());
            int countTokens = 0;
            while (tokens.hasMoreElements() && countTokens++ < 100)
                System.out.println(tokens.nextToken().toLowerCase());
            Elements links = doc.select("a[href]");
            for (Element link : links)
                System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String toString() {
        return "Downloader" + super.toString();
    }

}
