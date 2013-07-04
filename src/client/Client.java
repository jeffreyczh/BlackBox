
package client;

import fileutil.FileUtil;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import rmiClass.RedirectorControl;

/**
 *
 * @author
 */
public class Client {
    private String userName;
    private ArrayList<String> redirectorList = new ArrayList<>(3); // list of all ip addresses of re-director
    private int whichIP = 0; // the index of re-director IP that this client connects to
    
    /**
     * Constructor
     * @param userName
     * @param rootPath the root path of the sync folder, eg. "C:"
     */
    public Client(String userName, String rootPath) throws RemoteException {
        
        redirectorList.add("23.21.222.40");
        redirectorList.add("54.234.9.61");
        redirectorList.add("23.22.127.255");
        
        /* log in and validate the user name first */
        /*if ( ! logIn(userName) ) {
            System.exit(1);
        }*/
        this.userName = userName;
        /* install the file watcher to listen to files status */
        FileUtil.watchFiles(Paths.get(rootPath, "blackboxsync").toString(), new ClientFileOps(userName, redirectorList));
    }
    
    /**
     * connect to the server and log in
     * @param userName
     * @return true if log in successfully
     */
    private boolean logIn(String userName) {
         while (true) {
            String ipAddr = redirectorList.get(whichIP % 3);
            try {
                /* log in and validate the user name */
                RedirectorControl rdCtrl = (RedirectorControl) Naming.lookup("rmi://" + ipAddr + ":51966/Redirect");
                if (!rdCtrl.login(userName)) {
                    System.out.println("Fail to log in: user name is not correct");
                    return false;
                }
                System.out.println("Login successfully!");
                return true;
            } catch (RemoteException ex) {
                /* the server may fail, connect to another server */
                System.out.println("Re-director: " + ipAddr + " has no response. Change to another Re-director.");
                whichIP++;
            }  catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
