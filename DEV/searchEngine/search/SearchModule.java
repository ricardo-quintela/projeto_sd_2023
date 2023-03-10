package searchEngine.search;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import searchEngine.barrel.Register;
import searchEngine.barrel.SearchRequest;

public class SearchModule implements Serializable, Remote {


    public static void main(String[] args) {
        
        // tratamento de erros nos parametros
        if (args.length > 1){
            System.out.print("ERRO: Parametros a mais!");
            return;
        }

        String rmiEndpoint = args[0];
        
        try (Scanner sc = new Scanner(System.in)){

            // tentar ligar ao RMI
            Register server = (Register) Naming.lookup(rmiEndpoint);

            CopyOnWriteArrayList<String> query = new CopyOnWriteArrayList<String>();
            
            String string;
            SearchRequest barrel;


            barrel = server.getBarrel(0);

            while (true){
                query.clear();

                string = sc.nextLine();
                if (string.equals("exit")) break;
                
                
                System.out.print("Sending to server: " + string);
                query.add(string);
                
                System.out.print(barrel + " " + barrel.search(query));
            }



        } catch (NotBoundException e) {
            System.out.print("ERRO: RMI nao esta ligado em " + rmiEndpoint + "!");
            return;
        } catch (MalformedURLException e) {
            System.out.print("ERRO: nao foi possivel encontrar o RMI em " + rmiEndpoint + "!");
            return;
        } catch (AccessException e) {
            System.out.print("ERRO: nao e permitido ligar ao endpoint " + rmiEndpoint + "!");
        } catch (RemoteException e){
            System.out.println("ERRO: Ocorreu um erro no RMI");
            e.printStackTrace();
            return;
        }

    }
    
}
