package searchEngine.client;

import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Scanner;

import searchEngine.search.SearchResponse;

public class Client{
    private String name;

    public Client(){

    }

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

        try{

            // ligar ao server registado no rmiEndpoint fornecido
            SearchResponse ligacaoSearchModule = (SearchResponse) Naming.lookup("rmi://localhost:1234/ola");
            ligacaoSearchModule.postResponse("asad", "asdasd");

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
