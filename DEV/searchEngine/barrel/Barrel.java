package searchEngine.barrel;

import searchEngine.ThreadSlave;


/**
 * Um {@code Downloader} é uma thread criada por um {@code Crawler}.
 * Esta corre em paralelo com o {@code Crawler} para fazer o download de
 * páginas web cujos links estão numa fila de espera num grupo de multicast.
 * 
 * Os links são adicionados à fila por um {@code Barrel}
 */
public class Barrel extends ThreadSlave{

    /**
     * Construtor da classe {@code Downloader}
     * 
     * @param id o id da thread
     * @param parentName o nome do objeto que criou esta thread
     */
    public Barrel(int id, String parentName) {
        super(id, parentName);
    }

    /**
     * Construtor por omissão da classe {@code Downloader}
     */
    public Barrel() {}

    @Override
    public void run() {
        printState();

        // fim
        System.out.println(this + " terminated!");
    }



    @Override
    public String toString() {
        return "Barrel" + super.toString();
    }

}

