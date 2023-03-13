package searchEngine.search;

import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;


import fileWorker.TextFileWorker;
import searchEngine.barrel.QueryIf;

public class SearchModule {

    private ArrayList<String> barrels;
    private int rmiPort;


    /**
     * Construtor por omissão da classe SearchModule
     */
    public SearchModule(){}

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

        this.barrels = (ArrayList<String>) lines.subList(2, lines.size());

        if (this.barrels.size() == 0){
            System.out.println("Erro: Configuracao deve especificar a porta do registo RMI e os endpoints de cada storage barrel um por linha");
            return false;
        }

        return true;
        
    }


    /**
     * Imprime no {@code stdin} o modo de uso do programa
     */
    private static void printUsage() {
        System.out.println("Modo de uso:\nsearch {path}\n- path: Caminho do ficheiro de configuracao");
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

        // instanciar um search module para carregar a config
        SearchModule searchModule = new SearchModule();
        searchModule.loadConfig(args[0]);

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
                QueryIf barrel = (QueryIf) Naming.lookup(searchModule.barrels.get(barrelIndex));
    
                query = (ArrayList<String>) Arrays.asList(command.split("\\s*"));
    
                System.out.println(barrel.execQuery(query));
    
                
            } catch (NotBoundException e) {
                System.out.println("Erro: não existe um servidor registado no endpoint '" + searchModule.barrels.get(barrelIndex) + "'!");
                return;
            } catch (AccessException e) {
                System.out.println("Erro: Esta máquina não tem permissões para ligar ao endpoint '" + searchModule.barrels.get(barrelIndex) + "'!");
                return;
            } catch (MalformedURLException e) {
                System.out.println("Erro: O endpoint fornecido ('" + searchModule.barrels.get(barrelIndex) + "') não forma um URL válido!");
                return;
            } catch (RemoteException e) {

                // voltar ao início da fila de barrels
                if (barrelIndex == searchModule.barrels.size()){
                    barrelIndex = 0;
                    continue;
                }
                barrelIndex += 1;
            }

        }

        sc.close();

    }
    
}
