package searchEngine.barrel;

import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.concurrent.CopyOnWriteArrayList;

import searchEngine.ThreadHandler;
import searchEngine.ThreadSlave;


public class BarrelHandler extends ThreadHandler implements Register, Serializable {

    private CopyOnWriteArrayList<SearchRequest> barrelInterfaces;


    /**
     * Construtor por omissão da classe {@code BarrelHandeler}
     */
    public BarrelHandler() {
        this.barrelInterfaces = new CopyOnWriteArrayList<>();
    }


    @Override
    public boolean subscribe(String name, SearchRequest barrel) throws RemoteException {
        this.barrelInterfaces.add(barrel);
        System.out.println(name + " subscribed; " + this.barrelInterfaces.size() + " interfaces in total");
        return true;
    }

    @Override
    public boolean unsubcribe(int id) throws RemoteException {
        // TODO: implement unsubscribe
        return false;
    }


    @Override
    public SearchRequest getBarrel(int id) throws RemoteException {
        return (SearchRequest) this.barrelInterfaces.get(id);
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


        // criar um objeto BarrelHandler
        BarrelHandler barrelHandler = new BarrelHandler();

        
        try{
            // criar um registo de RMI e ligar o objeto crawler
            LocateRegistry.createRegistry(rmi_port).bind(rmiEndpoint, barrelHandler);


            // criar os storage barrels e adicioná-los à lista
            for (int i = 0; i < numThreads; i++) {
                barrelHandler.threads.add(new Barrel(i, "BarrelHandler", rmiEndpoint));
            }
            
            
            // esperar pelo término das threads
            for (ThreadSlave barrel : barrelHandler.threads) {
                barrel.getThread().join();
            }
        } catch (AlreadyBoundException e){
            System.out.println("ERRO: Registo já em uso!");
            return;

        } catch(InterruptedException e){
            System.out.println("ERRO: Barrel foi interrompido!");
        }
    }
    
    
}
