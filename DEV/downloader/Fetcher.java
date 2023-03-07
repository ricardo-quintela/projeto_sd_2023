package downloader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Um Fetcher é uma thread criada por um Downloader.
 * Esta corre em paralelo com o Downloader para fazer o download de
 * páginas web.
 */
public class Fetcher implements Runnable {
    private int id;
    private Thread thread;
    private boolean state;

    /**
     * Construtor da classe Fetcher
     * 
     * @param id o id da thread
     */
    public Fetcher(int id) {
        this.id = id;
        this.state = false;

        // criar e inicializar a thread
        this.thread = new Thread(this);
        System.out.println(this + " created!");
        this.thread.start();
    }

    /**
     * Construtor por omissão da classe Fetcher
     */
    public Fetcher() {
    }

    @Override
    public void run() {
        this.state = true;
        printState();

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

    /**
     * Imprime no stdout o estado da thread
     */
    public synchronized void printState() {
        if (getState()) {
            System.out.println(this + " is active!");
            return;
        }
        System.out.println(this + " is stopped!");
    }

    public int getId() {
        return id;
    }

    public Thread getThread() {
        return thread;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "fetcher#" + this.id + "@Downloader";
    }

}
