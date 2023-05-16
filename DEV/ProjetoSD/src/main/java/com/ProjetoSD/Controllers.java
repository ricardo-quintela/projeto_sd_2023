package com.ProjetoSD;

import com.ProjetoSD.data.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;
import org.springframework.ui.Model;

import org.springframework.web.servlet.view.RedirectView;
import searchEngine.search.SearchResponse;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
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

    /**
     * Permite a conexao RMI com o search model
     */
    @Bean
    @Autowired
    private void makeConnection(){
        if (searchModuleIF == null){
            try{
                searchModuleIF = (SearchResponse) LocateRegistry.getRegistry(2002).lookup("search-module");
                //searchModuleIF = (SearchResponse) LocateRegistry.getRegistry(IP, 2002).lookup("search-module");
            }  catch (NotBoundException e) {
                System.out.println("Erro: não existe um servidor registado no endpoint !");
            } catch (AccessException e) {
                System.out.println("Erro: Esta máquina não tem permissões para ligar ao endpoint '");
            } catch (RemoteException e) {
                System.out.println("Erro: Não foi possível encontrar o registo");
                e.printStackTrace();
            }
        }
    }


    @GetMapping("/")
    private RedirectView home(){
        return new RedirectView("/search_words");
    }

    /**
     *
     */
    // url/teste/url=texto
    @GetMapping("/admin")
    private void admin(){

        if (searchModuleIF != null){
            try{
                System.out.println(searchModuleIF.admin());
            } catch (Exception e){
                System.out.println("Erro");
            }
        } else {
            System.out.println("UPS");
        }
    }

    /**
     *
     * @param url
     */
    // url/teste/url=texto
    @GetMapping("/search_url")
    private String search_url(@RequestParam(name="url", required = false) String url){

        if (url != null && searchModuleIF != null){
            try{
                System.out.println(url);
                searchModuleIF.searchUrl("Cliente", url);

            } catch (Exception e){
                System.out.println("Erro");
            }
        } else {
            System.out.println("UPS");
        }
        return "home/index";
    }

    /**
     * Recebe um url e indexa o.
     *
     * @param url string de url
     */
    // url/teste/url=texto
    @GetMapping("/index_url")
    private String indexa_url(@RequestParam(name="url", required = false) String url){

        if (url != null){
            indexa_url_aux(url);
        }

        return "index_url/index_url";
    }

    /**
     * Procura por pesquisas que contenham todas as palavras pedidas.
     *
     * @param palavra String com todas as palavras separadas por espacos
     */
    // url/teste/palavra=a%20b%20c&is_hacker_news=true
    @GetMapping("/search_words")
    private String pesquisa(@RequestParam(name="palavra", required = false) String palavra, @RequestParam(name="is_hacker_news", required = false) String is_hacker_news, Model model){

        if (palavra != null && searchModuleIF != null){

            // Separamos em palavras simples, exceto a ultima que dira informacao extra
            String[] palavras = palavra.split(" ", 2);
            CopyOnWriteArrayList<String> array = new CopyOnWriteArrayList<>(Arrays.asList(palavras).subList(0, palavras.length));

            for (String alguma_coisa: array) {
                System.out.println(alguma_coisa);
            }

            try{
                // Fazemos a pesquisa das palavras
                CopyOnWriteArrayList<String> resultados = searchModuleIF.execSearch("Cliente", array);

                CopyOnWriteArrayList<Results> results = new CopyOnWriteArrayList<>();
                for (String str: resultados){
                    String[] parts = str.split("\\|");
                    String url = parts[0].trim().replace("Url: ", "");
                    String titulo = parts[1].trim().replace("Titulo: ", "");
                    String texto = parts[2].trim().replace("Texto: ", "");
                    results.add(new Results(titulo, texto, url));
                }
                model.addAttribute("result", results);

                // Se a ultima palavra assim disser, procuramos tambem no hacker
                if (is_hacker_news != null && is_hacker_news.equals("true")){
                    System.out.println("OLA EU SOU FIXE");
                    hacker_pesquisa_por_palavra(array);
                }

            } catch (Exception e){
                e.printStackTrace();
            }
        } else {
            System.out.println("UPS");
        }

        return "search_results/search_results";
    }



    /**
     * Procura pelas top stories escritas pelo autor pedido.
     *
     * @param user autor
     */
    @GetMapping("/top_user")
    private void hacker_pesquisa_por_autor(@RequestParam(name="user", required = true) String user){
        // url/teste/palavra=a%20b%20c

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




    /**
     * Liga-se ao hackerNews e procura pelas "top stories" que contenham as palavras desejadas.
     * As encontradas têm o seu url indexado na fila.
     *
     * @param array CopyOnWriteArrayList<String> com as palavras da pesquisa
     */
    private void hacker_pesquisa_por_palavra(CopyOnWriteArrayList<String> array){

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
            } catch (Exception e){
                System.out.println("Erro");
            }
        } else {
            System.out.println("UPS");
        }
    }

}