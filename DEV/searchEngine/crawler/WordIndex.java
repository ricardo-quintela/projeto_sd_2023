package searchEngine.crawler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Um buffer contém informação que vai sendo extraida continuamente para depois ser
 * usada toda de uma vez
 */
public class WordIndex {

    private HashMap<String, HashSet<String>> hashMap;
    private LinkedList<String> links;
    private String titulo;
    private String texto;
    private String url;

    /**
     * Construtor por omissão da classe {@code WordIndex}
     */
    public WordIndex(){
        this.hashMap = new HashMap<>();
        this.links = new LinkedList<>();
    }

    /**
     * Adiciona um valor ao buffer
     * @param value o valor a adicionar ao buffer
     */
    public void put(String key, String value){
        if (this.hashMap.containsKey(key)){
            this.hashMap.get(key).add(value);
            return;
        }

        HashSet<String> hashSet = new HashSet<>();
        hashSet.add(value);

        hashSet = this.hashMap.put(key, hashSet);
    }

    
    public void setUrl(String url) {
        this.url = url;
    }

    public void addLink(String link) {
        this.links.add(link);
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    /**
     * Esvazia o indice de palavras
     */
    public void reset(){
        this.hashMap.clear();
        this.links.clear();
        this.url = null;
    }

    // receivedMessage.text() = [[type | url_list], [msgid | 1], [url | www.google.pt], [titulo | sadfasdfasdf], [texto | asd asd asd asd], [palavras | asdf,sadf,sadf,sadf],  [children mf | a,a,a,a,a,a]]

    @Override
    public String toString() {

        String string = "url | " + this.url + "; titulo | " + this.titulo + "; texto | " + this.texto + "; words | ";

        // iterar pelas palavras guardadas no INDEX de URLS
        int j;
        for (String key : this.hashMap.keySet()) {

            if (!key.equals(this.url)) {
                continue;
            }

            // iterar pelos URL da palavra selecionada
            j = 0;
            for (String value : this.hashMap.get(key)) {
                string = string + value;

                // adicionar um separador aos URL
                j ++;
                if (j < this.hashMap.get(key).size() - 1){
                    string += ", ";
                }
            }
        }

        string = string + "; children | ";
        j = 0;
        for (String link : this.links) {
            string += link;
            j ++;

            // adicionar um separador aos URL
            if (j < this.links.size() - 1){
                string += ", ";
            }
        }

        return string;
    }
    
}
