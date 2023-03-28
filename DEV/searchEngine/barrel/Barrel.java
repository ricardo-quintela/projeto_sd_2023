package searchEngine.barrel;

import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CopyOnWriteArrayList;

import java.net.DatagramPacket;

import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.InetSocketAddress;

import searchEngine.utils.Log;

public class Barrel extends UnicastRemoteObject implements QueryIf, Runnable {

    private MulticastSocket multicastSocket;
    private String multicastAddress;
    private int multicastPort;

    private Thread multicastThread;

    private int rmiPort;
    private String rmiEndpoint;

    private Log log;

    /**
     * Construtor por omissão da classe {@code Barrel}
     *
     * @param rmiPort          o porto onde o registo RMI vai ser aberto
     * @param rmiEndpoint      o endpoint onde o {@code Barrel} vai ser registado
     * @param multicastAddress o endereço do grupo multicast dos {@code Downloader}s
     * @param multicastPort    o porto multicast dos {@code Downloader}s
     * @throws RemoteException caso ocorra um erro no RMI
     * @throws IOException     caso ocorra um erro a criar o MulticastSocket
     */
    public Barrel(int rmiPort, String rmiEndpoint, String multicastAddress, int multicastPort)
            throws RemoteException, IOException {
        this.rmiPort = rmiPort;
        this.rmiEndpoint = rmiEndpoint;
        this.log = new Log();

        this.multicastSocket = new MulticastSocket(multicastPort);
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.multicastThread = new Thread(this);
        this.multicastThread.start();

    }

    /**
     * Construtor por omissão da classe {@code Barrel}
     */
    public Barrel() throws RemoteException {
        this.log = new Log();
    }

    public void run() {
        try {

            InetSocketAddress group = new InetSocketAddress(this.multicastAddress, this.multicastPort);
            NetworkInterface netIf = NetworkInterface.getByName("bgc0");

            this.multicastSocket.joinGroup(group, netIf);

            while (true) {

                DatagramPacket message = this.receiveMessage();

                if (message != null) {
                    System.out.println(message.getData());
                }

            }

        } catch (IOException e) {
            log.error(toString(),
                    "Nao foi possivel juntar ao grupo multicast. O endereco fornecido e um endereco multicast?");
            return;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            log.error(toString(), "Um SecurityManager nao permitiu juntar ao grupo multicast!");
            return;
        }
        return;
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
     * Recebe uma mensagem do grupo multicast a que está ligado
     * 
     * @return o pacote recebido
     */
    public DatagramPacket receiveMessage() {

        byte[] buffer = new byte[1024];
        DatagramPacket packet_received = new DatagramPacket(buffer, buffer.length);

        try {
            this.multicastSocket.receive(packet_received);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return packet_received;
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
            log.error(toString(),
                    "Nao foi possivel criar o registo em 'localhost:" + this.rmiPort + "/" + this.rmiEndpoint + "'");
            return false;
        }

        // tentar registar o Barrel no endpoint atribuido
        try {
            registry.bind(this.rmiEndpoint, this);
            log.info(toString(), "Barrel registado em 'localhost:" + this.rmiPort + "/" + this.rmiEndpoint + "'");

        } catch (AlreadyBoundException e) {
            log.error(toString(), "'localhost:" + this.rmiPort + "/" + this.rmiEndpoint + "' ja foi atribuido!");

        } catch (RemoteException e) {
            log.error(toString(),
                    "Ocorreu um erro a registar o Barrel em 'localhost:" + this.rmiPort + "/" + this.rmiEndpoint + "'");
            return false;
        }

        return true;
    }

    /**
     * Tenta remover o objeto do RMI runtime
     * 
     * @return true caso consiga; false caso contrario
     */
    private boolean unexport() {

        try {
            return UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException e) {
            System.out.println("Erro: Ocorreu um erro ao remover o objeto do RMI runtime!");
        }
        return false;

    }

    /**
     * Fetcha a socket multicast
     */
    public void closeSocket() {
        this.multicastSocket.close();
    }

    /**
     * Imprime no {@code stdin} o modo de uso do programa
     */
    private static void printUsage() {
        System.out.println(
                "Modo de uso:\nBarrel {rmi_port} {rmi_endpoint}\n- rmi_port: Porta onde o registo RMI vai ser criado ou encontrado\n- rmi_endpoint: Endpoint do barrel a ser registado");
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
        if (args.length > 4) {
            printUsage();
            return;
        }

        // parsing dos argumentos da consola
        int rmiPort;
        int multicastPort;
        try {
            rmiPort = Integer.parseInt(args[0]);
            multicastPort = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            printUsage();
            return;
        }

        // política e segurança
        System.getProperties().put("java.security.policy", "policy.all");
        System.setSecurityManager(new SecurityManager());

        // Instanciar um barrel -> o barrel vai correr uma thread extra para receber
        // pacotes do grupo multicast
        Barrel barrel;
        try {
            barrel = new Barrel(rmiPort, args[1], args[2], multicastPort);

        } catch (RemoteException e) {
            System.out.println("Erro: Ocorreu um erro de RMI ao criar o Barrel!");
            return;
        } catch (IOException e) {
            System.out.println("Erro: Ocorreu um erro no MulticastSocket ao criar o Barrel!");
            return;
        }

        // tentar registar o barrel
        if (!barrel.register()) {
            barrel.unexport();
            barrel.closeSocket();
            return;
        }

        barrel.closeSocket();

    }

}
