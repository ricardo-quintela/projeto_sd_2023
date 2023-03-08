package searchEngine.barrel;

import searchEngine.ThreadHandler;
import searchEngine.ThreadSlave;


public class BarrelHandler extends ThreadHandler {

    /**
     * Construtor por omissão da classe {@code BarrelHandeler}
     */
    public BarrelHandler() {
    }

    public static void main(String[] args) {
        // tratamento de erros nos parâmetros iniciais
        if (args.length == 0) {
            System.out.println("ERRO: Deve ser especificado o número de threads Crawler!");
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


        // criar um objeto Crawler
        BarrelHandler crawler = new BarrelHandler();

        // criar os fetchers
        for (int i = 0; i < numThreads; i++) {
            crawler.threads.add(new Barrel(i, "BarrelHandler"));
        }

        try{
            // esperar pelo término das threads
            for (ThreadSlave downloader : crawler.threads) {
                downloader.getThread().join();
            }
        } catch(InterruptedException e){
            System.out.println("Barrel foi interrompido!");
        }
    }
    
    
}
