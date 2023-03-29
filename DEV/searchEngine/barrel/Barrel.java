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
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import searchEngine.utils.Log;

public class Barrel extends UnicastRemoteObject implements QueryIf, Runnable {

    private MulticastSocket multicastSocket;
    private String multicastAddress;
    private int multicastPort;
    private int sockTimeout;

    private Thread multicastThread;
    private InetAddress multicastGroup;

    private int rmiPort;
    private String rmiEndpoint;

    private Log log;

    private boolean running;

    /**
     * Construtor da classe {@code Barrel}
     *
     * @param rmiPort          o porto onde o registo RMI vai ser aberto
     * @param rmiEndpoint      o endpoint onde o {@code Barrel} vai ser registado
     * @param multicastAddress o endereço do grupo multicast dos {@code Downloader}s
     * @param multicastPort    o porto multicast dos {@code Downloader}s
     * @param sockTimeout      o tempo de espera do socket para receber resposta em
     *                         ms
     * @throws RemoteException caso ocorra um erro no RMI
     * @throws IOException     caso ocorra um erro a criar o MulticastSocket
     */
    public Barrel(int rmiPort, String rmiEndpoint, String multicastAddress, int multicastPort, int sockTimeout)
            throws RemoteException, IOException {
        this.rmiPort = rmiPort;
        this.rmiEndpoint = rmiEndpoint;
        this.log = new Log();

        this.multicastSocket = new MulticastSocket(multicastPort);
        this.sockTimeout = sockTimeout;
        this.multicastSocket.setSoTimeout(sockTimeout);

        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.multicastThread = new Thread(this);
        this.multicastThread.start();

        this.running = true;

    }

    /**
     * Construtor por omissão da classe {@code Barrel}
     */
    public Barrel() throws RemoteException {
        this.log = new Log();
    }

    /**
     * Define o estado do atributo running
     * 
     * @param state o estado para alterar o atributo running
     */
    public void setRunning(boolean state) {
        this.running = state;
    }

    public boolean receiveMessage() {

        DatagramPacket packet;
        String receivedMessage[], connectionMessage;

        int messageId, buffSize, numUrls;

        // ? 1 - SETUP

        // buffer que recebe mensagens default de conexao
        byte[] connectionBuffer, messageBuffer;

        // ? 2 - RECEBER HEARTBEAT

        connectionBuffer = new byte[50];
        packet = new DatagramPacket(connectionBuffer, connectionBuffer.length);

        // receber uma mensagem do grupo (BLOQUEANTE)
        while (true) {
            try {

                this.log.info(toString(), "A espera de Heartbeat...");
                this.multicastSocket.receive(packet);
                
            }
            // deu timeout por isso cancela-se o procedimento inteiro
            catch (SocketTimeoutException e) {
                this.log.error(toString(), "Heartbeat deu timeout!");
                return false;
                
            } catch (IOException e) {
                this.log.error(toString(), "Ocorreu um erro a receber um heartbeat!");
            }
            
            // separar a mensagem por ";"
            receivedMessage = (new String(packet.getData(), 0, packet.getLength())).split(" *; *");
            // //TODO: DEBUG HEARTBEAT
            // System.out.println("================\n" + (new String(packet.getData(), 0, packet.getLength())).split(" *; *")[0] + "\n===================");
            
            // verificar se a mensagem é uma confirmaçao de heartbeat
            if (receivedMessage.length > 0 && receivedMessage[0].equals("type | heartbeat")) {
                try {
                    messageId = Integer.parseInt(receivedMessage[1].split(" *\\| *")[1]);
                    buffSize = Integer.parseInt(receivedMessage[2].split(" *\\| *")[1]);
                } catch (NumberFormatException e) {
                    this.log.error(toString(), "ID de mensagem invalido - nao e um numero!");
                    return false;
                    
                } catch (IndexOutOfBoundsException e) {
                    this.log.error(toString(), "Formato de Heartbeat invalido!");
                    return false;
                }
                
                this.log.info(toString(), "Heartbeat recebido!");
                break;
            }
        }

        // ? 3 - ALOCAR ESPAÇO E RESPONDER A HEARTBEAT

        // alocar espaço para a mensagem a receber
        messageBuffer = new byte[buffSize];

        // criar a mensagem de resposta ao heartbeat
        connectionMessage = "type | ready; id | " + messageId + ";";
        connectionBuffer = connectionMessage.getBytes();
        packet = new DatagramPacket(connectionBuffer, connectionBuffer.length, this.multicastGroup, this.multicastPort);

        // continuar a enviar até conseguir
        while (true) {

            // enviar resposta a heartbeat
            try {

                this.log.info(toString(), "A responder a Heartbeat...");
                this.multicastSocket.send(packet);
                this.log.info(toString(), "Resposta a Heartbeat enviada!");

                break;

            } catch (IOException e) {
                this.log.error(toString(),
                        "Ocorreu um erro ao confirmar a conexao para '" + this.multicastAddress + "'!");
            } catch (SecurityException e) {
                this.log.error(toString(), "Um SecurityManager nao permitiu o envio da resposta de heartbeat para '"
                        + this.multicastAddress + "'!");
                return false;
            }
        }

        // ? 4 - RECEBER A MENSAGEM

        // preparar um novo packet para receber a mensagem
        packet = new DatagramPacket(messageBuffer, messageBuffer.length);

        // receber uma mensagem do grupo (BLOQUEANTE)
        while (true) {
            try {

                this.log.info(toString(), "A espera da mensagem " + messageId + "...");
                this.multicastSocket.receive(packet);

            }
            // deu timeout por isso cancela-se o procedimento inteiro
            catch (SocketTimeoutException e) {
                this.log.error(toString(), "Rececao de mensagem deu timeout!");
                return false;

            } catch (IOException e) {
                this.log.error(toString(), "Ocorreu um erro a receber a mensagem!");
                continue;
            }

            // separar a mensagem por ";"
            receivedMessage = (new String(packet.getData(), 0, packet.getLength())).split(" *; *");

            // //TODO: DEBUG MENSAGEM
            // System.out.println("================\n" + (new String(packet.getData(), 0, packet.getLength())).split(" *; *")[0] + "\n===================");

            // verificar se a mensagem é uma confirmaçao de heartbeat
            if (receivedMessage.length > 0 && receivedMessage[0].equals("type | url_list")
                    && receivedMessage[1].equals("id | " + messageId)) {

                this.log.info(toString(), "Mensagem " + messageId + " recebida!");

                try {

                    numUrls = Integer.parseInt(receivedMessage[2].split(" *\\| *")[1]);

                } catch (NumberFormatException e) {
                    this.log.error(toString(), "Numero de URLs invalido!");
                    return false;

                } catch (IndexOutOfBoundsException e) {
                    this.log.error(toString(), "Formato de mensagem invalido!");
                    return false;
                }

                break;
            }
        }


        // ? 5 - CONFIRMAR RECEBIMENTO DA MENSAGEM

        // criar a mensagem de confirmação de envio
        connectionMessage = "type | rcvd; id | " + messageId + ";";
        connectionBuffer = connectionMessage.getBytes();
        packet = new DatagramPacket(connectionBuffer, connectionBuffer.length, this.multicastGroup, this.multicastPort);

        // continuar a enviar até conseguir
        while (true) {

            // enviar resposta a heartbeat
            try {

                this.log.info(toString(), "A confirmar recebimento da mensagem " + messageId + "...");
                this.multicastSocket.send(packet);
                this.log.info(toString(), "Confirmacao enviada!");

                break;

            } catch (IOException e) {
                this.log.error(toString(),
                        "Ocorreu um erro ao confirmar o envio para '" + this.multicastAddress + "'!");
            } catch (SecurityException e) {
                this.log.error(toString(), "Um SecurityManager nao permitiu o envio da confirmação de envio para '"
                        + this.multicastAddress + "'!");
                return false;
            }
        }


        // TODO: PRINTS DE DEBUG PARA SABER SE RECEBEU A MENSAGEM
        for (int i = 3; i < 3 + numUrls; i++) {
            System.out.println("Mensagem recebida: " + receivedMessage[i]);
        }


        return true;

    }

