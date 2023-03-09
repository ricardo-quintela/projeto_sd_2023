package calculator;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Calculator extends UnicastRemoteObject implements CalculatorInterface {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String id;

	protected Calculator(String id) throws RemoteException {
		super();

		this.id = id;
	}

	@Override
	public String request() throws RemoteException {
		return this.id;
	}
	
	/**
	 * @param args
	 * @throws RemoteException 
	 */
	public static void main(String[] args) throws RemoteException {
		Calculator ci = new Calculator(args[0]);

		try{
			LocateRegistry.createRegistry(1099).rebind("calc", ci);
		} catch (RemoteException e) {
		}
		System.out.println("Calculator " + ci.id + " ready...");
	}

}
