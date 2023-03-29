package searchEngine.URLs;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface UrlQueueInterface extends Remote {

    /**
     * Addicona um url ao ultimo elemento da UrlQueue
     * 
     * @param url Url que vai ser adicionado a UrlQueue
     * @throws RemoteException
     */
    public void add(String url) throws RemoteException;

    /**
     * Remove o primeiro elemento da UrlQueue
     * 
     * @param downloader o downloader que remove o url da queue
     * @return String elemento eliminado da UrlQueue
     * @throws RemoteException
     */
    public String remove(String downloader) throws RemoteException;

    /**
     * Verifica se a UrLQueue esta vazia
     * 
     * @return boolean true se a UrlQueue nao tiver elementos
     * @throws RemoteException
     */
    public boolean isEmpty() throws RemoteException;

    public int getNumDownloaders() throws RemoteException;
}
