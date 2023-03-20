package searchEngine.search;

import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Scanner;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import utils.TratamentoStrings;
import searchEngine.barrel.QueryIf;

public class SearchModule extends UnicastRemoteObject implements SearchResponse{


    public SearchModule() throws RemoteException{}


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
        
        if (args.length != 3){
            printUsage();
            return;
        }

        // Conexao do server - porta e endpoint
        // A porta tem de ser igual 
        int port = Integer.parseInt(args[0]);
        String rmiEndpointSearchModule = args[1];

        // Conexao do barrel a que queremos ligar
        String rmiEndpointBarrels = args[2];
        
        try (Scanner sc = new Scanner(System.in)) {

            System.out.println(rmiEndpointBarrels);

            // ligar ao server registado no rmiEndpoint fornecido
            QueryIf barrel = (QueryIf) Naming.lookup(rmiEndpointBarrels);

            ArrayList<String> query = new ArrayList<>();

            query.add("Ola");

            System.out.println(barrel.execQuery(query));

            SearchModule sm = new SearchModule();

            if (!register(port, rmiEndpointSearchModule, sm)){
                return;
            }

            // Menu cliente
            sm.menu();
            System.out.println("Pressione CTRL+C para fechar.");
            return;    

        } catch (NotBoundException e) {
            System.out.println("Erro: não existe um servidor registado no endpoint '" + rmiEndpointBarrels + "'!");
            return;
        } catch (AccessException e) {
            System.out.println("Erro: Esta máquina não tem permissões para ligar ao endpoint '" + rmiEndpointBarrels + "'!");
            return;
        } catch (MalformedURLException e) {
            System.out.println("Erro: O endpoint fornecido ('" + rmiEndpointBarrels + "') não forma um URL válido!");
            return;
        } catch (RemoteException e) {
            System.out.println("Erro: Não foi possível encontrar o registo");
            return;
        }

    }
    
}
