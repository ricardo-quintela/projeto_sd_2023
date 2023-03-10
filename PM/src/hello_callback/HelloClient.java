package hello_callback;

import java.rmi.*;
import java.rmi.server.*;
import java.util.Scanner;

public class HelloClient extends UnicastRemoteObject implements Hello_C_I {

	HelloClient() throws RemoteException {
		super();
	}

	public void print_on_client(String s) throws RemoteException {
		System.out.println("client > " + s);
	}

	public static void main(String args[]) {
		String a;
		// usage: java HelloClient username
		/*
		System.getProperties().put("java.security.policy", "policy.all");
		System.setSecurityManager(new RMISecurityManager());
		*/
 
		try (Scanner sc = new Scanner(System.in)) {

			//User user = new User();
			Hello_S_I h = (Hello_S_I) Naming.lookup("XPTO");
			HelloClient c = new HelloClient();
	
			h.subscribe(args[0], (Hello_C_I) c);
			System.out.println("Client sent subscription to server");
	
	
			Hello_C_I i;

			while (true) {
				System.out.print("> ");
				a = sc.nextLine();
				i = h.print_on_server(a);

				i.print_on_client("HELLO MAN");
			}
		} catch (Exception e){
			e.printStackTrace();
		}


	}

}
