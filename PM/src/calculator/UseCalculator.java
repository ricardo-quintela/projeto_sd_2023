package calculator;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class UseCalculator {

	/**
	 * @param args
	 * @throws NotBoundException 
	 * @throws RemoteException 
	 * @throws MalformedURLException 
	 * @throws DivByZeroException 
	 */
	public static void main(String[] args) throws MalformedURLException, RemoteException, NotBoundException, DivByZeroException {
		System.out.println("Simple example of use of a remote calculator");
		CalculatorInterface ci = (CalculatorInterface) Naming.lookup("rmi://localhost/calc");
		System.out.println("8 + 3 = " + ci.request());
	}

}
