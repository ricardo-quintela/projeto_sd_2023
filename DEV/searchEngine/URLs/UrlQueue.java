package searchEngine.URLs;

// import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class UrlQueue extends UnicastRemoteObject implements UrlQueueInterface {
    private CopyOnWriteArrayList<String> urls;
    
    private int rmiPort;
    private String rmiEndpoint;

    /**
     * Construtor da UrlQueue
     * @throws RemoteException
     */
    public UrlQueue(int rmiPort, String rmiEndpoint) throws RemoteException {
        urls = new CopyOnWriteArrayList<String>();
        this.rmiPort = rmiPort;
        this.rmiEndpoint = rmiEndpoint;
    }
    
    /** 
     * Addicona um url ao ultimo elemento da UrlQueue
     * @param url Url que vai ser adicionado a UrlQueue
     * @throws RemoteException
     */
    public void add(String url) throws RemoteException {
        System.out.println("Url adicionado à fila: " + url);
        urls.add(url);
    }

    /** 
     * Remove o primeiro elemento da UrlQueue
     * @return String elemento eliminado da UrlQueue
     * @throws RemoteException
     */
    public String remove() throws RemoteException {
        String url = urls.remove(urls.size()-1);
        System.out.println("Url removida da fila: " + url);
        return url;
    }

    /** 
     * Vai buscar o elemento da posicao "i" da UrlQueue
     * @param i posicao na UrlQueue
     * @return String elemento da posicao "i" da UrlQueue
     * @throws RemotehException;
     */
    public String get(int i) throws RemoteException {
        return urls.get(i);
    }

    /** 
     * Verifica se a UrLQueue esta vazia
     * @return boolean true se a UrlQueue nao tiver elementos
     * @throws RemoteException
     */
    public boolean isEmpty() throws RemoteException {
        return urls.isEmpty();
    }

    public static boolean register(int port, String endpoint, UrlQueue urlQueue) {
        Registry registry;

        // tentar criar o registo
        try {
            registry = LocateRegistry.createRegistry(port);
            System.out.println("Registo criado em 'localhost:" + port);
        } catch (RemoteException re) {
            
            // caso não seja possível criar uma referência para o registo tentar localiza-lo
            try {
                registry = LocateRegistry.getRegistry(port);
            } catch (RemoteException e) {

                System.out.println("Erro: Nao foi possivel criar o registo em 'localhost:" + port + "/" + endpoint + "'");
                return false;
            }
        }

        // tentar registar urlQueue no endpoint atribuido
        try {
            registry.bind(endpoint, urlQueue);
            System.out.println("UrlQueue registado em 'localhost:" + port + "/" + endpoint + "'");

        } catch (AlreadyBoundException e) {
            System.out.println("Erro: 'localhost:" + port + "/" + endpoint + "' ja foi atribuido!");

        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println("Erro: Ocorreu um erro a registar a UrlQueue em 'localhost:" + port + "/" + endpoint + "'");
            return false;
        }

        return true;
    }

    private static void printUsage() {
        System.out.println("Modo de uso:\nUrlQueue {rmi_port} {rmi_endpoint}\n- rmi_port: Porta do registo RMI da UrlQueue\n- rmi_endpoint: Endpoint da UrlQueue no registo RMI");
    }

    @Override
    public String toString() {
        return "UrlQueue@localhost:" + this.rmiPort + "/" + this.rmiEndpoint;
    }


    public static void main(String[] args) {

        // tratamento de erros nos parâmetros
        if (args.length != 2) {
            printUsage();
            return;
        }

        int rmiPort = -1;
        String rmiEndpoint;

        try {
            rmiPort = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException");
            return;
        }
        rmiEndpoint = args[1];

        // política e segurança
        System.getProperties().put("java.security.policy", "policy.all");
        System.setSecurityManager(new SecurityManager());

        // Instanciar uma urlQueue
        UrlQueue urlQueue;

        try {
            urlQueue = new UrlQueue(rmiPort, rmiEndpoint);
        } catch (RemoteException e) {
            System.out.println("Erro: Ocorreu um erro ao criar a UrlQueue!");
            return;
        }

        // tentar registar urlQueue
        if (!register(rmiPort, rmiEndpoint, urlQueue)) {
            return;
        }

    }
}
