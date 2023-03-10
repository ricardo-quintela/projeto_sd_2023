package searchEngine.barrel;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import searchEngine.ThreadSlave;


/**
 * Um {@code Downloader} é uma thread criada por um {@code Crawler}.
 * Esta corre em paralelo com o {@code Crawler} para fazer o download de
 * páginas web cujos links estão numa fila de espera num grupo de multicast.
 * 
 * Os links são adicionados à fila por um {@code Barrel}
 */
public class Barrel extends ThreadSlave implements Serializable, SearchRequest{

    private String rmiEndpoint;

    /**
     * Construtor da classe {@code Downloader}
     * 
     * @param id o id da thread
     * @param parentName o nome do objeto que criou esta thread
     * @param rmiEndpoint o endpoint do RMI registry
     */
    public Barrel(int id, String parentName, String rmiEndpoint) {
        super(id, parentName);

        this.rmiEndpoint = rmiEndpoint;
    }

    /**
     * Construtor por omissão da classe {@code Downloader}
     */
    public Barrel() {}



    @Override
    public String search(CopyOnWriteArrayList<String> query) throws RemoteException {
        return this.toString() + ": " + query.get(0);
    }



    @Override
    public void run() {
        
        try{
            // tentar ligar ao RMI
            Register server = (Register) Naming.lookup(this.rmiEndpoint);

            // subscrever ao servidor
            this.setState(server.subscribe(this.toString(), (SearchRequest) this));

            printState();


            Scanner sc = new Scanner(System.in);

            sc.nextLine();

            sc.close();


            // fim
            System.out.println(this + " terminated!");

        } catch (NotBoundException e) {
            System.out.print(this + ": ERRO: RMI nao esta ligado em " + this.rmiEndpoint + "!");
            return;
        } catch (MalformedURLException e) {
            System.out.print(this + ": ERRO: nao foi possivel encontrar o RMI em " + this.rmiEndpoint + "!");
            return;
        } catch (AccessException e) {
            System.out.print(this + ": ERRO: nao e permitido ligar ao endpoint " + this.rmiEndpoint + "!");
        } catch (RemoteException e){
            System.out.println(this + ": ERRO: Ocorreu um erro no RMI");
            e.printStackTrace();
            return;
        }
    }



    @Override
    public String toString() {
        return "Barrel" + super.toString();
    }

}

