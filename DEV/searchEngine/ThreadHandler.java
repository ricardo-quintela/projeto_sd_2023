package searchEngine;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Um {@code ThreadHandeler} cria e adiciona instâncias de
 * {@code ThreadSlave} ao monitor
 */
public class ThreadHandler {

    protected CopyOnWriteArrayList<ThreadSlave> threads;
    
    /**
     * Construtor por omissão da classe {@code ThreadHandeler}
     * Inicializa as listas
     */
    public ThreadHandler() {
        this.threads = new CopyOnWriteArrayList<>();
    }
    
    /**
     * A partir de uma string fornecida tenta convertê-la para
     * inteiro e caso não consiga retorna -1
     * 
     * @param arg a string que deverá ser um argumento da consola
     * @return o inteiro convertido ou -1 em caso de erro
     */
    protected static int parseNumThreads(String arg) {
        int numThreads;

        try {
            numThreads = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return -1;
        }

        return numThreads;
    }
}
