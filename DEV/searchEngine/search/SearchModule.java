package searchEngine.search;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import searchEngine.barrel.QueryIf;
import searchEngine.fileWorker.TextFileWorker;
import searchEngine.utils.Log;
import searchEngine.utils.TratamentoStrings;
import searchEngine.URLs.UrlQueueInterface;

public class SearchModule extends UnicastRemoteObject implements SearchResponse{

    /**
     * Um {@code SearchModule} recebe pedidos RMI de um cliente e realiza
     * pedidos a {@code Barrels} com endpoints fornecidos num ficheiro de configuração
     */
    private ArrayList<Integer> barrel_ports;
    private ArrayList<String> barrel_endpoints;
    private int rmiPort;
    private String rmiEndpoint;
    private int rmiPortQueue;
    private String rmiEndpointQueue;

    private Log log;

    
    /**
     * Construtor por omissão da classe SearchModule
     * @throws RemoteException caso ocorra um erro de RMI
     */
    public SearchModule() throws RemoteException{

        // guardar os endpoints dos barrels
        this.barrel_ports = new ArrayList<Integer>();
        this.barrel_endpoints = new ArrayList<String>();

        this.log = new Log();
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
            this.rmiPortQueue = Integer.parseInt(lines.get(1).split("/")[0]);
            this.rmiEndpointQueue = lines.get(1).split("/")[1];
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
                this.barrel_ports.add(Integer.parseInt(lines.get(i).split("/")[0]));
                this.barrel_endpoints.add(lines.get(i).split("/")[1]);
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



    public String execSearch(String name, CopyOnWriteArrayList<String> query) throws RemoteException{
        
        int barrelIndex = 0;

        log.info(toString(), "Recebida query de " + name);

        // tentar com todos os barrels
        while (barrelIndex < this.barrel_ports.size()){


            try {

    
                // ligar ao server registado no rmiEndpoint fornecido
                QueryIf barrel = (QueryIf) LocateRegistry.getRegistry(this.barrel_ports.get(barrelIndex)).lookup(this.barrel_endpoints.get(barrelIndex));

                // retornar a resposta para o cliente
                return barrel.execQuery(query);   
                
            } catch (NotBoundException e) {
                log.error(toString(), "Nao existe um servidor registado no endpoint '" + this.barrel_endpoints.get(barrelIndex) + "'!");

                barrelIndex += 1;
                continue;
    
            } catch (AccessException e) {

                log.error(toString(), "Esta máquina nao tem permissões para ligar ao endpoint '" + this.barrel_endpoints.get(barrelIndex) + "'!");
                barrelIndex += 1;
                continue;

            } catch (RemoteException e) {

                log.error(toString(), this.barrel_ports.get(barrelIndex) + "/" + this.barrel_endpoints.get(barrelIndex) + " nao esta disponivel.");
                barrelIndex += 1;
                continue;
            }

        }

        return null;

    }

    /**
     * Tenta criar o registo RMI próprio do {@code SearchModule}
     * 
     * @param port     o porto do registo
     * @param endpoint o endpoint em que a instância de {@code SearchModule} vai ser registada
     * @param barrel   o {@code SearchModule} que se quer ligar
     */
    public boolean register(int port, String endpoint) {
        Registry registry;

        // tentar criar o registo
        try {
            registry = LocateRegistry.createRegistry(port);
            log.info(toString(), "Registo criado em 'localhost:" + port);
        } catch (RemoteException re) { // caso nao consiga criar sai com erro
            
            log.error(toString(), "Nao foi possivel criar o registo em 'localhost:" + port + "/" + endpoint + "'");
            return false;
        }

        // tentar registar o Barrel no endpoint atribuido
        try {
            registry.bind(endpoint, this);
            log.info(toString(), "SearchModule registado em 'localhost:" + port + "/" + endpoint + "'");

        } catch (AlreadyBoundException e) {
            log.error(toString(), "'localhost:" + port + "/" + endpoint + "' ja foi atribuido!");

        } catch (RemoteException e) {
            log.error(toString(), "Ocorreu um erro a registar o Barrel em 'localhost:" + port + "/" + endpoint + "'");
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
        System.out.println("Modo de uso:\nSearchModule {path}\n- path: Caminho do ficheiro de configuracao");
    }

    @Override
    public String toString() {
        return "SearchModule@localhost:" + this.rmiPort + "/" + this.rmiEndpoint;
    }


    public static void main(String[] args) {

        // tratamento de erros nos parametros
        if (args.length == 0){
            printUsage();
            return;
        }
        
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


        try{

            // ligar ao server da fila de urls registado no rmiEndpoint fornecido
            UrlQueueInterface urlqueue = (UrlQueueInterface) LocateRegistry.getRegistry(searchModule.rmiPortQueue).lookup(searchModule.rmiEndpointQueue);

            System.out.println(urlqueue.isEmpty());
            urlqueue.add("url1");
            System.out.println(urlqueue.isEmpty());
            System.out.println(urlqueue.get(0));

        } catch (NotBoundException e) {
            System.out.println("Erro: não existe um servidor registado no endpoint '" + searchModule.rmiEndpointQueue + "'!");
            return;
        } catch (AccessException e) {
            System.out.println("Erro: Esta máquina não tem permissões para ligar ao endpoint '" + searchModule.rmiEndpointQueue + "'!");
            return;
        } catch (RemoteException e) {
            System.out.println("Erro: Não foi possível encontrar o registo");
            return;
        }
    }
}
