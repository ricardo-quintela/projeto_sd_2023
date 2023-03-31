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
    public CopyOnWriteArrayList<String> execSearch(String name, CopyOnWriteArrayList<String> query) throws RemoteException;


    /**
     * Envia um URL para ser adicionado na fila de URLs
     * 
     * <br>
     * 
     * @param url url a ser adicionado
     * @return true caso o URL seja adicionado à fila; false caso contrário
     * @throws RemoteException caso haja um erro no RMI
     */
    public boolean execURL(String url) throws RemoteException;


    /**
     * 
     * Tenta registar um novo utilizador na base de dados.
     * 
     * @param name
     * @param password
     * @return
     * @throws RemoteException
     */
    public boolean register(String name, String password) throws RemoteException;
    

    /**
     * 
     * Tenta encontrar um utilizador na base de dados.
     * 
     * @param name
     * @param password
     * @return
     * @throws RemoteException
     */
    public boolean login(String name, String password) throws RemoteException;


    public CopyOnWriteArrayList<String> searchUrl(String name, String query) throws RemoteException;


    public String admin() throws RemoteException;


    /**
     * 
     * Recebe a resposta obtida na pesquisa e retorna a parte da pagina 
     * a ser imprimida.
     * 
     * @param response Conjunto de Urls e suas informacoes
     * @param page Pagina a mostrar
     * @return Parte da response a ser apresentada
     * @throws RemoteException
     */
    public CopyOnWriteArrayList<String> pagination(CopyOnWriteArrayList<String> response, int page) throws RemoteException;
}
