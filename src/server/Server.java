
package server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import rmiClass.LoginControl;

/**
 *
 * @author
 */
public class Server {
    
    public Server(String IPAddr) throws RemoteException {
        LoginControl loginctrl = new LoginControlImpl();
        LocateRegistry.createRegistry(51966);
        try {
                Naming.rebind("rmi://" + IPAddr + ":51966/Login", loginctrl);
        } catch (MalformedURLException e) {
                System.out.println("[Error] Fail to bind the method with url");
                System.exit(1);
        }
        System.out.println("Server is ready!");
    }
}
