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

import searchEngine.barrel.QueryIf;

public class SearchModule extends UnicastRemoteObject implements SearchResponse{


    public SearchModule() throws RemoteException{

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
        System.out.println("Modo de uso:\nsearch {server_endpoint}");
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
        
        if (args.length > 1){
            printUsage();
            return;
        }

        String rmiEndpoint = args[0];
        
        try (Scanner sc = new Scanner(System.in)) {

            // ligar ao server registado no rmiEndpoint fornecido
            QueryIf barrel = (QueryIf) Naming.lookup(rmiEndpoint);

            ArrayList<String> query = new ArrayList<>();

            query.add("Ola");

            System.out.println(barrel.execQuery(query));

            SearchModule sm = new SearchModule();

            if (!register(1234, rmiEndpoint, sm)){
                return;
            }

            
        } catch (NotBoundException e) {
            System.out.println("Erro: não existe um servidor registado no endpoint '" + rmiEndpoint + "'!");
            return;
        } catch (AccessException e) {
            System.out.println("Erro: Esta máquina não tem permissões para ligar ao endpoint '" + rmiEndpoint + "'!");
            return;
        } catch (MalformedURLException e) {
            System.out.println("Erro: O endpoint fornecido ('" + rmiEndpoint + "') não forma um URL válido!");
            return;
        } catch (RemoteException e) {
            System.out.println("Erro: Não foi possível encontrar o registo");
            return;
        }

    }
    
}
