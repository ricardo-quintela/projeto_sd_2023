package searchEngine.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import searchEngine.search.SearchResponse;
import searchEngine.utils.Log;
import searchEngine.fileWorker.TextFileWorker;

public class Client{
    private String name;

    private ArrayList<String> rmiEndpointSM;
    private ArrayList<Integer> rmiPortSM;

    private String lastSearch;

    private Log log;

    /**
     * Construtor por omissão da classe {@code Client}
     */
    public Client(){
        this.name = null;
        this.rmiEndpointSM = new ArrayList<>();
        this.rmiPortSM = new ArrayList<>();
        this.log = new Log();
        this.lastSearch = null;
    }

    /**
     * Construtor da classe {@code Client}
     * @param name o nome do cliente
     */
    public Client(String name){
        this.name = name;
        this.rmiEndpointSM = new ArrayList<>();
        this.rmiPortSM = new ArrayList<>();
        this.log = new Log();
        this.lastSearch = null;
    }

    public boolean searchURL(SearchResponse searchModuleIF){

        String response;
        Scanner sc = new Scanner(System.in);

        while (true){

            if (this.lastSearch == null){
                System.out.print("Googol - Pesquisa\nDigite um url para pesquisar e '/back' para voltar atras.\nDigite: ");
                
                // ler uma linha do stdin
                this.lastSearch = sc.nextLine();
            }
            
            // voltar atrás no menu
            if (this.lastSearch.equals("/back")){
                this.lastSearch = null;
                break;
            }

            // pedir a um barrel para executar a query
            try {

                response = searchModuleIF.searchUrl(this.name, this.lastSearch);

                // caso o pedido não possa ser executado
                if (response == null){
                    System.out.println("Erro: Nao houve resposta para o pedido!");
                    this.lastSearch = null;
                    continue;
                }

            } catch (RemoteException e) {
                //System.out.println("Erro: Ocorreu um erro do servidor ao efetuar a pesquisa!");
                return false;
            }

            // imprimir a resposta recebida
            System.out.println(response);
            this.lastSearch = null;
        }

        return true;
    }

    public boolean administracao(SearchResponse searchModuleIF){

        // pedir a informacao atual
        String response = null;

        try {

            response = searchModuleIF.admin();

            // caso o pedido não possa ser executado
            if (response == null){
                System.out.println("Erro: Nao houve resposta para o pedido!");
            }

        } catch (RemoteException e) {
            //System.out.println("Erro: Ocorreu um erro do servidor ao efetuar a pesquisa!");
            return false;
        }

        // imprimir a resposta recebida
        System.out.println(response);
        return true;
    }

    /**
     * Pede ao utilizador por uma string de palavras chave para pesquisar
     */
    public boolean searchMenu(SearchResponse searchModuleIF){

        String response;
        Scanner sc = new Scanner(System.in);

        while (true){

            if (this.lastSearch == null){
                System.out.print("Googol - Pesquisa\nDigite palavras-chave para pesquisar e '/back' para voltar atras.\nDigite: ");
                
                // ler uma linha do stdin
                this.lastSearch = sc.nextLine();

            }
            
            // voltar atrás no menu
            if (this.lastSearch.equals("/back")){
                this.lastSearch = null;
                break;
            }

            // criar uma lista de palavras-chave
            CopyOnWriteArrayList<String> keywords = new CopyOnWriteArrayList<>(this.lastSearch.split("[^a-zA-Z0-9]+"));

            // pedir a um barrel para executar a query
            try {

                response = searchModuleIF.execSearch(this.name, keywords);

                // caso o pedido não possa ser executado
                if (response == null){
                    System.out.println("Erro: Nao houve resposta para o pedido!");
                    this.lastSearch = null;
                    continue;
                }

            } catch (RemoteException e) {
                //System.out.println("Erro: Ocorreu um erro do servidor ao efetuar a pesquisa!");
                return false;
            }

            // imprimir a resposta recebida
            System.out.println(response);
            this.lastSearch = null;
        }
        return true;
    }

