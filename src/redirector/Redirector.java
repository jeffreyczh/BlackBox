
package redirector;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import rmiClass.RedirectorControl;

/**
 *
 * @author
 */
public class Redirector {
    
    public Redirector(String IPAddr) throws RemoteException {
        RedirectorControl rdctrl = new RedirectorControlImpl();
        LocateRegistry.createRegistry(51966);
        try {
                Naming.rebind("rmi://" + IPAddr + ":51966/Redirect", rdctrl);
        } catch (MalformedURLException e) {
                System.out.println("[Error] Fail to bind the method with url");
                System.exit(1);
        }
        System.out.println("Redirector is ready!");
    }
}
