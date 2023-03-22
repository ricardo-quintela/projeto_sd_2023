package searchEngine.search;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SearchResponse extends Remote {
    
    /**
     * Envia a resposta para o cliente que fez a query
     * @param response a resposta à query
     * @param barrel o nome do storage {@code Barrel}
     * @throws RemoteException caso haja um erro no RMI
     */
    public void postResponse(String response, String barrel) throws RemoteException;
}
