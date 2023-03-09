package searchEngine.search;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import searchEngine.barrel.Register;

public class SearchModule {


    public static void main(String[] args) {
        
        // tratamento de erros nos parametros
        if (args.length > 1){
            System.out.print("ERRO: Parametros a mais!");
            return;
        }

        String rmiEndpoint = args[0];
        
        try{

            // tentar ligar ao RMI
            Register server = (Register) Naming.lookup(rmiEndpoint);


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
