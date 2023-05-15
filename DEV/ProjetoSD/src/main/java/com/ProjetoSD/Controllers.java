package com.ProjetoSD;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import searchEngine.search.SearchResponse;

import java.lang.reflect.Array;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
public class Controllers {

    private static final Logger logger = LoggerFactory.getLogger(Controllers.class);
    private static SearchResponse searchModuleIF = null;

    @Bean
    @Autowired
    private void makeConnection(){
        System.out.println("PASSOU AQUI");
        if (searchModuleIF == null){
            try{
                searchModuleIF = (SearchResponse) LocateRegistry.getRegistry(2002).lookup("search-module");
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

    // url/teste/url=texto
    @GetMapping("/search_url")
    private void search_url(@RequestParam(name="url", required = true) String url){

        if (searchModuleIF != null){
            try{
                System.out.println(url);
                searchModuleIF.searchUrl("Cliente", url);
            } catch (Exception e){
                System.out.println("Erro");
            }
        } else {
            System.out.println("UPS");
        }
    }

    // url/teste/url=texto
    @GetMapping("/indexa_url")
    private void indexa_url(@RequestParam(name="url", required = true) String url){

        if (searchModuleIF != null){
            try{
                System.out.println(url);
                searchModuleIF.execURL(url);
            } catch (Exception e){
                System.out.println("Erro");
            }
        } else {
            System.out.println("UPS");
        }
    }

    // url/teste/palavra=a%20b%20c
    @GetMapping("/search_palavras")
    private void pesquisa(@RequestParam(name="palavra", required = true) String palavra){

        if (searchModuleIF != null){
            CopyOnWriteArrayList<String> array = new CopyOnWriteArrayList<>();
            String[] palavras = palavra.split(" ", 2);

            Collections.addAll(array, palavras);
            try{
                System.out.println(searchModuleIF.execSearch("Cliente", array));

            } catch (Exception e){
                System.out.println("Erro");
            }
        } else {
            System.out.println("UPS");
        }
    }

    // url/teste/palavra=a%20b%20c
    @GetMapping("/top_user")
    private void hacker_pesquisa_por_autor(@RequestParam(name="user", required = true) String user){

        String str = "https://hacker-news.firebaseio.com/v0/topstories.json";

        RestTemplate restTemplate = new RestTemplate();
        List<Integer> hacker = restTemplate.getForObject(str , List.class);

        List<String> urls = new ArrayList<>();
        for (int i = 0; i < hacker.size(); i++) {
            RestTemplate rest_2 = new RestTemplate();
            HackerNewsItemRecord hackerNewsItemRecord = rest_2.getForObject("https://hacker-news.firebaseio.com/v0/item/" + i + ".json", HackerNewsItemRecord.class);

            if (hackerNewsItemRecord == null) {
                continue;
            }

            // Neste momento apenas verifica se a palavra é exatamente igual
            if (!hackerNewsItemRecord.by().equals(user)) {
                urls.add(hackerNewsItemRecord.url());
            }
        }

        if (searchModuleIF != null && urls.size() > 0){
            try{
                for (String url : urls) {
                    System.out.println(searchModuleIF.execURL(url));
                }
            } catch (Exception e){
                System.out.println("Erro");
            }
        } else {
            System.out.println("UPS");
        }
    }

    @GetMapping("/search/")
    private void indexTopStories(@RequestParam(name="palavra", required = false) String palavra){

        //TODO: ligar rmi hacker e receber as topstories
        String str = "https://hacker-news.firebaseio.com/v0/topstories.json";

        RestTemplate restTemplate = new RestTemplate();
        List<Integer> hacker = restTemplate.getForObject(str , List.class);

        System.out.println(hacker);

        //TODO: quais têm as palavras
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < hacker.size(); i++) {
            RestTemplate rest_2 = new RestTemplate();
            HackerNewsItemRecord hackerNewsItemRecord = rest_2.getForObject("https://hacker-news.firebaseio.com/v0/item/" + i + ".json", HackerNewsItemRecord.class);

            if (hackerNewsItemRecord == null) {
                continue;
            }

            // Neste momento apenas verifica se a palavra é exatamente igual
            if (!hackerNewsItemRecord.text().equals(palavra)) {
                urls.add(hackerNewsItemRecord.url());
            }
        }

        //TODO: ligar ao searchmodel e enviar os ips
        if (urls.size() > 0){
            ;
        }

        return;
    }

}
