package hello_callback;

import java.rmi.*;
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

	public void print_on_server(String s) throws RemoteException {
		System.out.println("> " + s);
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
			LocateRegistry.createRegistry(1099).rebind("XPTO", h);
			System.out.println("Hello Server ready.");
			while (true) {

				Hello_C_I cl = (Hello_C_I) Naming.lookup("XPTO");

				System.out.print("> ");
				a = sc.nextLine();
				
				for (Hello_C_I client : h.client) {
					client.print_on_client(a);
				}

				

			}
		} catch (Exception re) {
			System.out.println("Exception in HelloImpl.main: " + re);
		} 
	}
}