    /**
     * Pede ao utilizador um URL e envia ao SearchModule para pesquisar.
     */
    public boolean sendURL(SearchResponse searchModuleIF){

        Scanner sc = new Scanner(System.in);

        while (true){

            if (this.lastSearch == null){
                System.out.print("Googol - Pesquisa\nDigite um url para pesquisar e '/back' para voltar atras.\nDigite: ");
                
                // ler uma linha do stdin
                this.lastSearch = sc.nextLine();

            }
            
            // voltar atrás no menu
            if (this.lastSearch.equals("/back")){
                this.lastSearch = null;
                break;
            }

            // pedir a um barrel para executar a query
            try {

                if (searchModuleIF.execURL(this.lastSearch)){
                    System.out.println("Sucesso! '" + this.lastSearch + "' foi adicionado a fila!");
                    this.lastSearch = null;
                } else {
                    System.out.println("Erro: Nao foi possivel adicionar o URL a fila!");
                    this.lastSearch = null;
                    continue;
                }

            } catch (RemoteException e) {
                //System.out.println("Erro: Ocorreu um erro do servidor ao enviar o URL!");
                return false;
            }

        }
        return true;
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
                System.out.println("Erro no login.");
            }
        } catch (RemoteException e){
            return false;
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
            return false;
        }

        return false;
    }

    /**
     * Menu de utilizador
     */
    public int menu(SearchResponse searchModuleIF, int num){

        Scanner sc = new Scanner(System.in);

        boolean loop = true;
        while (loop){

            try{
                
                if (num == 0) {
                    System.out.print("Googol\nDigite a opcao desejada:\n1 - Indexar um URL\n2 - Pesquisar\n3 - Registar\n4 - Login\n5 - Logout\n6 - Lista de paginas\n7 - Administracao\n8 - Sair\nDigite: ");
                    num = sc.nextInt();
                }
                

                switch(num){

                    // indexar um URL
                    case 1:
                        if (!this.sendURL(searchModuleIF)){
                            return num;
                        }
                        break;

                    // pesquisar
                    case 2:
                        if (!this.searchMenu(searchModuleIF)){
                            return num;
                        }
                        break;

                    // registar
                    case 3:
                        if (this.name != null) {
                            System.out.println("Já estás logado.");
                        }
                        else if (this.registo(searchModuleIF)){
                            System.out.println("Registado com sucesso.");
                        }
                        else {
                            return num;
                        }
                        break;
                    
                    // logar
                    case 4:
                        if (this.name != null) {
                            System.out.println("Já estás logado.");
                        }
                        else if (this.logar(searchModuleIF)){
                            System.out.println("Logado com sucesso.");
                        }
                        else {
                            return num;
                        }
                        break;
                    
                    // logout
                    case 5:
                        if (this.name == null) {
                            System.out.println("Não estás logado.");
                        }
                        this.name = null;
                        System.out.println("Deslogado.");
                        break;
                    
                    // lista de paginas ligadas a um url 
                    case 6:
                        if (this.name == null) {
                            System.out.println("Não estás logado.");
                        }
                        else {
                            if (!this.searchURL(searchModuleIF)){
                                return num;
                            }
                        }
                        break;
                    
                    // administracao
                    case 7:
                        if (!this.administracao(searchModuleIF)){
                            return num;
                        }
                        break;

                    // sair
                    case 8:
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
            } finally {
                num = 0;
            }

        }

        sc.close();
        return -1;
    }

    public boolean loadConfig(String path){
        TextFileWorker fileWorker = new TextFileWorker(path);
        ArrayList<String> lines = fileWorker.read();

        // ler porta RMI e endpoint dos Barrels
        try{

            // adiciona os portos e os endpoints às suas respetivas listas
            for (int i = 0; i < lines.size(); i++) {
                this.rmiPortSM.add(Integer.parseInt(lines.get(i).split("/")[0]));
                this.rmiEndpointSM.add(lines.get(i).split("/")[1]);
            }

        } catch (NumberFormatException e){
            log.error(toString(), "Ocorreu um erro ao ler o ficheiro de configuracao em '" + path + "'! Porta invalida!");
            return false;
        } catch (IndexOutOfBoundsException e){
            log.error(toString(), "Ocorreu um erro ao ler o ficheiro de configuracao em '" + path + "'! Um URL foi mal especificado!");
            return false;
        }

        if (this.rmiPortSM.size() == 0){
            log.error(toString(), "Configuracao deve especificar a porta do registo RMI do SearchModule, o endpoint do SearchModule no sesu próprio registo, a porta do registo RMI dos Barrels e os endpoints de cada Barrel um por linha");
            return false;
        }

        
        log.info(toString(), "Configuracao carregada!");
        return true;
        
    }

    /**
     * Imprime no {@code stdin} o modo de uso do programa
     */
    private static void printUsage() {
        System.out.println("Modo de uso:\nClient {config_file}\n- config_file: Config file dos clientes\n");
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
        
        // parsing da porta RMI
        SearchResponse searchModuleIF;
        Client client = new Client();
        if (!client.loadConfig(args[0])){
            return;
        }

        int index = 0, valor = 0;
        while (true){

            if (index == client.rmiEndpointSM.size()) index = 0;

            try{
    
                // ligar ao server registado no rmiEndpoint fornecido
                searchModuleIF = (SearchResponse) LocateRegistry.getRegistry(client.rmiPortSM.get(index)).lookup(client.rmiEndpointSM.get(index));
    
                // menu da aplicação
                valor = client.menu(searchModuleIF, valor);
                if(valor == -1){
                    return;
                }

            } catch (NotBoundException e) {
                //System.out.println("Erro: não existe um servidor registado no endpoint '" + client.rmiEndpointSM.get(index) + "'!");
                index ++;
            } catch (AccessException e) {
                //System.out.println("Erro: Esta máquina não tem permissões para ligar ao endpoint '" + client.rmiEndpointSM.get(index) + "'!");
                index ++;
            } catch (RemoteException e) {
                //System.out.println("Erro: Não foi possível encontrar o registo");
                index ++;
            }
    
        }
        
    }
}
