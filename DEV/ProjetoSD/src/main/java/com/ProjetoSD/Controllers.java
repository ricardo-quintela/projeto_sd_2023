package com.ProjetoSD;

import com.ProjetoSD.data.HackerNewsItemRecord;
import com.ProjetoSD.data.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;
import org.springframework.ui.Model;

import searchEngine.search.SearchResponse;

import java.nio.charset.StandardCharsets;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


@Controller
public class Controllers {

    private static final Logger logger = LoggerFactory.getLogger(Controllers.class);
    private static SearchResponse searchModuleIF = null;
    private boolean logado;

    @Value("${searchModuleIP}")
    private String searchModuleIP;
    @Value("${searchModulePort}")
    private int searchModulePort;
    @Value("${searchModuleEndpoint}")
    private String searchModuleEndpoint;

    private final SimpMessagingTemplate messagingTemplate;


    @Autowired
    public Controllers(SimpMessagingTemplate messagingTemplate){
        this.messagingTemplate = messagingTemplate;
    }



    /**
     * Permite a conexao RMI com o search module
     */
    @Bean
    @Autowired
    private void makeConnection(){


        if (searchModuleIF == null){
            try{
                //earchModuleIF = (SearchResponse) LocateRegistry.getRegistry(2002).lookup("search-module");
                searchModuleIF = (SearchResponse) LocateRegistry.getRegistry(this.searchModuleIP, this.searchModulePort).lookup(this.searchModuleEndpoint);
            }  catch (NotBoundException e) {
                System.out.println("Erro: não existe um servidor registado no endpoint !");
            } catch (AccessException e) {
                System.out.println("Erro: Esta máquina não tem permissões para ligar ao endpoint '");
            } catch (RemoteException e) {
                System.out.println("Erro: Não foi possível encontrar o registo");
                e.printStackTrace();
            }
        }
        this.logado = false;
    }


    @GetMapping("/")
    private String home(){
        return "index";
    }

    /**
     *
     */
    // url/teste/url=texto
    @GetMapping("/administration")
    private String admin(){
        return "administration";
    }

    @Scheduled(fixedDelay = 500)
    @MessageMapping("/admin") // Rota para receber a solicitação do cliente
    public void handleAdminRequest() {

        try {
            String info = searchModuleIF.admin();
            messagingTemplate.convertAndSend("/topic/info", info);
        } catch (RemoteException e){
            System.out.println("Erro: Ocorreu um erro de RMI entre o SearchModule e o WebServer!");
        }
    }



    /**
     *
     * @param url
     */
    // url/teste/url=texto
    @GetMapping("/search_url")
    private String search_url(@RequestParam(name="url", required = false) String url, Model model){

        if (url != null && searchModuleIF != null && !url.equals("")){
            try{

                if (!this.logado){
                    model.addAttribute("popup", "Precisas de estar logado para esta tarefa!");
                    return "index";
                }

                CopyOnWriteArrayList<String> resultados = searchModuleIF.searchUrl("Cliente", url);
                List<Results> urls_recebidos = new ArrayList<>();

                for (String result: resultados) {
                    urls_recebidos.add(new Results("", "", result));
                }

                model.addAttribute("results", urls_recebidos);
                model.addAttribute("searched_url", url);

            } catch (RemoteException e){
                System.out.println("Ocorreu um erro de RMI entre o SearchModule e o WebServer!");
            }
        } else {
            return "search_url_home";
        }

        return "search_url";
    }

    /**
     * Recebe um url e indexa o.
     *
     * @param url string de url
     */
    // url/teste/url=texto
    @GetMapping("/index_url")
    private String indexa_url(@RequestParam(name="url", required = false) String url){

        if (url != null && !url.equals("")){
            indexa_url_aux(url);
        }

        return "index_url";
    }

