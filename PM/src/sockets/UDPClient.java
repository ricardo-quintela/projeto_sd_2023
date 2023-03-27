package sockets;

import java.net.*;
import java.util.Scanner;
import java.io.*;

public class UDPClient{
	private static int serverPort = 6789;
	
	public static void main(String args[]){ 
		// argumentos da linha de comando: hostname 
		if(args.length == 0){
			System.out.println("java UDPClient hostname");
			System.exit(0);
		}
		
		try (DatagramSocket aSocket = new DatagramSocket()) {			
			try (Scanner sc = new Scanner(System.in)) {
				while(true){
					String texto = sc.nextLine();
					byte [] m = texto.getBytes();
					
					InetAddress aHost = InetAddress.getByName(args[0]);
					DatagramPacket request = new DatagramPacket(m,m.length,aHost,serverPort);
					aSocket.send(request);
					
					byte[] buffer = new byte[1000];
					//aSocket.setSoTimeout(10000);
					DatagramPacket reply = new DatagramPacket(buffer, buffer.length);	
					aSocket.receive(reply);
					System.out.println("Recebeu: " + new String(reply.getData(), 0, reply.getLength()));
				}	
			}
		}catch (SocketException e){
			System.out.println("Socket: " + e.getMessage());
		}catch (IOException e){
			System.out.println("IO: " + e.getMessage());
		}
	} 
}