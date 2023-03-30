package searchEngine.barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.CopyOnWriteArrayList;

public interface QueryIf extends Remote {

    /**
     * Através de uma query de palavras introduzidas por um utilizador
     * é pedido a um storage barrel que retorne os links em que as palavras pedidas
     * aparecem
     * 
     * @param query o conjunto de palavras a pesquisar
     * @return o conjunto de links em que as palavras aparecem
     * @throws RemoteException se ocorrer um erro do lado do server
     */
    public CopyOnWriteArrayList<String> execQuery(CopyOnWriteArrayList<String> query) throws RemoteException;

    public CopyOnWriteArrayList<String> execURL(String url) throws RemoteException;

    /**
     * Retorna o caminho do ficheiro da base de dados
     * 
     * @return o caminho do ficheiro da base de dados
     * @throws RemoteException caso ocorra um erro no RMI
     */
    public String getDataBaseFile() throws RemoteException;

}
