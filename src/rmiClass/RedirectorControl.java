
package rmiClass;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * the control for redirector
 * take care of client login, client-server redirection
 * and server heartbeat
 * @author
 */
public interface RedirectorControl extends Remote {
    public boolean login(String userName) throws RemoteException; 
    public String redirect() throws RemoteException;
    public String heartbeat(Integer load) throws RemoteException;
}
