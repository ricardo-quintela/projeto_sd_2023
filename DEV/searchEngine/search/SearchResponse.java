package searchEngine.search;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.CopyOnWriteArrayList;

public interface SearchResponse extends Remote {
    
    /**
     * Envia uma query de palavras-chave para um {@code Barrel}
     * 
     * <br>
     * Tenta sequêncialmente fazer uma pesquisa por barrels até conseguir.
     * Caso não consiga ligar a nenhum da lista a pesquisa falha e é retornado false
     * 
     * @param name o nome do cliente
     * @param query a lista de palavras-chave
     * @return uma {@code String} contendo a pesquisa realizada pelo {@code Barrel}
     * @throws RemoteException caso haja um erro no RMI
     */
    public String execSearch(String name, CopyOnWriteArrayList<String> query) throws RemoteException;
}
