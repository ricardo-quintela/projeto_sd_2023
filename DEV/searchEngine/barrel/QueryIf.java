package searchEngine.barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface QueryIf extends Remote{
    
    /**
     * Através de uma query de palavras introduzidas por um utilizador
     * é pedido a um storage barrel que retorne os links em que as palavras pedidas
     * aparecem
     * 
     * @param query o conjunto de palavras a pesquisar
     * @return o conjunto de links em que as palavras aparecem
     * @throws RemoteException se ocorrer um erro do lado do server
     */
    public String execQuery(ArrayList<String> query) throws RemoteException;

}