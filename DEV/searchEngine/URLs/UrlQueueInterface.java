package searchEngine.URLs;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface UrlQueueInterface extends Remote {

    /**
     * Addicona um url ao ultimo elemento da UrlQueue
     * 
     * @param url Url que vai ser adicionado a UrlQueue
     * @throws RemoteException caso ocorra um erro no RMI
     */
    public void add(String url) throws RemoteException;

    /**
     * Remove o primeiro elemento da UrlQueue
     * 
     * @param downloader o downloader que remove o url da queue
     * @return Url eliminado da UrlQueue
     * @throws RemoteException caso ocorra um erro no RMI
     */
    public Url remove(String downloader) throws RemoteException;

    /**
     * Verifica se a UrLQueue esta vazia
     * 
     * @return true se a UrlQueue nao tiver elementos; false caso contrario
     * @throws RemoteException caso ocorra um erro no RMI
     */
    public boolean isEmpty() throws RemoteException;
}
