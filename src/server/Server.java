
package server;

import fileutil.FileInfo;
import fileutil.FilePair;
import fileutil.FileUtil;
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
import java.nio.channels.FileLock;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.Timer;
import rmiClass.RedirectorControl;
import rmiClass.ServerSyncControl;

/**
 *
 * @author
 */
public class Server {
    
    final private int heartbeatInterval = 3000;
    public Integer load = 0; // the chunks that currently need to be transferred
    private ArrayList<String> redirectorList = new ArrayList<>(3); // list of all ip addresses of re-director
    private ArrayList<String> serverList = new ArrayList<>(3); // list of all ip addresses of servers
    
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
       
       
       ServerSyncControlImpl syncctrl = new ServerSyncControlImpl(this);
       LocateRegistry.createRegistry(47805);
       try {
               Naming.rebind("rmi://" + privateIP + ":47805/ServerSync", syncctrl);
       } catch (MalformedURLException e) {
               System.out.println("[Error] Fail to bind the method with url");
               System.exit(1);
       }
       checkUpdate(userName);
       
       ServerControlImpl serverctrl = new ServerControlImpl(this);
       try {
               Naming.rebind("rmi://" + privateIP + ":47805/Server", serverctrl);
       } catch (MalformedURLException e) {
               System.out.println("[Error] Fail to bind the method with url");
               System.exit(1);
       }
       sendHeartbeat(); // get the return value to sync amoung servers
       initHeartbeatTimer();
       
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
                rdCtrl.heartbeat(load);
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
        int whichtocheck = new Random().nextInt(serverList.size()); // randomly choose a server to get backup 
        for (int i = 0; i < userNames.length; i++) {
            for(int j = 0; j < serverList.size(); j++ ) {
                String serverIP = serverList.get(whichtocheck % serverList.size());
                try {
                    ServerSyncControl syncCtrl = (ServerSyncControl) Naming.lookup("rmi://" + serverIP + ":47805/ServerSync");
                    ArrayList<FileInfo> recordList = syncCtrl.checkUpdate(userNames[i]);
                    applyUpdate(userNames[i], recordList, syncCtrl);
                    break;
                } catch (Exception ex){ 
                    whichtocheck++;
                }
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
    public void deleteLocalFile(String userName, 
                                   String recordName) throws IOException, ClassNotFoundException {
        Path recordPath = Paths.get(userName, ".metadata", recordName);
        RandomAccessFile raf = new RandomAccessFile(recordPath.toFile(), "rw");
        FileLock lock = raf.getChannel().lock();
        FileInfo record = (FileInfo) FileUtil.readObjectFromFile(recordPath.toFile(), raf);
        lock.release();
        raf.close();
        /* delete the record */
        /* busy-waiting util the file is not being used */
        while (Files.exists(recordPath)) {
            try {
                Files.delete(recordPath);
            } catch (FileSystemException ex) {}
        }
        ArrayList<FilePair> fpList = record.getChunkList();
        /* delete all chunks */
        for ( int i = 0; i < fpList.size(); i++ ) {
            String chunkName = fpList.get(i).getChunkFileName();
            Files.deleteIfExists(Paths.get(userName, chunkName));
        }
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
            Files.write(Paths.get(userName, chunkName), queue.pop());
        }
    }
    /**
     * send the file to all the other servers as backup
     * @param userName
     * @param pack 
     * @param toDelete tell if this file needs to be deleted, true if yes
     */
    public void sendUpdate(String userName, FileInfo record, boolean toDelete) {
        System.out.println("Backup processing ... ...");
        ArrayDeque<byte[]> dataQueue = new ArrayDeque<>();
        if ( !toDelete ) {
            try {
                RandomAccessFile raf = new RandomAccessFile(
                                        Paths.get(userName, ".metadata", record.getMD5Name()).toFile(), 
                                        "rw");
                FileLock lock = raf.getChannel().lock();
                dataQueue = getChunks(userName, record);
                lock.release();
                raf.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        for (int i = 0; i < serverList.size(); i++) {
            String serverIP = serverList.get(i);
            try {
                ServerSyncControl syncCtrl = (ServerSyncControl) Naming.lookup("rmi://" + serverIP + ":47805/ServerSync");
                syncCtrl.sendUpdate(userName, new SyncPacket(record, dataQueue), toDelete);
            } catch (Exception ex){ }
        }
        System.out.println("Backup Finish");
    }
    public ArrayDeque<byte[]> getChunks(String userName, FileInfo record) {
        ArrayDeque<byte[]> dataQueue = new ArrayDeque<>();
        try {
            ArrayList<FilePair> chunkList = record.getChunkList();
            /* read all chunks of this file */
            for (int i = 0; i < chunkList.size(); i++) {
                FilePair fp = chunkList.get(i);
                byte[] b = Files.readAllBytes(Paths.get(userName, fp.getChunkFileName()));
                dataQueue.add(b);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return dataQueue;
    }
}
