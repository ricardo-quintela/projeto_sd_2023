package searchEngine.utils;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

/**
 * A classe log serve para facilitar a impressão de mensagens de informação e de erro
 */
public class Log{

    private DateTimeFormatter dtf;

    /**
     * Construtor por omissão da classe Log
     */
    public Log(){
        this.dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    }

    /**
     * Imprime uma mensagem de INFO no ficheiro de log e no stdout
     * @param entity a entidade que envia a mensagem
     * @param message a mensagem a enviar
     */
    public synchronized void info(String entity, String message){
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] |INFO| " + entity + ": " + message);
    }

    /**
     * Imprime uma mensagem de ERRO no ficheiro de log e no stdout
     * @param entity a entidade que envia a mensagem
     * @param message a mensagem a enviar
     */
    public synchronized void error(String entity, String message){
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] |ERRO| " + entity + ": " + message);
    }
}
