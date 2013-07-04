
package client;

import common.ActionPair;
import fileutil.FileInfo;
import fileutil.FileOps;
import fileutil.FilePair;
import fileutil.FileUtil;
import fileutil.SmallFile;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import rmiClass.RedirectorControl;
import rmiClass.ServerControl;

/**
 * indicate what actions should be taken when the watched files' status changes
 * @author
 */
public class ClientFileOps extends FileOps{
    
    protected String userName;
    protected ArrayList<String> redirectorList;
    protected String serverIP;
    
    public ClientFileOps(String userName, ArrayList<String> redirectorList) {
        super();
        this.userName = userName;
        this.redirectorList = redirectorList;
    }
    @Override
    public void run() {
        //serverIP = askServerIP();
        serverIP = "54.242.71.176";
        
        if ( eventName.equals("ENTRY_DELETE") ) {
            /* tell the server to delete this file */
            return;
        }
        
        if ( file.isFile() ) {
            fileModified();
        } else {
            dirModified();
        }
    }

    @Override
    protected void fileModified() {
        if (eventName.equals("ENTRY_CREATE")) {
            return;
        }
        SmallFile[] smallfiles = FileUtil.createSmallFiles(file.getPath());
        FileInfo fileInfo = new FileInfo(file.getPath(), smallfiles);
        while (true) {
            try {
                ServerControl serverCtrl = (ServerControl) Naming.lookup("rmi://" + serverIP + ":47805/Server");
                ActionPair  actionPair = serverCtrl.checkFileInfo(userName, fileInfo);
                ArrayList<FilePair> needPartList = actionPair.getFilePairList();
                if (needPartList.size() == 0) {
                    /* the file does not need to sync */
                    return;
                } else {
                    for (int i = 0; i < needPartList.size(); i++ ) {
                        System.out.println(needPartList.get(i).getFileName());
                    }
                }
                
                
            } catch (RemoteException ex) {
                /* the server may fail, connect to another server */
                System.out.println("Server: " + serverIP + " has no response. Ask server IP again.");
                //serverIP = askServerIP();
                ex.printStackTrace();
            }  catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void dirModified() {
        
    }
    /**
     * ask the redirector for the IP of the least load server
     * @return the server's IP
     */
    protected String askServerIP() {
        for (int i = 0; ; i++ ) {
            String ipAddr = redirectorList.get(i % 3);
            try {
                RedirectorControl rdCtrl = (RedirectorControl) Naming.lookup("rmi://" + ipAddr + ":51966/Redirect");
                return rdCtrl.redirect();
            } catch (RemoteException ex) {
                /* the server may fail, connect to another server */
                System.out.println("Re-director: " + ipAddr + " has no response. Change to another Re-director.");
            }  catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
