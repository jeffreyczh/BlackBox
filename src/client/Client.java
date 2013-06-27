
package client;

import fileutil.FileUtil;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import rmiClass.LoginControl;

/**
 *
 * @author
 */
public class Client {
    private String userName;
    private ArrayList<String> ipList = new ArrayList<>(3); // list of all ip addresses of servers
    private int whichIP = 0; // the index of IP in the ip list that this client connects to
    
    /**
     * Constructor
     * @param userName
     * @param rootPath the root path of the sync folder, eg. "C:"
     */
    public Client(String userName, String rootPath) {
        ipList.add("23.21.222.40");
        
        /* log in and validate the user name first */
        if ( ! logIn(userName) ) {
            System.exit(1);
        }
        this.userName = userName;
        /* install the file watcher to listen to files status */
        FileUtil.watchFiles(Paths.get(rootPath, "blackboxsync").toString());
    }
    /**
     * connect to the server and log in
     * @param userName
     * @return true if log in successfully
     */
    private boolean logIn(String userName) {
         while (true) {
            String ipAddr = ipList.get(whichIP % 3);
            try {
                /* log in and validate the user name */
                LoginControl loginCtrl = (LoginControl) Naming.lookup("rmi://" + ipAddr + ":51966/Login");
                if (!loginCtrl.validate(userName)) {
                    System.out.println("Fail to log in: user name is not correct");
                    return false;
                }
                System.out.println("Login success!");
                return true;
            } catch (RemoteException ex) {
                /* the server may fail, connect to another server */
                System.out.println("Server: " + ipAddr + " has no response. Change to another server.");
                whichIP++;
            }  catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
