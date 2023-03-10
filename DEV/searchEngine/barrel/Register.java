package searchEngine.barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * Interface que define métodos usados pela thread pai
 * dos {@code Barrel}
 * 
 * Servidores podem assim subscrever, cancelar subscrições
 * e um {@code SearchModule} pode obter informações sobre
 * o estado dos servidores
 */
public interface Register extends Remote {
    
    /**
     * Permite que o {@code Barrel} subscreva ao servidor {@code BarrelHandler}
     * através da sua interface
     * @param name o nome do barrel que vai subscrever
     * @param barrel a interface do storage barrel que vai subscrever
     * @return true caso consiga subscrever; false caso caontrario
     * @throws RemoteException caso ocorra um erro no RMI
     */
    public boolean subscribe(String name, SearchRequest barrel) throws RemoteException;
    
    /**
     * Permite que o {@code Barrel} cancele a subscrição ao servidor {@code BarrelHandler}
     * @param barrel o storage {@code Barrel} que vai cancelar a subscrição
     * @return true caso consiga cancelar a subscrição; false caso caontrario
     * @throws RemoteException caso ocorra um erro no RMI
     */
    public boolean unsubcribe(int id) throws RemoteException;

    /**
     * Retorna a interface do {@code Barrel} no índice fornecido
     * @return o {@code Barrel} com o {@code id} forncecido
     * @param id o identificador do Barrel
     * @throws RemoteException caso ocorra um erro no RMI
     */
    public SearchRequest getBarrel(int id) throws RemoteException;
}
