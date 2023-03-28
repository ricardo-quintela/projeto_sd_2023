package sockets;

import java.net.*;
import java.io.*;

public class UDPServer{
	private static int port = 6789;

	public static void main(String args[]) throws InterruptedException{ 
		try (DatagramSocket aSocket = new DatagramSocket(port)) {
			
			System.out.println("Socket Datagram à escuta no porto " + port);
			while(true){
				byte[] buffer = new byte[1000]; 			
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);
				String s = new String(request.getData(), 0, request.getLength());	
				System.out.println("Server Recebeu: " + s);	
				
				DatagramPacket reply = new DatagramPacket(request.getData(), 
				request.getLength(), request.getAddress(), request.getPort());
				aSocket.send(reply);
			}
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		}
	}
}