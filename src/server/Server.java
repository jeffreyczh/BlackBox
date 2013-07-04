
package server;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import javax.swing.Timer;
import rmiClass.RedirectorControl;

/**
 *
 * @author
 */
public class Server {
    
    final private int heartbeatInterval = 3000;
    private ArrayList<String> redirectorList = new ArrayList<>(3); // list of all ip addresses of re-director
    final ServerControlImpl serverctrl;
    
    public Server(String IPAddr) throws RemoteException {
       
       redirectorList.add("23.21.222.40");
       redirectorList.add("54.234.9.61");
       redirectorList.add("23.22.127.255");
       
       String[] userName = new String[] {"user1", "user2"};
       checkUserFolder(userName);
       
       serverctrl = new ServerControlImpl();
       LocateRegistry.createRegistry(47805);
       try {
               Naming.rebind("rmi://" + IPAddr + ":47805/Server", serverctrl);
       } catch (MalformedURLException e) {
               System.out.println("[Error] Fail to bind the method with url");
               System.exit(1);
       }
       
       //sendHeartbeat(); // get the return value to sync amoung servers
       //initHeartbeatTimer();
       
       System.out.println("Server is ready!");
    }
    /**
     * send hearbeat to all the redirectors
     * @param IPAddr 
     */
    private void sendHeartbeat() {
        for (int i = 0; i < redirectorList.size(); i++) {
            String rdIP = redirectorList.get(i);
            try {
                RedirectorControl rdCtrl = (RedirectorControl) Naming.lookup("rmi://" + rdIP + ":51966/Redirect");
                rdCtrl.heartbeat(serverctrl.getLoad());
            } catch (Exception ex){ }
        }
    }
    
    private void initHeartbeatTimer() {
        new Timer(heartbeatInterval, 
                new ActionListener() {
                   public void actionPerformed(ActionEvent evt) {
                       sendHeartbeat();
                    }
                }).start();
    }
    
    /**
     * check whether the folders which stores each user's files exists
     * if not, create the folder
     * @param userName 
     */
    private void checkUserFolder(String[] userName) {
        for (int i = 0; i < userName.length; i++) {
            File folder = new File(userName[i]);
            if ( ! folder.exists() ) {
                folder.mkdir();
            }
        }
    }
}
