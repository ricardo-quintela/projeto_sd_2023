package hello_callback;

import java.rmi.*;

public interface Hello_S_I extends Remote {

	public Hello_C_I print_on_server(String s) throws java.rmi.RemoteException;
  public void subscribe(String name, Hello_C_I client) throws RemoteException;
}