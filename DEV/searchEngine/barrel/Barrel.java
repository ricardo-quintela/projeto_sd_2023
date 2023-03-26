package searchEngine.barrel;

import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CopyOnWriteArrayList;

import searchEngine.utils.Log;

public class Barrel extends UnicastRemoteObject implements QueryIf {

    private int rmiPort;
    private String rmiEndpoint;

    private Log log;

    /**
     * Construtor por omissão da classe {@code Barrel}
     */
    public Barrel(int rmiPort, String rmiEndpoint) throws RemoteException {
        this.rmiPort = rmiPort;
        this.rmiEndpoint = rmiEndpoint;
        this.log = new Log();
    }

    /**
     * Construtor por omissão da classe {@code Barrel}
     */
    public Barrel() throws RemoteException {
        this.log = new Log();
    }

    @Override
    public String execQuery(CopyOnWriteArrayList<String> query) throws RemoteException {
        String string = "";

        for (String word : query) {
            string += word + " ";
        }

        log.info(toString(), "Query recebida: '" + query + "'");

        return this + ": " + string;
    }


    /**
     * Tenta criar o registo RMI
     * <p>
     * Caso o registo já exista regista o registo falha e é retornado false
     * 
     * @param port     o porto do registo
     * @param endpoint o endpoint em que a instância de {@code Barrel} vai ser
     *                 registada
     * @param barrel   o {@code Barrel} que se quer ligar
     * @return true caso seja possível registar; false caso contrário
     */
    public boolean register() {
        Registry registry;

        // tentar criar o registo
        try {
            registry = LocateRegistry.createRegistry(this.rmiPort);
            log.info(toString(), "Registo criado em 'localhost:" + this.rmiPort);
        } catch (RemoteException re) {
            
            // caso não seja possível criar uma referência para o registo tentar localiza-lo
            log.error(toString(), "Nao foi possivel criar o registo em 'localhost:" + this.rmiPort + "/" + this.rmiEndpoint + "'");
            return false;
        }

        // tentar registar o Barrel no endpoint atribuido
        try {
            registry.bind(this.rmiEndpoint, this);
            log.info(toString(), "Barrel registado em 'localhost:" + this.rmiPort + "/" + this.rmiEndpoint + "'");

        } catch (AlreadyBoundException e) {
            log.error(toString(), "'localhost:" + this.rmiPort + "/" + this.rmiEndpoint + "' ja foi atribuido!");

        } catch (RemoteException e) {
            log.error(toString(), "Ocorreu um erro a registar o Barrel em 'localhost:" + this.rmiPort + "/" + this.rmiEndpoint + "'");
            return false;
        }

        return true;
    }

    /**
     * Tenta remover o objeto do RMI runtime
     * @return true caso consiga; false caso contrario
     */
    private boolean unexport(){

        try {
            return UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException e){
            System.out.println("Erro: Ocorreu um erro ao remover o objeto do RMI runtime!");
        }
        return false;

    }

    /**
     * Imprime no {@code stdin} o modo de uso do programa
     */
    private static void printUsage() {
        System.out.println("Modo de uso:\nBarrel {rmi_port} {rmi_endpoint}\n- rmi_port: Porta onde o registo RMI vai ser criado ou encontrado\n- rmi_endpoint: Endpoint do barrel a ser registado");
    }

    @Override
    public String toString() {
        return "Barrel@localhost:" + this.rmiPort + "/" + this.rmiEndpoint;
    }

    public static void main(String[] args) {

        // tratamento de erros nos parâmetros
        if (args.length == 0) {
            printUsage();
            return;
        }
        if (args.length > 2) {
            printUsage();
            return;
        }

        int rmiPort = -1;
        String rmiEndpoint;

        if (args.length == 2) {
            try {
                rmiPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                printUsage();
                return;
            }
            rmiEndpoint = args[1];
        } else {
            rmiEndpoint = args[0];
        }

        // política e segurança
        System.getProperties().put("java.security.policy", "policy.all");
        System.setSecurityManager(new SecurityManager());

        // Instanciar um barrel
        Barrel barrel;

        try {
            barrel = new Barrel(rmiPort, rmiEndpoint);
        } catch (RemoteException e) {
            System.out.println("Erro: Ocorreu um erro ao criar o Barrel!");
            return;
        }

        // tentar registar o barrel
        if (!barrel.register()) {
            barrel.unexport();
            return;
        }

    }

}
