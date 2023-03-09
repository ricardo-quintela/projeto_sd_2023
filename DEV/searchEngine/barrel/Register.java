package searchEngine.barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Register extends Remote {
    
    /**
     * Permite que o {@code Barrel} subscreva ao servidor {@code BarrelHandler}
     * @param barrel o storage {@code Barrel} que vai subscrever
     * @return true caso consiga subscrever; false caso caontrario
     * @throws RemoteException caso ocorra um erro no server
     */
    public boolean subscribe(Barrel barrel) throws RemoteException;
    
    /**
     * Permite que o {@code Barrel} cancele a subscrição ao servidor {@code BarrelHandler}
     * @param barrel o storage {@code Barrel} que vai cancelar a subscrição
     * @return true caso consiga cancelar a subscrição; false caso caontrario
     * @throws RemoteException caso ocorra um erro no server
     */
    public boolean unsubcribe(int id) throws RemoteException;

}
