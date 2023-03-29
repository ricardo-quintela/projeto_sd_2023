package searchEngine.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import searchEngine.search.SearchResponse;

public class Client{
    private String name;

    /**
     * Construtor por omissão da classe {@code Client}
     */
    public Client(){
    }

    /**
     * Construtor da classe {@code Client}
     * @param name o nome do cliente
     */
    public Client(String name){
        this.name = name;
    }

    /**
     * Pede ao utilizador por uma string de palavras chave para pesquisar
     * @param sc o {@code Scanner} de input ligado ao {@code stdin}
     */
    public void searchMenu(Scanner sc, SearchResponse searchModuleIF){

        String response;

        while (true){

            System.out.print("Googol - Pesquisa\nDigite palavras-chave para pesquisar e '/back' para voltar atras.\nDigite: ");
            
            // ler uma linha do stdin
            String query = sc.nextLine();
            
            // voltar atrás no menu
            if (query.equals("/back")){
                break;
            }

            // criar uma lista de palavras-chave
            CopyOnWriteArrayList<String> keywords = new CopyOnWriteArrayList<>(query.split("[^a-zA-Z0-9]+"));

            // pedir a um barrel para executar a query
            try {

                response = searchModuleIF.execSearch(this.name, keywords);

                // caso o pedido não possa ser executado
                if (response == null){
                    System.out.println("Erro: Nao houve resposta para o pedido!");
                    continue;
                }

            } catch (RemoteException e) {
                System.out.println("Erro: Ocorreu um erro do servidor ao efetuar a pesquisa!");
                continue;
            }

            // imprimir a resposta recebida
            System.out.println(response);

        }
    }

    /**
     * Pede ao utilizador um URL e envia ao SearchModule para pesquisar.
     * @param sc o {@code Scanner} de input ligado ao {@code stdin}
     */
    public void sendURL(Scanner sc, SearchResponse searchModuleIF){

        while (true){

            System.out.print("Googol - Pesquisa\nDigite um url para pesquisar e '/back' para voltar atras.\nDigite: ");
            
            // ler uma linha do stdin
            String query = sc.nextLine();
            
            // voltar atrás no menu
            if (query.equals("/back")){
                break;
            }

            // pedir a um barrel para executar a query
            try {

                if (searchModuleIF.execURL(query)){
                    System.out.println("Sucesso! '" + query + "' foi adicionado a fila!");
                } else {
                    System.out.println("Erro: Nao foi possivel adicionar o URL a fila!");
                    continue;
                }

            } catch (RemoteException e) {
                System.out.println("Erro: Ocorreu um erro do servidor ao enviar o URL!");
                continue;
            }

        }
    }


    public boolean login(Scanner sc, SearchResponse searchModuleIF){

        sc.nextLine();
        System.out.print("Username: ");
        
        // ler uma linha do stdin
        String username = sc.nextLine();

        System.out.print("Password: ");
        
        // ler uma linha do stdin
        String password = sc.nextLine();
        
        // voltar atrás no menu
        if (searchModuleIF.login(username, password)){
            return true;
        }

        return false;
    }

    public boolean registar(Scanner sc, SearchResponse searchModuleIF){

        sc.nextLine();
        System.out.print("Username: ");
        
        // ler uma linha do stdin
        String username = sc.nextLine();

        System.out.print("Password: ");
        
        // ler uma linha do stdin
        String password = sc.nextLine();
        
        // voltar atrás no menu
        if (searchModuleIF.register(username, password)){
            return true;
        }

        return false;
    }

    /**
     * Menu de utilizador
     * @param sc o {@code Scanner} de input ligado ao {@code stdin}
     */
    public void menu(Scanner sc, SearchResponse searchModuleIF){

        boolean loop = true;
        int num;
        while (loop){

            System.out.print("Googol\nDigite a opcao desejada:\n1 - Indexar um URL\n2 - Pesquisar\n3 - Registar\n4 - Login\n5 - sair\nDigite: ");

            try{
                
                num = sc.nextInt();

                switch(num){

                    // indexar um URL
                    case 1:
                        this.sendURL(sc, searchModuleIF);
                        break;

                    // pesquisar
                    case 2:
                        this.searchMenu(sc, searchModuleIF);
                        break;

                    case 3:
                        if(this.registar(sc, searchModuleIF)){
                            System.out.println("Registado com sucesso.");
                        }
                        break;

                    case 4:
                        if(this.login(sc, searchModuleIF)){
                            System.out.println("Logado com sucesso.");
                        }
                        break;

                    // sair
                    case 5:
                        loop = false;
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
     * Imprime no {@code stdin} o modo de uso do programa
     */
    private static void printUsage() {
        System.out.println("Modo de uso:\nClient {rmi_port} {rmi_endpoint} {username}\n- rmi_port: Porta do registo RMI do SearchModule\n- rmi_endpoint: Endpoint do SearchModule no registo RMI\n- username: O nome do cliente");
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

        // parsing da porta RMI
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e){
            printUsage();
            return;
        }

        // guardar o rmi_endpoint
        String rmiEndpoint = args[1];

        SearchResponse searchModuleIF;
        try{

            // ligar ao server registado no rmiEndpoint fornecido
            searchModuleIF = (SearchResponse) LocateRegistry.getRegistry(port).lookup(rmiEndpoint);

            // UrlQueueInterface ligacaoUrlQueue = (UrlQueueInterface) Naming.lookup(TratamentoStrings.urlTratamento(port, rmiEndpoint));
            // System.out.println(ligacaoUrlQueue.isEmpty());
            // ligacaoUrlQueue.add("url1");
            // System.out.println(ligacaoUrlQueue.isEmpty());
            // System.out.println(ligacaoUrlQueue.get(0));

        } catch (NotBoundException e) {
            System.out.println("Erro: não existe um servidor registado no endpoint '" + rmiEndpoint + "'!");
            return;
        } catch (AccessException e) {
            System.out.println("Erro: Esta máquina não tem permissões para ligar ao endpoint '" + rmiEndpoint + "'!");
            return;
        } catch (RemoteException e) {
            System.out.println("Erro: Não foi possível encontrar o registo");
            return;
        }

        Client client = new Client(args[2]);

        // instanciar um scanner
        Scanner sc = new Scanner(System.in);

        // menu da aplicação
        client.menu(sc, searchModuleIF);
        
        sc.close();

    }
}
