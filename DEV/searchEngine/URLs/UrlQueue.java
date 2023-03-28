package searchEngine.URLs;

// import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import searchEngine.utils.Log;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class UrlQueue extends UnicastRemoteObject implements UrlQueueInterface {
    private BlockingQueue<Url> urls;
    
    private int rmiPort;
    private String rmiEndpoint;
    private Log log;
    private int urlCounter;


    /**
     * Construtor por omissão da classe UrlQueue
     * @throws RemoteException caso ocorra um erro no RMI
     */
    public UrlQueue() throws RemoteException{
        this.urls = new LinkedBlockingQueue<Url>();
        this.urlCounter = 0;
    }

    /**
     * Construtor da classe UrlQueue
     * @param rmiPort o porto onde criar o registo RMI
     * @param rmiEndpoint o endpoint do RMI onde registar a fila
     * @throws RemoteException caso ocorra um erro no RMI
     */
    public UrlQueue(int rmiPort, String rmiEndpoint) throws RemoteException {
        this.urls = new LinkedBlockingQueue<Url>();
        this.rmiPort = rmiPort;
        this.rmiEndpoint = rmiEndpoint;
        this.log = new Log();
        this.urlCounter = 0;
    }
    

    public void add(String url) throws RemoteException {
        this.log.info(toString(), "Url adicionado à fila: " + url);

        try {
            urls.put(new Url(url, ++urlCounter));
            
        } catch (InterruptedException e) {
            this.log.error(toString(), "Nao foi possivel adicionar '" + url + "' a fila!");
            return;
        }
    }


    public Url remove(String downloader) throws RemoteException {
        try {
            Url url = urls.take();
            this.log.error(toString(), "Url removida da fila por " + downloader + ": " + url);
            return url;
        } catch (InterruptedException e) {
            this.log.error(toString(), "Registo estava interrompido");
            return null;
        }
    }

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

    /**
     * Imprime a mensagem de uso da fila de URLs
     */
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
