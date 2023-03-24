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

public class SearchModule extends UnicastRemoteObject implements SearchResponse{

    /**
     * Um {@code SearchModule} recebe pedidos RMI de um cliente e realiza
     * pedidos a {@code Barrels} com endpoints fornecidos num ficheiro de configuração
     */
    private ArrayList<String> barrels;
    private int rmiPort, rmiPortBarrels;
    private String rmiEndpoint;

    
    /**
     * Construtor por omissão da classe SearchModule
     */
    public SearchModule() throws RemoteException{}
    

    /**
     * Carrega a configuração a partir de um ficheiro
     * fornecido em {@code path}
     * @param path o caminho do ficheiro
     * @return true caso a configuração seja carregada; false caso contrário
     */
    public boolean loadConfig(String path){
        TextFileWorker fileWorker = new TextFileWorker(path);
        ArrayList<String> lines = fileWorker.read();

        // ler a porta RMI do SearchModule e depois dos Barrels
        try {
            this.rmiPort = Integer.parseInt(lines.get(0));
            this.rmiPortBarrels = Integer.parseInt(lines.get(2));
        } catch (NumberFormatException e){
            System.out.println("Erro: Ocorreu um erro ao ler o ficheiro de configuracao em '" + path + "'!\n Nao foi possivel carregar uma porta!");
            return false;
        } catch (IndexOutOfBoundsException e){
            System.out.println("Erro: Ocorreu um erro ao ler o ficheiro de configuracao em '" + path + "'! Nao existem linhas suficientes!");
            return false;
        }

        // guardar o endpoint do SearchModule
        this.rmiEndpoint = lines.get(1);

        // guardar os endpoints dos barrels
        this.barrels = new ArrayList<String>(lines.subList(3, lines.size()));

        if (this.barrels.size() == 0){
            System.out.println("Erro: Configuracao deve especificar a porta do registo RMI do SearchModule, o endpoint do SearchModule no sesu próprio registo, a porta do registo RMI dos Barrels e os endpoints de cada Barrel um por linha");
            return false;
        }

        return true;
        
    }



    public String execSearch(CopyOnWriteArrayList<String> query) throws RemoteException{

        int barrelIndex = 0;

        // tentar com todos os barrels
        while (barrelIndex < this.barrels.size()){


            try {
    
                // ligar ao server registado no rmiEndpoint fornecido
                QueryIf barrel = (QueryIf) LocateRegistry.getRegistry(this.rmiPortBarrels).lookup(this.barrels.get(barrelIndex));

                // retornar a resposta para o cliente
                return barrel.execQuery(query);   
                
            } catch (NotBoundException e) {
                System.out.println("Erro: não existe um servidor registado no endpoint '" + this.barrels.get(barrelIndex) + "'!");

                barrelIndex += 1;
                continue;
    
            } catch (AccessException e) {

                System.out.println("Erro: Esta máquina não tem permissões para ligar ao endpoint '" + this.barrels.get(barrelIndex) + "'!");
                barrelIndex += 1;
                continue;

            } catch (RemoteException e) {

                System.out.println("Erro: " + this.barrels.get(barrelIndex) + " nao esta disponivel.");
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
    public static boolean register(int port, String endpoint, SearchModule searchModule) {
        Registry registry;

        // tentar criar o registo
        try {
            registry = LocateRegistry.createRegistry(port);
            System.out.println("Registo criado em 'localhost:" + port);
        } catch (RemoteException re) { // caso nao consiga criar sai com erro
            
            System.out.println("Erro: Nao foi possivel criar o registo em 'localhost:" + port + "/" + endpoint + "'");
            return false;
        }

        // tentar registar o Barrel no endpoint atribuido
        try {
            registry.bind(endpoint, searchModule);
            System.out.println("SearchModule registado em 'localhost:" + port + "/" + endpoint + "'");

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
        System.out.println("Modo de uso:\nSearchModule {path}\n- path: Caminho do ficheiro de configuracao");
    }


    /**
     * Tenta remover o objeto do RMI runtime
     * @return true caso consiga; false caso contrario
     */
    public boolean unexport(){

        try {
            return UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException e){
            System.out.println("Erro: Ocorreu um erro ao remover o objeto do RMI runtime!");
        }
        return false;

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
        if (!register(searchModule.rmiPort, searchModule.rmiEndpoint, searchModule)){
            searchModule.unexport();
            return;
        }

    }
}
