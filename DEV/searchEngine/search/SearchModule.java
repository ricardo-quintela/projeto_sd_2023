package search;

import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import barrel.QueryIf;
import fileWorker.TextFileWorker;
import utils.TratamentoStrings;

public class SearchModule extends UnicastRemoteObject implements SearchResponse{

    /**
     * Um {@code SearchModule} recebe pedidos RMI de um cliente e realiza
     * pedidos a {@code Barrels} com endpoints fornecidos num ficheiro de configuração
     */
    private ArrayList<String> barrels;
    private int rmiPort;

    
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

        try {
            this.rmiPort = Integer.parseInt(lines.get(0));
        } catch (NumberFormatException e){
            System.out.println("Erro: Ocorreu um erro ao ler o ficheiro de configuracao em '" + path + "'!");
            return false;
        }

        this.barrels = new ArrayList<String>(lines.subList(1, lines.size()));

        if (this.barrels.size() == 0){
            System.out.println("Erro: Configuracao deve especificar a porta do registo RMI e os endpoints de cada storage barrel um por linha");
            return false;
        }

        return true;
        
    }




    public void menu(){

        Scanner sc = new Scanner(System.in);
        int loop = 1;
        while (loop == 1){

            System.out.println("Google da wish\nDigite a opcao desejada:\n1 - url\n2 - palavra\n3 - sair\nDigite: ");

            try{
                
                int num = sc.nextInt();

                switch(num){
                    case 1:
                        System.out.println("URL\n");
                        break;
                    case 2:
                        System.out.println("Palavra\n");
                        break;
                    case 3:
                        loop = 0;
                        break;
                }
            } 
            catch (Exception e){
                System.out.println("Digite um valor permitido");
            }

        }
        sc.close();
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
    public static boolean register(int port, String endpoint, SearchModule searchModule) {
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
        System.out.println("Modo de uso:\nporta do searchModule {port}\nsearch {server_endpoint}\nbarrel {barrel_endpoint}");
    }
    
    public void postResponse(String response, String barrel) throws RemoteException{
        System.out.println("OLA ESTOU AQUI!");
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

        SearchModule searchModule;
        try{
            // instanciar um search module para carregar a config
            searchModule = new SearchModule();
            if (!searchModule.loadConfig(args[0])){
                return;
            }
        }
        catch (Exception e){
            return;
        }
        
        
        // indice do barrel a consultar
        int barrelIndex = 0;

        // instanciar um Scanner para ler comandos da consola
        Scanner sc = new Scanner(System.in);
        String command;
        ArrayList<String> query;
        
        while (true) {
            
            command = sc.nextLine();
            if (command.equals("q") || command.equals("quit")) break;
            
            try {

                // ligar ao server registado no rmiEndpoint fornecido
                QueryIf barrel = (QueryIf) Naming.lookup(TratamentoStrings.urlTratamento(searchModule.rmiPort, searchModule.barrels.get(barrelIndex)));
                
                query = new ArrayList<>(Arrays.asList(command.split(" ")));
                
                System.out.println(barrel.execQuery(query));
    
                // if (!register(port, rmiEndpointSearchModule, sm)){
                //     return;
                // }
    
                // Menu cliente
                searchModule.menu();
                System.out.println("Pressione CTRL+C para fechar.");
                    
                
            } catch (NotBoundException e) {
                System.out.println("Erro: não existe um servidor registado no endpoint '" + searchModule.barrels.get(barrelIndex) + "'!");
                // voltar ao início da fila de barrels
                if (barrelIndex == searchModule.barrels.size() - 1){
                    barrelIndex = 0;
                    continue;
                }
                barrelIndex += 1;
                continue;

            } catch (AccessException e) {
                System.out.println("Erro: Esta máquina não tem permissões para ligar ao endpoint '" + searchModule.barrels.get(barrelIndex) + "'!");
                break;
            } catch (MalformedURLException e) {
                System.out.println("Erro: O endpoint fornecido ('" + searchModule.barrels.get(barrelIndex) + "') não forma um URL válido!");
                break;
            } catch (RemoteException e) {
                System.out.println("Erro: " + searchModule.barrels.get(barrelIndex) + " nao esta disponivel.");
                
                // voltar ao início da fila de barrels
                if (barrelIndex == searchModule.barrels.size() - 1){
                    barrelIndex = 0;
                    continue;
                }
                barrelIndex += 1;
            }


        }
    }
}
