package searchEngine.barrel;

import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.NetworkInterface;
import java.net.InetSocketAddress;
import java.io.File;

import searchEngine.utils.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;


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
    private File databaseFile;

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
        
        this.running = true;

        this.multicastThread = new Thread(this);
        this.multicastThread.start();
        this.databaseFile = new File("../DataBase/" + this.rmiPort + "_" + this.rmiEndpoint + ".db");


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

        this.log.info(toString(), "A escutar por envio de indices...");

        // receber as mensagens por multicast
        while (this.running) {
            receiveMessage();
        }

    }

    public CopyOnWriteArrayList<String> execURL(String url) throws RemoteException{
        return this.searchdataBase(url, null);
    }

    @Override
    public CopyOnWriteArrayList<String> execQuery(CopyOnWriteArrayList<String> query) throws RemoteException {
        // String string = "";

        // for (String word : query) {
        //     string += word + " ";
        // }

        // log.info(toString(), "Query recebida: '" + query + "'");

        return this.searchdataBase(null, query);
    }


    public boolean dataBaseInitialize(){

        Connection conn = null;
        Statement stmt = null;

        try {

            Class.forName("org.sqlite.JDBC");

            
            if (this.databaseFile.exists()){
                conn = DriverManager.getConnection("jdbc:sqlite:" + this.databaseFile.getAbsolutePath());
                System.out.println("Conexão estabelecida com sucesso!");
                stmt = conn.createStatement();
            }
            else {
                File folder = new File("../DataBase");
                folder.mkdir();
                databaseFile.createNewFile();
                conn = DriverManager.getConnection("jdbc:sqlite:" + this.databaseFile.getAbsolutePath());
                System.out.println("Conexão estabelecida com sucesso!");

                // Criar a tabela
                stmt = conn.createStatement();
                String sql = "CREATE TABLE palavras (" + 
                                    "palavra TEXT," +
                                    "numpesquisas INTEGER," +
                                    "PRIMARY KEY(palavra)" +
                                ");" +
                                
                                "CREATE TABLE link (" +
                                    "url TEXT," +
                                    "titulo TEXT," +
                                   " texto TEXT," +
                                   " msgid INTEGER," +
                                    "numpesquisas INTEGER," +
                                    "PRIMARY KEY(url)" +
                                ");" +

                                "CREATE TABLE referencias (" +
                                    "url_referencia TEXT," +
                                    "PRIMARY KEY(url_referencia)" +
                                ");" +

                                "CREATE TABLE link_referencias (" +
                                   " link_url TEXT," +
                                   " referencias_url_referencia TEXT," +
                                   " PRIMARY KEY(link_url,referencias_url_referencia)" +
                                ");" +
                                
                                "CREATE TABLE palavras_link (" +
                                    "palavras_palavra TEXT," +
                                    "link_url TEXT," +
                                    "PRIMARY KEY(palavras_palavra,link_url)" +
                                ");";

                stmt.executeUpdate(sql);
                System.out.println("Criada as tabelas com sucesso.");
            }

            conn.close();
            stmt.close();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Fechar a conexão com o banco de dados
                conn.close();
                stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }


    public CopyOnWriteArrayList<String> searchdataBase(String url, CopyOnWriteArrayList<String> palavras){

        Connection conn = null;
        Statement stmt = null;
        String retornar = "";

        CopyOnWriteArrayList<String> busca = new CopyOnWriteArrayList<>();

        // Faz a conexao na base de dados e insere o que for pedido
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + this.databaseFile.getAbsolutePath());
            stmt = conn.createStatement();

            if (url != null){
                // Procura na base de dados
                String sql = "SELECT * FROM link_referencias WHERE link_url = '" + url + "'";
                ResultSet rs = stmt.executeQuery(sql);

                while (rs.next()){
                    busca.add(rs.getString("referencias_url_referencia"));
                }
                
                sql = "UPDATE link SET numpesquisas = numpesquisas + 1 WHERE url = '" + url + "'";
                stmt.executeUpdate(sql);
            }

            else if (palavras != null){

                ArrayList<String> urlsEncontrados = new ArrayList<>();

                for (String word : palavras) {
                    // Insere na base de dados
                    String sql = "SELECT * FROM palavras_link WHERE palavras_palavra = '" + word + "'";
                    ResultSet rs = stmt.executeQuery(sql);
    
                    while (rs.next()){
                        urlsEncontrados.add(rs.getString("link_url"));
                    }
                    
                    sql = "UPDATE palavras SET numpesquisas = numpesquisas + 1 WHERE palavra = '" + word + "'";
                    stmt.executeUpdate(sql);
                }

                Map<String, Long> couterMap = urlsEncontrados.stream().collect(Collectors.groupingBy(e -> e.toString(),Collectors.counting()));

                for (Map.Entry<String, Long> entry : couterMap.entrySet()) {
                    if (entry.getValue() == palavras.size()){
                        String sql = "SELECT * FROM link WHERE url = '" + entry.getKey() + "'";
                        ResultSet rs = stmt.executeQuery(sql);
                        retornar = rs.getString("url") + "|" + rs.getString("titulo") + "|" + rs.getString("texto");
                        busca.add(retornar);
                    }
                }
            }



        } catch (SQLException e1){
            e1.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Fechar a conexão com o banco de dados
                conn.close();
                stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return busca;

    }

    public boolean insertDataBase(int msgId, String url, ArrayList<String> palavras, ArrayList<String> referencias){

        Connection conn = null;
        Statement stmt = null;
        boolean check = true;

        // Faz a conexao na base de dados e insere o que for pedido
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + this.databaseFile.getAbsolutePath());
            stmt = conn.createStatement();

            // Insere na base de dados
            String sql = "SELECT * FROM link WHERE url = '" + url + "'";
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()){
                check = false;
            }
            else {
                String sqlINSERT = "INSERT INTO link(url, titulo, texto, msgid, numpesquisas) VALUES('" + url + "', '', ''," + msgId + ", 0)";
                stmt.executeUpdate(sqlINSERT);
                System.out.println("Inserção nos links");
            }

            for (String word : palavras) {

                // Insere na base de dados
                sql = "SELECT * FROM palavras WHERE palavra = '" + word + "'";
                rs = stmt.executeQuery(sql);

                if (rs.next()){
                    check = false;
                }
                else {
                    String sqlINSERT = "INSERT INTO palavras(palavra, numpesquisas) VALUES('" + word + "', 0)";
                    stmt.executeUpdate(sqlINSERT);
                    System.out.println("Inserção nas palavras");
                }

                // Insere na base de dados
                sql = "SELECT * FROM palavras_link WHERE palavras_palavra  = '" + word + "' and link_url = '" + url + "'";
                rs = stmt.executeQuery(sql);

                if (rs.next()){
                    check = false;
                }
                else {
                    String sqlINSERT = "INSERT INTO palavras_link(palavras_palavra, link_url) VALUES('" + word + "','" + url + "')";
                    stmt.executeUpdate(sqlINSERT);
                    System.out.println("Inserção nas palavras + links");
                }
            }

            for (String ref: referencias){

                // Insere na base de dados
                sql = "SELECT * FROM referencias WHERE url_referencia = '" + ref + "'";
                rs = stmt.executeQuery(sql);

                if (rs.next()){
                    check = false;
                }
                else {
                    String sqlINSERT = "INSERT INTO referencias(url_referencia) VALUES('" + ref + "')";
                    stmt.executeUpdate(sqlINSERT);
                    System.out.println("Inserção da referencia");
                }

                // Insere na base de dados
                sql = "SELECT * FROM link_referencias WHERE referencias_url_referencia  = '" + ref + "' and link_url = '" + url + "'";
                rs = stmt.executeQuery(sql);

                if (rs.next()){
                    check = false;
                }
                else {
                    String sqlINSERT = "INSERT INTO link_referencias(link_url, referencias_url_referencia) VALUES('" + url + "','" + ref + "')";
                    stmt.executeUpdate(sqlINSERT);
                    System.out.println("Inserção da referencia links");
                }
            }

        } catch (SQLException e1){
            e1.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            check = false;
        } finally {
            try {
                // Fechar a conexão com o banco de dados
                conn.close();
                stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
                check = false;
            }
        }

        return check;
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

        if(!barrel.dataBaseInitialize()){
            barrel.unexport();
            barrel.closeSocket();
            return;
        }

        ArrayList<String> palavras = new ArrayList<>();
        ArrayList<String> links = new ArrayList<>();
        links.add("link1");
        links.add("link2");
        links.add("link3");

        palavras.add("ola");
        palavras.add("adeus");
        barrel.insertDataBase(1,"url", palavras, links);

        palavras.clear();
        palavras.add("adeus");
        barrel.insertDataBase(1,"url2", palavras, links);

        System.out.println("PASSOU");

    }
}
