package downloader;

import java.util.concurrent.CopyOnWriteArrayList;

public class Downloader {

    private CopyOnWriteArrayList<Fetcher> fetchers;

    /**
     * Construtor por omissão da classe Downloader
     * Inicializa o atributo fetchers
     */
    public Downloader() {
        this.fetchers = new CopyOnWriteArrayList<Fetcher>();
    }

    private static int parseNumThreads(String arg) {
        int numThreads;

        try {
            numThreads = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return -1;
        }

        return numThreads;
    }

    /**
     * Método principal onde todos os outros serão chamados
     * 
     * @param args os argumentos da consola
     */
    public static void main(String[] args) {

        // tratamento de erros nos parâmetros iniciais
        if (args.length == 0) {
            System.out.println("ERRO: Deve ser especificado o número de threads downloader!");
            return;
        }

        if (args.length > 1) {
            System.out.println("ERRO: Parametros a mais!");
            return;
        }

        // fazer parse do numero de threads
        int numThreads = parseNumThreads(args[0]);

        if (numThreads < 1) {
            System.out.println("ERRO: numero de threads invalido!");
            return;
        }

        // criar um objeto downloader
        Downloader downloader = new Downloader();

        // criar os fetchers
        for (int i = 0; i < numThreads; i++) {
            downloader.fetchers.add(new Fetcher(i));
        }

        try{
            // esperar pelo término das threads
            for (Fetcher fetcher : downloader.fetchers) {
                fetcher.getThread().join();
            }
        } catch(InterruptedException e){
            System.out.println("Fetcher foi interrompido!");
        }
        

    }
}
