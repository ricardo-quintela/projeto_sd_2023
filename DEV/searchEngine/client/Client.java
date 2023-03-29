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
        this.name = null;
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
     */
    public void searchMenu(SearchResponse searchModuleIF){

        String response;
        Scanner sc = new Scanner(System.in);

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
     */
    public void sendURL(SearchResponse searchModuleIF){

        Scanner sc = new Scanner(System.in);

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

    /**
     * Tentamos logar alguem na base de dados.
     * 
     * @return true em caso de login bem sucedido
     */
    public boolean logar(SearchResponse searchModuleIF){

        Scanner sc = new Scanner(System.in);

        System.out.print("Username: ");
        
        // ler uma linha do stdin
        String username = sc.nextLine();

        System.out.print("Password: ");
        
        // ler uma linha do stdin
        String password = sc.nextLine();
        
        // voltar atrás no menu
        try {
            if (searchModuleIF.login(username, password)){
                this.name  = username;
                return true;
            } 
            else {
                System.out.print("Erro no login.");
            }
        } catch (RemoteException e){
            ;
        }

        return false;
    }

    /**
     * Tentamos registar alguem na base de dados.
     * 
     * @return true em caso de registo bem sucedido
     */
    public boolean registo(SearchResponse searchModuleIF){

        Scanner sc = new Scanner(System.in);

        System.out.print("Username: ");
        
        // ler uma linha do stdin
        String username = sc.nextLine();

        System.out.print("Password: ");
        
        // ler uma linha do stdin
        String password = sc.nextLine();
        
        // voltar atrás no menu
        try{
            if (searchModuleIF.register(username, password)){
                return true;
            }
            else {
                System.out.printf("Erro no registo.");
            }
        } catch (RemoteException e){
            ;
        }

        return false;
    }

    /**
     * Menu de utilizador
     */
    public void menu(SearchResponse searchModuleIF){

        Scanner sc = new Scanner(System.in);

        boolean loop = true;
        int num;
        while (loop){

            System.out.print("Googol\nDigite a opcao desejada:\n1 - Indexar um URL\n2 - Pesquisar\n3 - Registar\n4 - Login\n5 - sair\nDigite: ");

            try{
                
                num = sc.nextInt();

                switch(num){

                    // indexar um URL
                    case 1:
                        this.sendURL(searchModuleIF);
                        break;

                    // pesquisar
                    case 2:
                        this.searchMenu(searchModuleIF);
                        break;

                    // registar
                    case 3:
                        if (this.name != null) {
                            System.out.println("Já estás logado.");
                        }
                        else if (this.registo(searchModuleIF)){
                            System.out.println("Registado com sucesso.");
                        }
                        break;
                    
                    // loagr
                    case 4:
                        if (this.name != null) {
                            System.out.println("Já estás logado.");
                        }
                        else if (this.logar(searchModuleIF)){
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

        sc.close();

    }


    /**
     * Imprime no {@code stdin} o modo de uso do programa
     */
    private static void printUsage() {
        System.out.println("Modo de uso:\nClient {rmi_port} {rmi_endpoint} {username}\n- rmi_port: Porta do registo RMI do SearchModule\n- rmi_endpoint: Endpoint do SearchModule no registo RMI");
    }


    public static void main(String[] args) {
        
        // tratamento de erros nos parametros
        if (args.length == 0){
            printUsage();
            return;
        }
        
        if (args.length != 2){
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

        Client client = new Client();

        // menu da aplicação
        client.menu(searchModuleIF);
        
    }
}
