package searchEngine.barrel;

import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import searchEngine.ThreadHandler;
import searchEngine.ThreadSlave;
import searchEngine.search.SearchResponse;


public class BarrelHandler extends ThreadHandler implements Register, Serializable {

    /**
     * Construtor por omissão da classe {@code BarrelHandeler}
     */
    public BarrelHandler() {
    }


    @Override
    public boolean subscribe(Barrel barrel) throws RemoteException {
        this.threads.add(barrel);
        System.out.println(this.threads.size());
        return true;
    }

    @Override
    public boolean unsubcribe(int id) throws RemoteException {
        for (int i = 0; i < threads.size(); i++) {
            if (threads.get(i).getId() == id){
                this.threads.remove(i);
                return true;
            }
        }
        return false;
    }


    @Override
    public SearchRequest getBarrel() throws RemoteException {
        System.out.println(this.threads.size());
        return (SearchRequest) this.threads.get(this.threads.size() - 1);
    }


    @Override
    public SearchRequest getBarrel(int id) throws RemoteException {
        return (SearchRequest) this.threads.get(id);
    }


    public static void main(String[] args) throws RemoteException {
        // tratamento de erros nos parâmetros iniciais
        if (args.length == 0) {
            System.out.println("ERRO: Deve ser especificado o número de threads Barrel, a porta do registo RMI e o nome do registo!");
            return;
        }

        if (args.length > 3) {
            System.out.println("ERRO: Parametros a mais!");
            return;
        }

        // fazer parse do numero de threads
        int numThreads = parsePositiveInt(args[0]);

        if (numThreads < 1) {
            System.out.println("ERRO: numero de threads invalido!");
            return;
        }

        int rmi_port = parsePositiveInt(args[1]);

        if (numThreads < 1) {
            System.out.println("ERRO: numero de porta invalido!");
            return;
        }

        String rmiEndpoint = args[2];


        // criar um objeto Crawler
        BarrelHandler crawler = new BarrelHandler();

        
        try{
            // criar um registo de RMI e ligar o objeto crawler
            LocateRegistry.createRegistry(rmi_port).bind(rmiEndpoint, crawler);


            // criar os downloaders
            for (int i = 0; i < numThreads; i++) {
                new Barrel(i, "BarrelHandler", rmiEndpoint);
            }
            
            
            // esperar pelo término das threads
            for (ThreadSlave downloader : crawler.threads) {
                downloader.getThread().join();
            }
        } catch (AlreadyBoundException e){
            System.out.println("ERRO: Registo já em uso!");
            return;

        } catch(InterruptedException e){
            System.out.println("ERRO: Barrel foi interrompido!");
        }
    }
    
    
}
