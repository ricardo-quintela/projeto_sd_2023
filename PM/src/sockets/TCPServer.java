package sockets;
// TCPServer2.java: Multithreaded server
import java.net.*;
import java.io.*;

public class TCPServer{
    private static int serverPort = 6000;

    public static void main(String args[]){
        int numero=0;
        
        try (ServerSocket listenSocket = new ServerSocket(serverPort)) {
            System.out.println("A escuta no porto 6000");
            System.out.println("LISTEN SOCKET=" + listenSocket);
            while(true) {
                Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                System.out.println("CLIENT_SOCKET (created at accept())="+clientSocket);
                numero++;
                new Connection(clientSocket, numero);
            }
        } catch(IOException e) {
            System.out.println("Listen:" + e.getMessage());
        }
    }
}

//= Thread para tratar de cada canal de comunicação com um cliente
class Connection extends Thread {
    DataInputStream in;
    DataOutputStream out;
    Socket clientSocket;
    int thread_number;
    
    public Connection (Socket aClientSocket, int numero) {
        thread_number = numero;
        try{
            clientSocket = aClientSocket;
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            this.start();
        }catch(IOException e){System.out.println("Connection:" + e.getMessage());}
    }
    //=============================
    public void run(){
        String resposta;
        try {
            while(true){
                //an echo server
                String data = in.readUTF();
                System.out.println("T[" + thread_number + "] Recebeu: "+data);
                resposta=data.toUpperCase();
                out.writeUTF(resposta);
            }
        } catch(EOFException e) {
            System.out.println("EOF:" + e);
        } catch(IOException e) {
            System.out.println("IO:" + e);
        }
    }
}