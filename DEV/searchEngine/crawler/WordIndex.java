package searchEngine.crawler;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Um buffer contém informação que vai sendo extraida continuamente para depois ser
 * usada toda de uma vez
 */
public class WordIndex {

    protected HashMap<String, HashSet<String>> hashMap;

    /**
     * Construtor por omissão da classe {@code Buffer}
     */
    public WordIndex(){
        this.hashMap = new HashMap<>();
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

        this.hashMap.put(key, new HashSet<>()).add(value);

    }


    @Override
    public String toString() {
        String string = "";

        // iterar pelas palavras guardadas no INDEX de URLS
        int i = 0, j;
        for (String key : this.hashMap.keySet()) {
            string = string + " ; item_" + i + "_" + key + " | "; // item_I_palavra | url1, url2
            
            // iterar pelos URL da palavra selecionada
            j = 0;
            for (String value : this.hashMap.get(key)) {
                string = string + value;

                // adicionar um separador aos URL
                if (j < this.hashMap.get(key).size()){
                    string += ", ";
                }
            }

            i += 1;
        }

        return string;
    }
    
}
