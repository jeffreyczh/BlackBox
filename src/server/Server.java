
package server;

import fileutil.FileInfo;
import fileutil.FilePair;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import javax.swing.Timer;
import rmiClass.RedirectorControl;
import rmiClass.ServerSyncControl;

/**
 *
 * @author
 */
public class Server {
    
    final private int heartbeatInterval = 3000;
    private ArrayList<String> redirectorList = new ArrayList<>(3); // list of all ip addresses of re-director
    private ArrayList<String> serverList = new ArrayList<>(3); // list of all ip addresses of servers
    private ServerControlImpl serverctrl;
    private ServerSyncControlImpl syncctrl;
    
    public Server(String privateIP, String publicIP) throws RemoteException {
       
       redirectorList.add("23.21.222.40");
       redirectorList.add("54.234.9.61");
       redirectorList.add("23.22.127.255");
       
       serverList.add("54.242.71.176");
       serverList.add("54.234.8.26");
       serverList.add("50.19.34.25");
       
       int myselfIndex = serverList.indexOf(publicIP);
       serverList.remove(myselfIndex);
       
       String[] userName = new String[] {"user1", "user2"};
       checkUserFolder(userName);
       
       serverctrl = new ServerControlImpl();
       syncctrl = new ServerSyncControlImpl();
       LocateRegistry.createRegistry(47805);
       try {
               Naming.rebind("rmi://" + privateIP + ":47805/ServerSync", syncctrl);
       } catch (MalformedURLException e) {
               System.out.println("[Error] Fail to bind the method with url");
               System.exit(1);
       }
       checkUpdate(userName);
       
       try {
               Naming.rebind("rmi://" + privateIP + ":47805/Server", serverctrl);
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
            File folder = Paths.get(userName[i], ".metadata").toFile();
            if ( ! folder.exists() ) {
                folder.mkdirs();
            }
        }
    }
    /**
     * check update from another server
     * @param userNames all user's name 
     */
    private void checkUpdate(String[] userNames) {
        System.out.println("Checking Update ... ...");
        for (int i = 0; i < userNames.length; i++) {
            for (int j = 0; j < serverList.size(); j++) {
                String serverIP = serverList.get(j);
                try {
                    ServerSyncControl syncCtrl = (ServerSyncControl) Naming.lookup("rmi://" + serverIP + ":47805/ServerSync");
                    ArrayList<FileInfo> recordList = syncCtrl.checkUpdate(userNames[i]);
                    applyUpdate(userNames[i], recordList, syncCtrl);
                    break;
                } catch (Exception ex){ }
            }
        }
        System.out.println("Finish");
    }
    private void applyUpdate(String userName, ArrayList<FileInfo> recordList, ServerSyncControl syncCtrl) {
        /* get all local records */
        ArrayList<FileInfo> localList = new ArrayList<>();
        /* get all metadata and return it to the requesting server */
        File[] records = Paths.get(userName, ".metadata").toFile().listFiles();
        try {
            for ( int i = 0; i < records.length; i++ ) {
                File record = records[i];
                ObjectInputStream ois = new ObjectInputStream(
                                            new FileInputStream(record));
                localList.add((FileInfo) ois.readObject());
                ois.close();
            }
            /* compare the local records with records from another server */
            /* firstly, check if any files have been deleted */
            checkDeletion(userName, localList, recordList);
            /* then check if any files need to be updated */
            checkDownload(userName, localList, recordList, syncCtrl);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private void checkDeletion (String userName, 
                                  ArrayList<FileInfo> localList, 
                                  ArrayList<FileInfo> serverList) throws IOException, ClassNotFoundException {
        ArrayDeque<String> deletedList = new ArrayDeque<>();
        for (int i = 0; i < localList.size(); i++) {
            FileInfo fileinfo = localList.get(i);
            int index = serverList.indexOf(fileinfo);
            if ( index != -1 ) {
                continue;
            }
            /* the file does not exist in another server, delete this file */
            deletedList.add(fileinfo.getMD5Name());
        }
        /* delete files */
        while ( !deletedList.isEmpty() ) {
            String recordName = deletedList.pop();
            deleteLocalFile(userName, recordName);
        }
    }
    /**
     * delete a local record and its related chunks
     * @param userName
     * @param recordName
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    private void deleteLocalFile(String userName, 
                                   String recordName) throws IOException, ClassNotFoundException {
        Path recordPath = Paths.get(userName, ".metadata", recordName);
            ObjectInputStream ois = new ObjectInputStream(
                                        new FileInputStream(
                                            recordPath.toFile()));
            FileInfo record = (FileInfo) ois.readObject();
            ois.close();
            ArrayList<FilePair> fpList = record.getChunkList();
            /* delete all chunks */
            for ( int i = 0; i < fpList.size(); i++ ) {
                String chunkName = fpList.get(i).getChunkFileName();
                Files.delete(Paths.get(userName, chunkName));
            }
            /* delete the record */
            Files.delete(recordPath);
    }
    private void checkDownload (String userName,
                                  ArrayList<FileInfo> localList, 
                                  ArrayList<FileInfo> serverList,
                                  ServerSyncControl syncCtrl) throws RemoteException, IOException, ClassNotFoundException {
        ArrayDeque<String> downloadQueue = new ArrayDeque<>(); // list of files which need to download
        for (int i = 0; i < serverList.size(); i++) {
            FileInfo fileinfo = serverList.get(i);
            int index = localList.indexOf(fileinfo);
            if ( index == -1 ) {
                downloadQueue.add(fileinfo.getMD5Name());
                continue;
            }
            /* if the file exists in both sides, compare the timestamp */
            if (fileinfo.getSyncTime() > localList.get(index).getSyncTime()) {
                downloadQueue.add(fileinfo.getMD5Name()); // download the file with earlier timestamp
                continue;
            }
        }
        /* download the files that need to update */
        while ( !downloadQueue.isEmpty() ) {
            download(userName, downloadQueue.pop(), syncCtrl);
        }
    }
    
    private void download(String userName, 
                           String recordName,
                           ServerSyncControl syncCtrl) throws RemoteException, IOException, ClassNotFoundException {
        File localrecord = Paths.get(userName, ".metadata", recordName).toFile();
        /* delete the local file if it exists */
        if ( localrecord.exists() ) {
            deleteLocalFile(userName, recordName);
        }
        /* get the latest version from another server */
        SyncPacket sp = syncCtrl.getUpdate(userName, recordName);
        FileInfo fileinfo = sp.getFileInfo();
        if (fileinfo == null) {
            return; // the file does not exist on the server side
        }
        ArrayDeque<byte[]> queue = sp.getDataQueue();
        /* copy the record */
        ObjectOutputStream oos = new ObjectOutputStream(
                                    new FileOutputStream(localrecord));
        oos.writeObject(fileinfo);
        oos.close();
        /* copy the chunks to local location */
        ArrayList<FilePair> chunkList = fileinfo.getChunkList();
        for ( int i = 0; !queue.isEmpty(); i++ ) {
            String chunkName = chunkList.get(i).getChunkFileName();
            File file = Paths.get(userName, chunkName).toFile();
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.write(queue.pop());
            raf.close();
        }
    }
}
