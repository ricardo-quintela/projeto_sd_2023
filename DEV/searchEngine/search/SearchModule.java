package searchEngine.search;

import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Scanner;
import searchEngine.barrel.SearchRequest;

public class SearchModule {


    /**
     * Imprime no {@code stdin} o modo de uso do programa
     */
    private static void printUsage() {
        System.out.println("Modo de uso:\nsearch {server_endpoint}");
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
            SearchRequest barrel = (SearchRequest) Naming.lookup(rmiEndpoint);

            ArrayList<String> query = new ArrayList<>();

            query.add("Ola");

            System.out.println(barrel.search(query));

            
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