    /**
     * Procura por pesquisas que contenham todas as palavras pedidas.
     *
     * @param palavra String com todas as palavras separadas por espacos
     */
    // url/teste/palavra=a%20b%20c&is_hacker_news=true
    @GetMapping("/search_words")
    private String pesquisa(@RequestParam(name="palavra", required = false) String palavra, @RequestParam(name="is_hacker_news", required = true, defaultValue = "0") String is_hacker_news, @RequestParam(name="page", required = true, defaultValue = "1") int page, Model model){

        if (palavra != null && searchModuleIF != null && !palavra.equals("")){

            // Separamos em palavras simples, exceto a ultima que dira informacao extra
            String[] palavras = palavra.split(" ", 2);
            CopyOnWriteArrayList<String> array = new CopyOnWriteArrayList<>(Arrays.asList(palavras).subList(0, palavras.length));

            for (String alguma_coisa: array) {
                System.out.println(alguma_coisa);
            }

            CopyOnWriteArrayList<String> resultados;
            CopyOnWriteArrayList<Results> results = new CopyOnWriteArrayList<>();;

            try{

                // paginação - default = 1
                if (page >= 1){
                    // Fazemos a pesquisa das palavras
                    resultados = searchModuleIF.pagination(searchModuleIF.execSearch("Cliente", array), page);

                    if (searchModuleIF.pagination(searchModuleIF.execSearch("Cliente", array), page) != null) {

                        // preencher a lista de resultados
                        for (String str : resultados) {
                            String[] parts = str.split("\\|");
                            String url = new String(parts[0].trim().replace("Url: ", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                            String titulo = new String(parts[1].trim().replace("Titulo: ", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                            String texto = new String(parts[2].trim().replace("Texto: ", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                            results.add(new Results(titulo, texto, url));
                        }
                    }

                } else {
                    return "404";
                }

                model.addAttribute("results", results);
                model.addAttribute("searched_string", palavra);
                model.addAttribute("page", page);
                model.addAttribute("is_hacker_news", is_hacker_news);

                // Se a ultima palavra assim disser, procuramos tambem no hacker
                if (is_hacker_news != null && is_hacker_news.equals("true")){
                    hacker_pesquisa_por_palavra(array);
                }

            } catch (RemoteException e){
                System.out.println("Ocorreu um erro de RMI entre o SearchModule e o WebServer!");
            }
        } else {
            System.out.println("Erro: A palavra passada como parametro esta vazia!");
            return "index";
        }

        return "search_results";
    }

    @GetMapping("/logout")
    private String logout(Model model){

        if (this.logado){
            model.addAttribute("popup", "Deslogado com sucesso.");
            this.logado = false;
        } else {
            model.addAttribute("popup", "Não te encontras logado.");
        }

        return "index";
    }

    @GetMapping("/login")
    private String login(@RequestParam(name="name", required = false) String name, @RequestParam(name="password", required = false) String password, Model model){

        if (this.logado){
            model.addAttribute("popup", "Já te encontras logado!");
            return "index";
        }

        if (name != null && password != null && searchModuleIF != null){

            if (!password.equals("") && !name.equals("")){
                try{

                    this.logado = searchModuleIF.login(name, password);
                    System.out.println("Utilizador: " + name + " Logado com sucesso = " + this.logado);

                } catch (RemoteException e){
                    System.out.println("Ocorreu um erro de RMI entre o SearchModule e o WebServer!");
                }
            } else {
                model.addAttribute("popup", "Erro no login");
                return "login";
            }

        } else {
            return "login";
        }

        if (this.logado){
            model.addAttribute("popup", "Logado");
        } else {
            model.addAttribute("popup", "Erro no login");
            return "login";
        }

        return "index";
    }

    @GetMapping("/registo")
    private String registo(@RequestParam(name="name", required = false) String name, @RequestParam(name="password", required = false) String password, Model model){

        if (name != null && password != null && searchModuleIF != null){

            if (!password.equals("") && !name.equals("")){
                try{

                    if (searchModuleIF.register(name, password)){
                        model.addAttribute("popup", "Registado");
                    }
                    else {
                        model.addAttribute("popup", "Erro no registo");
                        return "registo";
                    }

                } catch (RemoteException e){
                    System.out.println("Ocorreu um erro de RMI entre o SearchModule e o WebServer!");
                }
            } else {
                model.addAttribute("popup", "Erro no registo");
                return "registo";
            }


        } else {
            return "registo";
        }

        return "index";
    }

    /**
     * Procura pelas top stories escritas pelo autor pedido.
     *
     * @param user autor
     */
    @Async
    public void hacker_pesquisa_por_autor(String user){
        // url/teste/palavra=a%20b%20c

        if (user != null){
            // Site das top stories
            String str = "https://hacker-news.firebaseio.com/v0/topstories.json";

            // Download dos ids das top stories
            RestTemplate restTemplate = new RestTemplate();
            List<Integer> hacker = restTemplate.getForObject(str , List.class);

            if (hacker == null) return;

            // Percorremos todos os ids e verificamos os seus autores
            List<String> urls = new ArrayList<>();
            for (Integer i: hacker) {
                RestTemplate rest_2 = new RestTemplate();
                HackerNewsItemRecord hackerNewsItemRecord = rest_2.getForObject("https://hacker-news.firebaseio.com/v0/item/" + i + ".json", HackerNewsItemRecord.class);

                if (hackerNewsItemRecord == null) continue;

                // Verificamos se o autor e o correto
                if (!hackerNewsItemRecord.by().equals(user)) {
                    indexa_url_aux(hackerNewsItemRecord.url());
                }
            }
        }
    }

    /**
     * Liga-se ao hackernews e indexa os urls das estorias do autor fornecido como
     * parametro
     * @param user o autor das estorias a indexar
     * @return a vista de /top_user
     */
    @GetMapping("/top_user")
    private String hacker_news_author(@RequestParam(name="user", required = false) String user){

        if (user != null && !user.equals("")){
            hacker_pesquisa_por_autor(user);
        }

        return "top_user";
    }


    /**
     * Liga-se ao hackerNews e procura pelas "top stories" que contenham as palavras desejadas.
     * As encontradas têm o seu url indexado na fila.
     *
     * @param array CopyOnWriteArrayList<String> com as palavras da pesquisa
     */
    @Async
    public void hacker_pesquisa_por_palavra(CopyOnWriteArrayList<String> array){

        // Ligacao ao site e vars necessarias
        String str = "https://hacker-news.firebaseio.com/v0/topstories.json";
        boolean verificador = true;

        // Download dos ids das top stories
        RestTemplate restTemplate = new RestTemplate();
        List<Integer> hacker = restTemplate.getForObject(str , List.class);

        if (hacker == null) return;

        // Percorrer todos os ids
        for (Integer i: hacker) {
            verificador = true;

            RestTemplate rest_2 = new RestTemplate();
            HackerNewsItemRecord hackerNewsItemRecord = rest_2.getForObject("https://hacker-news.firebaseio.com/v0/item/" + i + ".json", HackerNewsItemRecord.class);

            if (hackerNewsItemRecord == null || hackerNewsItemRecord.text() == null){
                continue;
            }

            // Sempre que se encontre uma palavra que não esteja presente nao adicionamos o url e passamos ao proxima
            for (String palavra: array){
                if (!hackerNewsItemRecord.text().contains(palavra)){
                    verificador = false;
                    break;
                }
            }

            if (verificador) {
                indexa_url_aux(hackerNewsItemRecord.url());
            }
        }
    }



    /**
     * Funcao auxiliar que recebe um url e indexa o.
     *
     * @param url String url
     */
    private void indexa_url_aux(String url) {
        if (searchModuleIF != null && url != null){
            try{
                searchModuleIF.execURL(url);
            } catch (RemoteException e){
                System.out.println("Ocorreu um erro de RMI entre o SearchModule e o WebServer!");
            }
        } else {
            System.out.println("Erro: O URL fornecido esta vazio!");
        }
    }

}
