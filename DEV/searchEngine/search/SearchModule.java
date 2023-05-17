package searchEngine.search;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import java.io.File;

import searchEngine.barrel.QueryIf;
import searchEngine.fileWorker.TextFileWorker;
import searchEngine.utils.Log;
import searchEngine.URLs.UrlQueueInterface;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SearchModule extends UnicastRemoteObject implements SearchResponse{

    /**
     * Um {@code SearchModule} recebe pedidos RMI de um cliente e realiza
     * pedidos a {@code Barrels} com endpoints fornecidos num ficheiro de configuração
     */
    private ArrayList<String> barrel_hosts;
    private ArrayList<Integer> barrel_ports;
    private ArrayList<String> barrel_endpoints;
    private ArrayList<Integer> ativos;
    private int rmiPort;
    private String rmiEndpoint;
    private String rmiHostQueue;
    private int rmiPortQueue;
    private String rmiEndpointQueue;
    private int barrelIndex;

    private Log log;

    private File fileDataBase;

    
    /**
     * Construtor por omissão da classe SearchModule
     * @throws RemoteException caso ocorra um erro de RMI
     */
    public SearchModule() throws RemoteException{

        // guardar os endpoints dos barrels
        this.barrel_hosts = new ArrayList<String>();
        this.barrel_ports = new ArrayList<Integer>();
        this.barrel_endpoints = new ArrayList<String>();
        this.ativos = new ArrayList<Integer>();
        this.barrelIndex = 0;

        this.log = new Log();

        this.fileDataBase = new File("../DataBase/users.db");
    }
    

    /**
     * Carrega a configuração a partir de um ficheiro
     * fornecido em {@code path}
     * @param path o caminho do ficheiro
     * @return true caso a configuração seja carregada; false caso contrário
     */
    public boolean loadConfig(String path){
        TextFileWorker fileWorker = new TextFileWorker(path);
        ArrayList<String> lines = fileWorker.read();

        // ler a porta RMI e endpoint do SearchModule
        try {
            this.rmiPort = Integer.parseInt(lines.get(0).split("/")[0]);
            this.rmiEndpoint = lines.get(0).split("/")[1];
        } catch (NumberFormatException e){
            log.error(toString(), "Ocorreu um erro ao ler o ficheiro de configuracao em '" + path + "'!\n Nao foi possivel carregar a porta do SearchModule!");
            return false;
        } catch (IndexOutOfBoundsException e){
            log.error(toString(), "Ocorreu um erro ao ler o ficheiro de configuracao em '" + path + "'! Ficheiro esta vazio!");
            return false;
        }

        // ler porta RMI e endpoint da fila de urls
        try {
            this.rmiHostQueue = lines.get(1).split("/")[0];
            this.rmiPortQueue = Integer.parseInt(lines.get(1).split("/")[1]);
            this.rmiEndpointQueue = lines.get(1).split("/")[2];;
        } catch (NumberFormatException e){
            log.error(toString(), "Ocorreu um erro ao ler o ficheiro de configuracao em '" + path + "'!\n Nao foi possivel carregar a porta da Fila!");
            return false;
        } catch (IndexOutOfBoundsException e){
            log.error(toString(), "Ocorreu um erro ao ler o ficheiro de configuracao em '" + path + "'! Ficheiro esta vazio!");
            return false;
        }

        // ler porta RMI e endpoint dos Barrels
        try{

            // adiciona os portos e os endpoints às suas respetivas listas
            for (int i = 2; i < lines.size(); i++) {
                this.barrel_hosts.add(lines.get(i).split("/")[0]);
                this.barrel_ports.add(Integer.parseInt(lines.get(i).split("/")[1]));
                this.barrel_endpoints.add(lines.get(i).split("/")[2]);
                this.ativos.add(1);
            }

        } catch (NumberFormatException e){
            log.error(toString(), "Ocorreu um erro ao ler o ficheiro de configuracao em '" + path + "'! Porta invalida!");
            return false;
        } catch (IndexOutOfBoundsException e){
            log.error(toString(), "Ocorreu um erro ao ler o ficheiro de configuracao em '" + path + "'! Um URL foi mal especificado!");
            return false;
        }

        if (this.barrel_ports.size() == 0){
            log.error(toString(), "Configuracao deve especificar a porta do registo RMI do SearchModule, o endpoint do SearchModule no sesu próprio registo, a porta do registo RMI dos Barrels e os endpoints de cada Barrel um por linha");
            return false;
        }

        
        log.info(toString(), "Configuracao carregada!");
        return true;
    }

    public CopyOnWriteArrayList<String> pagination(CopyOnWriteArrayList<String> response, int page) throws RemoteException{
        int indiceInicial = page * 10 - 10;
        if (indiceInicial > response.size()){
            return null;
        }
        int indiceFinal = 10 * page;
        if (indiceFinal > response.size()){
            indiceFinal = response.size();
        }
        
        List<String> lista = response.subList(indiceInicial, indiceFinal);
        CopyOnWriteArrayList<String> returnList = new CopyOnWriteArrayList<>(lista);

        return returnList;
    }


    public CopyOnWriteArrayList<String> execSearch(String name, CopyOnWriteArrayList<String> query) throws RemoteException{
        log.info(toString(), "Recebida query de " + name);

        CopyOnWriteArrayList<String> response_par = new CopyOnWriteArrayList<>(), response_impar = new CopyOnWriteArrayList<>();
        
        boolean par = false, impar = false;

        Connection conn = null;
        Statement stmt = null;
        String retornar = "";

        // tentar com todos os barrels
        int count = 0;
        while (true){

            if (this.barrelIndex == this.barrel_ports.size()) this.barrelIndex = 0;
            count ++;

            try {
                // ligar ao server registado no rmiEndpoint fornecido
                QueryIf barrel = (QueryIf) LocateRegistry.getRegistry(this.barrel_hosts.get(this.barrelIndex), this.barrel_ports.get(this.barrelIndex)).lookup(this.barrel_endpoints.get(this.barrelIndex));          
                this.ativos.set(this.barrelIndex, 1);      
                
                if (this.barrel_ports.get(this.barrelIndex) % 2 == 0) {
                    par = true;
                    response_par = barrel.execQuery(query);
                } else {
                    impar = true;
                    response_impar = barrel.execQuery(query);    
                }

                
                if (this.ativos.get(this.barrelIndex) == 0) this.ativos.set(this.barrelIndex, 1);
                this.barrelIndex ++;

                // retornar a resposta para o cliente
                if ((par && impar) || (count >= this.barrel_ports.size())) {
                    CopyOnWriteArrayList<String> response = new CopyOnWriteArrayList<>();
                    response.addAll(response_par);
                    response.addAll(response_impar);


                    try {

                        conn = DriverManager.getConnection("jdbc:sqlite:" + barrel.getDataBaseFile());
                        stmt = conn.createStatement();

                        Map<String, Long> couterMap = response.stream().collect(Collectors.groupingBy(e -> e.toString(),Collectors.counting()));
                        
                        // limpar a resposta
                        response.clear();
                        List<String> resultList = new ArrayList<>();
                        
                        for (Map.Entry<String, Long> entry : couterMap.entrySet()) {
                            if (entry.getValue() == query.size()){                                
                                String sql =    "SELECT count(*) as 'contagem', url, titulo, texto" +
                                                " FROM link" +
                                                " LEFT JOIN link_referencias ON link.url = link_referencias.link_url" +
                                                " WHERE link.url = '" + entry.getKey() + "'" +
                                                " ORDER BY contagem";
                                ResultSet rs = stmt.executeQuery(sql);

                                while (rs.next()) {
                                    retornar = "Url: " + rs.getString("url") + " | Titulo: " + rs.getString("titulo") + " | Texto: " + rs.getString("texto")  + " | Contagem: "  + rs.getInt("contagem");
                                    resultList.add(retornar);
                                }
                            }
                        }

                        // Ordenar a lista manualmente em ordem crescente
                        Collections.sort(resultList, new Comparator<String>() {
                            @Override
                            public int compare(String o1, String o2) {
                                int contagem1 = Integer.parseInt(o1.substring(o1.lastIndexOf("Contagem: ") + 10).trim());
                                int contagem2 = Integer.parseInt(o2.substring(o2.lastIndexOf("Contagem: ") + 10).trim());
                                return Integer.compare(contagem2, contagem1);
                            }
                        });

                        // Adicionar os resultados à lista final na ordem correta
                        response.addAll(resultList);

                    } catch (SQLException e1){
                        e1.printStackTrace();
                    } finally {
                        try {
                            // Fechar a conexão com o banco de dados
                            conn.close();
                            stmt.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    
                    return response;
                }
                
            } catch (NotBoundException e) {
                log.error(toString(), "Nao existe um servidor registado no endpoint '" + this.barrel_endpoints.get(this.barrelIndex) + "'!");
                this.ativos.set(this.barrelIndex, 0);
                this.barrelIndex += 1;
                continue;
    
            } catch (AccessException e) {
                log.error(toString(), "Esta máquina nao tem permissões para ligar ao endpoint '" + this.barrel_endpoints.get(this.barrelIndex) + "'!");
                this.ativos.set(this.barrelIndex, 0);
                this.barrelIndex += 1;
                continue;

            } catch (RemoteException e) {
                log.error(toString(), this.barrel_ports.get(this.barrelIndex) + "/" + this.barrel_endpoints.get(this.barrelIndex) + " nao esta disponivel.");
                this.ativos.set(this.barrelIndex, 0);
                this.barrelIndex += 1;
                continue;
            }

        }

        return null;
    }

    public String admin() throws RemoteException{

        CopyOnWriteArrayList<String> downloaders = new CopyOnWriteArrayList<>();

        try {
            // ligar ao server da fila de urls registado no rmiEndpoint fornecido
            UrlQueueInterface urlqueue = (UrlQueueInterface) LocateRegistry.getRegistry(this.rmiHostQueue, this.rmiPortQueue).lookup(this.rmiEndpointQueue);
            downloaders = urlqueue.getDownloaders();
        } catch (NotBoundException e) {
            System.out.println("Erro: não existe um servidor registado no endpoint '" + this.rmiEndpointQueue + "'!");
        } catch (AccessException e) {
            System.out.println("Erro: Esta máquina não tem permissões para ligar ao endpoint '" + this.rmiEndpointQueue + "'!");
        } catch (RemoteException e) {
            System.out.println("Erro: Não foi possível encontrar o registo");
        }

        String barrelsAtivos = "";
        String downloadersAtivos = "";
        for (int i = 0; i < this.barrel_ports.size(); i++) {
            if(this.ativos.get(i) == 1) {
                barrelsAtivos += "IP:" + this.barrel_hosts.get(i) + ":" + this.barrel_ports.get(i) + "/" + this.barrel_endpoints.get(i) + "\n";
            }
        }
        for (String string : downloaders) {
            downloadersAtivos += string + "\n";
        }

        // tentar com todos os barrels
        Boolean par = false, impar = false;
        Connection conn = null;
        Statement stmt = null;
        ArrayList<String> retornar = new ArrayList<>();
        int count = 0;
        String pesquisas = "PESQUISAS:\n";
        while (true){
            count ++;
            if (this.barrelIndex == this.barrel_ports.size()) this.barrelIndex = 0;

            try {

                // ligar ao server registado no rmiEndpoint fornecido
                QueryIf barrel = (QueryIf) LocateRegistry.getRegistry(this.barrel_ports.get(this.barrelIndex)).lookup(this.barrel_endpoints.get(this.barrelIndex));
                
                if (this.barrel_ports.get(this.barrelIndex) % 2 == 0) {
                    par = true;
                    try {

                        conn = DriverManager.getConnection("jdbc:sqlite:" + barrel.getDataBaseFile());
                        stmt = conn.createStatement();
                    
                        String sql =    "SELECT url, numPesquisas" +
                                        " FROM link";
    
                        String sql2 =    "SELECT palavra, numPesquisas" +
                                         " FROM palavras";
    
                        ResultSet rs = stmt.executeQuery(sql);
                        String str;  
                        while (rs.next()) {
                            str = rs.getString("url") + " |" + rs.getString("numPesquisas");
                            retornar.add(str);
                        }
    
                        rs = stmt.executeQuery(sql2);
                        while (rs.next()) {
                            str = rs.getString("palavra") + " |" + rs.getString("numPesquisas");
                            retornar.add(str);
                        }
                    
                    } catch (SQLException e1){
                        e1.printStackTrace();
                    } finally {
                        try {
                            // Fechar a conexão com o banco de dados
                            conn.close();
                            stmt.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                } else {
                    impar = true;
                    try {

                        conn = DriverManager.getConnection("jdbc:sqlite:" + barrel.getDataBaseFile());
                        stmt = conn.createStatement();
                    
                        String sql =    "SELECT url, numPesquisas" +
                                        " FROM link";
    
                        String sql2 =    "SELECT palavra, numPesquisas" +
                                         " FROM palavras";
    
                        ResultSet rs = stmt.executeQuery(sql);
                        String str;  
                        while (rs.next()) {
                            str = rs.getString("url") + " |" + rs.getString("numPesquisas");
                            retornar.add(str);
                        }
    
                        rs = stmt.executeQuery(sql2);
                        while (rs.next()) {
                            str = rs.getString("palavra") + " |" + rs.getString("numPesquisas");
                            retornar.add(str);
                        }
                    
                    } catch (SQLException e1){
                        e1.printStackTrace();
                    } finally {
                        try {
                            // Fechar a conexão com o banco de dados
                            conn.close();
                            stmt.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                }

                
                if (this.ativos.get(this.barrelIndex) == 0) this.ativos.set(this.barrelIndex, 1);
                this.barrelIndex ++;

                // retornar a resposta para o cliente
                if ((par && impar) || (count >= this.barrel_ports.size())) {

                    Collections.sort(retornar, new Comparator<String>() {
                        @Override
                        public int compare(String o1, String o2) {
                            int contagem1 = Integer.parseInt(o1.substring(o1.lastIndexOf("|") + 1).trim());
                            int contagem2 = Integer.parseInt(o2.substring(o2.lastIndexOf("|") + 1).trim());
                            return Integer.compare(contagem2, contagem1);
                        }
                    });

                    for (int i = 0; i < 10 && retornar.size() > i; i++) {
                        if(Integer.parseInt(retornar.get(i).substring(retornar.get(i).lastIndexOf("|") + 1).trim()) == 0){
                            break;
                        }
                        pesquisas += retornar.get(i) + "\n";
                    }
                    break;
                }

                
                
            } catch (NotBoundException e) {
                log.error(toString(), "Nao existe um servidor registado no endpoint '" + this.barrel_endpoints.get(this.barrelIndex) + "'!");
                this.ativos.set(this.barrelIndex, 0);
                this.barrelIndex += 1;
                continue;
    
            } catch (AccessException e) {
                log.error(toString(), "Esta máquina nao tem permissões para ligar ao endpoint '" + this.barrel_endpoints.get(this.barrelIndex) + "'!");
                this.ativos.set(this.barrelIndex, 0);
                this.barrelIndex += 1;
                continue;

            } catch (RemoteException e) {
                log.error(toString(), this.barrel_ports.get(this.barrelIndex) + "/" + this.barrel_endpoints.get(this.barrelIndex) + " nao esta disponivel.");
                this.ativos.set(this.barrelIndex, 0);
                this.barrelIndex += 1;
                continue;
            }

        }

        return "BARRELS:\n" + barrelsAtivos + "DOWNLOADERS:\n" + downloadersAtivos + pesquisas;
    }

    public CopyOnWriteArrayList<String> searchUrl(String name, String query){
        log.info(toString(), "Recebida query de " + name);

        // tentar com todos os barrels
        while (true){

            if (this.barrelIndex == this.barrel_ports.size()) this.barrelIndex = 0;

            try {

                // ligar ao server registado no rmiEndpoint fornecido
                QueryIf barrel = (QueryIf) LocateRegistry.getRegistry(this.barrel_ports.get(this.barrelIndex)).lookup(this.barrel_endpoints.get(this.barrelIndex));
                
                CopyOnWriteArrayList<String> response = barrel.execURL(query);   

                this.ativos.set(this.barrelIndex, 1);

                if (this.ativos.get(this.barrelIndex) == 0) this.ativos.set(this.barrelIndex, 1);
                this.barrelIndex ++;

                // retornar a resposta para o cliente
                return response;
                
            } catch (NotBoundException e) {
                log.error(toString(), "Nao existe um servidor registado no endpoint '" + this.barrel_endpoints.get(this.barrelIndex) + "'!");
                this.ativos.set(this.barrelIndex, 0);
                this.barrelIndex += 1;
                continue;
    
            } catch (AccessException e) {
                log.error(toString(), "Esta máquina nao tem permissões para ligar ao endpoint '" + this.barrel_endpoints.get(this.barrelIndex) + "'!");
                this.ativos.set(this.barrelIndex, 0);
                this.barrelIndex += 1;
                continue;

            } catch (RemoteException e) {
                log.error(toString(), this.barrel_ports.get(this.barrelIndex) + "/" + this.barrel_endpoints.get(this.barrelIndex) + " nao esta disponivel.");
                this.ativos.set(this.barrelIndex, 0);
                this.barrelIndex += 1;
                continue;
            }
        }
    }

    public boolean execURL(String url) throws RemoteException{
        
        try {
            // ligar ao server da fila de urls registado no rmiEndpoint fornecido
            UrlQueueInterface urlqueue = (UrlQueueInterface) LocateRegistry.getRegistry(this.rmiPortQueue).lookup(this.rmiEndpointQueue);
            urlqueue.add(url);
        } catch (NotBoundException e) {
            System.out.println("Erro: não existe um servidor registado no endpoint '" + this.rmiEndpointQueue + "'!");
            return false;
        } catch (AccessException e) {
            System.out.println("Erro: Esta máquina não tem permissões para ligar ao endpoint '" + this.rmiEndpointQueue + "'!");
            return false;
        } catch (RemoteException e) {
            System.out.println("Erro: Não foi possível encontrar o registo");
            return false;
        }
        
        return true;
    }

    
    /**
     * Tenta criar o registo RMI próprio do {@code SearchModule}
     * 
     * @param host     o endereço IP do registo
     * @param port     o porto do registo
     * @param endpoint o endpoint em que a instância de {@code SearchModule} vai ser registada
     * @param barrel   o {@code SearchModule} que se quer ligar
     */
    public boolean register(String host, int port, String endpoint) {
        Registry registry;

        // tentar criar o registo
        try {
            System.setProperty("java.rmi.server.hostname", InetAddress.getLocalHost().getHostAddress());
            registry = LocateRegistry.createRegistry(port);
            log.info(toString(), "Registo criado em 'localhost:" + port);
        } catch (RemoteException re) { // caso nao consiga criar sai com erro
            
            log.error(toString(), "Nao foi possivel criar o registo em 'localhost:" + port + "/" + endpoint + "'");
            return false;
        }

        // tentar registar o SearchModule no endpoint atribuido
        try {
            registry.bind(endpoint, this);
            log.info(toString(), "SearchModule registado em 'localhost:" + port + "/" + endpoint + "'");

        } catch (AlreadyBoundException e) {
            log.error(toString(), "'localhost:" + port + "/" + endpoint + "' ja foi atribuido!");

        } catch (RemoteException e) {
            log.error(toString(), "Ocorreu um erro a registar o SearchModule em 'localhost:" + port + "/" + endpoint + "'");
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
            log.error(toString(), "Ocorreu um erro ao remover o objeto do RMI runtime!");
        }
        return false;

    }

    /**
     * Verifica na base de dados se um usuario existe.
     * 
     * @param nome username
     * @param password password
     * @return true ou false caso exista ou não
     */
    public boolean checkDataBase(String nome, String password){
        Connection conn = null;
        Statement stmt = null;
        boolean check = true;

        // Faz a conexao na base de dados e verifica se existe o que foi pedido
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + this.fileDataBase.getAbsolutePath());
            stmt = conn.createStatement();

            // Insere na base de dados
            String sql = "SELECT * FROM users WHERE nome = '" + nome + "' and password = '" + password + "'";
            ResultSet rs = stmt.executeQuery(sql);

            if (!rs.next()){
                check = false;
            }
            
            while (rs.next()) {
                String name = rs.getString("nome");
                String email = rs.getString("password");
                System.out.println(name + " - " + email);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                // Fechar a conexão com o banco de dados
                conn.close();
                stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return check;
    }

    /**
     * Insere na base de dados um novo usuario.
     * 
     * @param nome username
     * @param password password
     * @return true ou false caso seja bem inserido ou não
     */
    public boolean insertDataBase(String nome, String password){

        Connection conn = null;
        Statement stmt = null;
        boolean check = true;

        // Faz a conexao na base de dados e insere o que for pedido
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + this.fileDataBase.getAbsolutePath());
            stmt = conn.createStatement();

            // Insere na base de dados
            String sql = "SELECT * FROM users WHERE nome = '" + nome + "'";
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()){
                check = false;
            }
            else {
                String sqlINSERT = "INSERT INTO users(nome, password) VALUES('" + nome + "', '" + password + "')";
                stmt.executeUpdate(sqlINSERT);
            }

        } catch (SQLException e1){
            System.out.println("Erro na inserção.");
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
     * Cria a base de dados se necessário. Caso contrário apenas testa a ligação.
     * 
     * @return true ou false consoante a base de dados está operacional ou não
     */
    public boolean dataBaseInitialize(){

        Connection conn = null;
        Statement stmt = null;

        try {

            File dataBaseFile = new File(this.fileDataBase.getAbsolutePath());
            Class.forName("org.sqlite.JDBC");

            
            if (dataBaseFile.exists()){
                conn = DriverManager.getConnection("jdbc:sqlite:" + this.fileDataBase.getAbsolutePath());
                log.info(toString(), "Conexão estabelecida com sucesso!");
                stmt = conn.createStatement();
            }
            else {
                File folder = new File("../DataBase");
                folder.mkdir();
                dataBaseFile.createNewFile();
                conn = DriverManager.getConnection("jdbc:sqlite:" + this.fileDataBase.getAbsolutePath());
                log.info(toString(), "Conexão estabelecida com sucesso!");

                // Criar a tabela
                stmt = conn.createStatement();
                String sql = "CREATE TABLE users (nome TEXT PRIMARY KEY, password TEXT)";
                stmt.executeUpdate(sql);
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

    /**
     * Imprime no {@code stdin} o modo de uso do programa
     */
    private static void printUsage() {
        System.out.println("Modo de uso:\nSearchModule {path}\n- path: Caminho do ficheiro de configuracao");
    }


    public boolean register(String name, String password) throws RemoteException{
        if (checkDataBase(name, password)){
            return true;
        }
        if (insertDataBase(name, password)){
            return true;
        }
        return false;
    }
     

    public boolean login(String name, String password) throws RemoteException{
        if (checkDataBase(name, password)){
            return true;
        }
        return false;
    }


    @Override
    public String toString() {
        return "SearchModule@localhost:" + this.rmiPort + "/" + this.rmiEndpoint;
    }


    public static void main(String[] args) {

        // tratamento de erros nos parametros
        if (args.length != 1){
            printUsage();
            return;
        }

        
        
        // instanciar um SearchModule
        SearchModule searchModule;
        try{
            // instanciar um search module para carregar a config
            searchModule = new SearchModule();
        }
        catch (RemoteException e){
            System.out.println("Erro: Ocorreu um erro ao criar o SearchModule");
            e.printStackTrace();
            return;
        }

        // ler o ficheiro de config
        if (!searchModule.loadConfig(args[0])){
            searchModule.unexport();
            return;
        }
        
        // tentar registar o SearchModule no seu próprio RMI register
        if (!searchModule.register(searchModule.rmiPort, searchModule.rmiEndpoint)){
            searchModule.unexport();
            return;
        }

        // tenta conectar na base de dados
        if (!searchModule.dataBaseInitialize()){
            searchModule.unexport();    
            return;
        }
    }
}
