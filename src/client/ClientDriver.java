
package client;

import java.rmi.RemoteException;

/**
 *
 * @author
 */
public class ClientDriver {
    public static void main(String[] args) throws RemoteException {
        new Client(args[0], args[1]);
    }
}
