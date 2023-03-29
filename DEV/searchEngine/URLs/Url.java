package searchEngine.URLs;

import java.io.Serializable;

/**
 * Classe que serve para guardar um link e o seu id na fila
 */
public class Url implements Serializable{

    String hyperlink;
    int id;

    /**
     * Construtor por omissão da classe Url
     */
    public Url() {

    }

    /**
     * Construtor da classe Url
     * 
     * @param hyperlink o link do URL
     * @param id        o id do URL na fila
     */
    public Url(String hyperlink, int id) {
        this.hyperlink = hyperlink;
        this.id = id;
    }

    @Override
    public String toString() {
        return this.hyperlink;
    }

    public String getHyperlink() {
        return hyperlink;
    }

    public void setHyperlink(String hyperlink) {
        this.hyperlink = hyperlink;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    

}
