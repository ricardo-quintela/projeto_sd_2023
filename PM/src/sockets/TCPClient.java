package sockets;

import java.net.*;
import java.util.Scanner;
import java.io.*;

public class TCPClient {
	private static int serversocket = 6000;
	
	public static void main(String args[]) {
		// args[0] <- hostname of destination
		if (args.length == 0) {
			System.out.println("java TCPClient hostname");
			System.exit(0);
		}
		
		// 1o passo - criar socket
		try (Socket s = new Socket(args[0], serversocket)) {
			System.out.println("SOCKET=" + s);

			// 2o passo
			DataInputStream in = new DataInputStream(s.getInputStream());
			DataOutputStream out = new DataOutputStream(s.getOutputStream());
			
			// 3o passo
			try (Scanner sc = new Scanner(System.in)) {
				while (true) {
					// READ STRING FROM KEYBOARD
					String texto = sc.nextLine();
	
					// WRITE INTO THE SOCKET
					out.writeUTF(texto);
					
					// READ FROM SOCKET
					String data = in.readUTF();
					
					// DISPLAY WHAT WAS READ
					System.out.println("Received: " + data);
				}
			}
			
		} catch (UnknownHostException e) {
			System.out.println("Sock:" + e.getMessage());
		} catch (EOFException e) {
			System.out.println("EOF:" + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO:" + e.getMessage());
		}
	}
}