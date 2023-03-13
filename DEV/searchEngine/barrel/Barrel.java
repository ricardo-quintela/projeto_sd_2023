package searchEngine.barrel;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class Barrel extends UnicastRemoteObject implements QueryIf {

    private int rmiPort;
    private String rmiEndpoint;

    /**
     * Construtor por omissão da classe {@code Barrel}
     */
    public Barrel(int rmiPort, String rmiEndpoint) throws RemoteException {
        this.rmiPort = rmiPort;
        this.rmiEndpoint = rmiEndpoint;
    }

    /**
     * Construtor por omissão da classe {@code Barrel}
     */
    public Barrel() throws RemoteException {
    }

    @Override
    public String execQuery(ArrayList<String> query) throws RemoteException {
        String string = "";

        for (String word : query) {
            string += word + " ";
        }

        return this + ": " + string;
    }


    /**
     * Tenta criar o registo RMI
     * <p>
     * Caso o registo já exista regista uma instância de {@code Barrel} no mesmo
     * 
     * @param port     o porto do registo
     * @param endpoint o endpoint em que a instância de {@code Barrel} vai ser
     *                 registada
     * @param barrel   o {@code Barrel} que se quer ligar
     */
    public static boolean register(int port, String endpoint, Barrel barrel) {
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

        // tentar registar o Barrel no endpoint atribuido
        try {
            registry.bind(endpoint, barrel);
            System.out.println("Barrel registado em 'localhost:" + port + "/" + endpoint + "'");

        } catch (AlreadyBoundException e) {
            System.out.println("Erro: 'localhost:" + port + "/" + endpoint + "' ja foi atribuido!");

        } catch (RemoteException e) {
            System.out.println("Erro: Ocorreu um erro a registar o Barrel em 'localhost:" + port + "/" + endpoint + "'");
            return false;
        }

        return true;
    }

    /**
     * Imprime no {@code stdin} o modo de uso do programa
     */
    private static void printUsage() {
        System.out.println("Modo de uso:\nbarrel {rmi_port} {rmi_endpoint}");
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
        if (!register(rmiPort, rmiEndpoint, barrel)) {
            return;
        }

    }

}