    public void run() {

        // criar um grupo multicast
        try {
            this.multicastGroup = InetAddress.getByName(this.multicastAddress);
        } catch (UnknownHostException e) {
            this.log.error(toString(), "Nao foi possivel encontrar '" + this.multicastAddress + "'!");
            return;
        } catch (SecurityException e) {
            this.log.error(toString(), "Um SecurityManager nao permitiu a ligacao a '" + this.multicastAddress + "'!");
            return;
        }

        // juntar ao grupo multicast
        try {
            this.multicastSocket.joinGroup(this.multicastGroup);
        } catch (IOException e) {
            e.printStackTrace();
            this.log.error(toString(), "Ocorreu um erro a juntar ao grupo multicast!");
            return;
        }

        // receber as mensagens por multicast
        while (this.running) {
            receiveMessage();
        }

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
                "Modo de uso:\nBarrel {rmi_port} {rmi_endpoint} {multicast_ip} {multicast_port}\n- rmi_port: Porta onde o registo RMI vai ser criado ou encontrado\n- rmi_endpoint: Endpoint do barrel a ser registado\n- multicast_ip: O ip para qual vao ser transmitidas mensagens por multicast\n- multicast_port: O porto para onde as mensagens vao ser enviadas no Host de multicast");
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
        if (args.length != 4) {
            printUsage();
            return;
        }

        // parsing dos argumentos da consola
        int rmiPort;
        int multicastPort;
        try {
            rmiPort = Integer.parseInt(args[0]);
            multicastPort = Integer.parseInt(args[3]);
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
            barrel = new Barrel(rmiPort, args[1], args[2], multicastPort, 5000);

        } catch (RemoteException e) {
            System.out.println("Erro: Ocorreu um erro de RMI ao criar o Barrel!");
            return;
        } catch (IOException e) {
            System.out.println("Erro: Ocorreu um erro no MulticastSocket ao criar o Barrel!");
            return;
        }

        // apanhar sinal SIGINT
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {

                synchronized (this) {
                    System.out.println("RECEBIDO SIGINT");
                    barrel.closeSocket();
                    barrel.setRunning(false);
                }
            }
        });

        // tentar registar o barrel
        if (!barrel.register()) {
            barrel.unexport();
            barrel.closeSocket();
            return;
        }

    }

}
