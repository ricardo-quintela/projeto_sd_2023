package searchEngine.search;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;
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



    public boolean execSearch(ArrayList<String> query){

        int barrelIndex = 0;

        // tentar com todos os barrels
        while (barrelIndex < this.barrels.size()){


            try {
    
                // ligar ao server registado no rmiEndpoint fornecido
                QueryIf barrel = (QueryIf) LocateRegistry.getRegistry(this.rmiPortBarrels).lookup(this.barrels.get(barrelIndex));

                // printar a query que o barrel recebeu
                System.out.println(barrel.execQuery(query));

                return true;     
                
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

        return false;

    }




    public void menu(Scanner sc){

        int loop = 1;
        while (loop == 1){

            System.out.println("Googol\nDigite a opcao desejada:\n1 - Indexar um URL\n2 - Pesquisar\n3 - sair\nDigite: ");

            try{
                
                int num = sc.nextInt();

                switch(num){
                    case 1:
                        System.out.println("Indexar um URL foi selecionado\n");
                        break;
                    case 2:
                        System.out.println("Pesquisar por uma query\n");
                        break;
                    case 3:
                        loop = 0;
                        break;

                    default:
                        System.out.println("Escolha invalida.");
                }
            } 
            catch (InputMismatchException e){
                System.out.println("Digite um valor permitido.");
            } catch (NoSuchElementException e){
                System.out.println("Digite um valor permitido.");
            }

        }

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
    
    public void postResponse(String response, String barrel) throws RemoteException{
        System.out.println("OLA ESTOU AQUI!");
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
        
        // instanciar um scanner
        Scanner sc = new Scanner(System.in);
        

        // menu do clente (TEMPORARIO)
        searchModule.menu(sc);

        sc.close();


        searchModule.unexport();

    }
}
