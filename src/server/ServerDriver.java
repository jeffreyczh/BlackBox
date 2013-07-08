package server;

import java.rmi.RemoteException;

/**
 *
 * @author
 */
public class ServerDriver {
    public static void main(String[] args) {
            try {
                    new Server(args[0], args[1]);
            } catch (Exception e) {
                    System.out.println("[Error] Fail to start the server");
                    System.out.println(e.getMessage());
            }
    }
}
