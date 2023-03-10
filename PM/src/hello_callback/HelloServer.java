package hello_callback;

import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.*;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;


public class HelloServer extends UnicastRemoteObject implements Hello_S_I {
	public CopyOnWriteArrayList<Hello_C_I> client;

	public HelloServer() throws RemoteException {
		super();

		this.client = new CopyOnWriteArrayList<>();
	}

	@Override
	public Hello_C_I print_on_server(String s) throws RemoteException {
		System.out.println("server > " + s);
		return (Hello_C_I) this.client.get(this.client.size() - 1);
	}

	public void subscribe(String name, Hello_C_I c) throws RemoteException {
		System.out.println("Subscribing " + name);
		System.out.print("> ");
		
		this.client.add(c);
	}

	// =======================================================

	public static void main(String args[]) {
		String a;

		/*
		System.getProperties().put("java.security.policy", "policy.all");
		System.setSecurityManager(new RMISecurityManager());
		*/

		try (Scanner sc = new Scanner(System.in)) {

			//User user = new User();
			HelloServer h = new HelloServer();
			Registry r = LocateRegistry.createRegistry(1099);

			System.out.println(r);

			r.bind("XPTO", h);
			System.out.println("Hello Server ready.");
			while (true) {
	
				System.out.print("> ");
				a = sc.nextLine();
				
				for (Hello_C_I client : h.client) {
					client.print_on_client(a);
					
				}
	
				
	
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}
}
