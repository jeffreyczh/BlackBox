
package client;

import common.ActionPair;
import fileutil.FileInfo;
import fileutil.FilePair;
import fileutil.FileUtil;
import fileutil.MD5Calculator;
import fileutil.Packet;
import fileutil.SmallFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import rmiClass.RedirectorControl;
import rmiClass.ServerControl;

/**
 * indicate what actions should be taken when the watched files' status changes
 * @author
 */
public class FileOps implements Runnable{
    
    private Client client;
    private String userName;
    private ArrayList<String> redirectorList;
    private String serverIP;
    private String event;
    private Path path; // the full path of the file
    private Path subpath; // the subpath of the file
    private String subpathHash; // the subpath of the file in MD5
    public static int whichRedirector = 0;
    
    /* event list */
    final public static String DELETE = "_delete"; // delete a file on the server side
    final public static String UPLOAD = "_upload"; // upload a file to the server
    final public static String DOWNLOAD = "_download"; // download a file from the server
    
    public FileOps(Client client,
                    String event,
                    Path path) {
        super();
        this.client = client;
        this.userName = client.getUserName();
        this.redirectorList = client.getRedirectorList();
        this.event = event;
        this.path = path;
        subpath = FileUtil.parsePath(path);
        subpathHash = MD5Calculator.getMD5(
                        subpath.toString().getBytes());
    }
    
    public FileOps(Client client,
                    String event,
                    String pathHash) {
        super();
        this.client = client;
        this.userName = client.getUserName();
        this.redirectorList = client.getRedirectorList();
        this.event = event;
        this.subpathHash = pathHash;
    }
    
    @Override
    public void run() {
        serverIP = askServerIP(redirectorList);
        if (event.equals(DELETE)) {
            delete();
        } else if (event.equals(UPLOAD)) {
            System.out.println("Uploading " + path.getFileName() + "   ...");
            upload();
            System.out.println("Upload: " +  path.getFileName() + " finish.");
        } else {
            /* download a file */
            System.out.println("Downloading " + path.getFileName() + "   ...");
            download();
            System.out.println("Download: " +  path.getFileName() + " finish.");
        }
    }

    private void upload() {
       
        SmallFile[] smallfiles = FileUtil.createSmallFiles(path);
        FileInfo fileInfo = new FileInfo(subpath, smallfiles);
        while (true) {
            try {
                ServerControl serverCtrl = (ServerControl) Naming.lookup("rmi://" + serverIP + ":47805/Server");
                ActionPair  actionPair = serverCtrl.checkFileInfo(userName, fileInfo);
                ArrayList<FilePair> needChunkList = actionPair.getFilePairList();
                if (needChunkList.isEmpty()) {
                    /* 
                     * the file does not need to sync
                     * only update the sync time of the record
                     */
                    updateLocalRecord(actionPair.getActionId().getActionTime());
                    client.removeTask(path);
                    return;
                } else {
                    /* upload the chunks that the server needs to update */
                    for (int i = 0; i < needChunkList.size(); i++ ) {
                        SmallFile chunk = FileUtil.searchSmallFile(smallfiles, needChunkList.get(i));
                        if ( chunk == null ) {
                            throw new Exception("Fail to search among small files");
                        }
                        boolean isLastChunk = false;
                        if ( i == needChunkList.size() - 1 ) {
                            isLastChunk = true;
                        }
                        long serverTime = serverCtrl.uploadChunk(actionPair.getActionId(), chunk, isLastChunk);
                        if ( serverTime > -1 ) {
                            /* all chunks have been uploaded */
                            updateLocalRecord(serverTime);
                            client.removeTask(path);
                        }
                    }
                    return;
                }
                
                
            } catch (RemoteException ex) {
                /* the server may fail, connect to another server */
                System.out.println("Server: " + serverIP + " has no response. Ask server IP again.");
                //serverIP = askServerIP(redirectorList);
                ex.printStackTrace();
            }  catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * download the whole file and replace the exsiting one
     */
    private void download() {
        while (true) {
            try {
                ServerControl serverCtrl = (ServerControl) Naming.lookup("rmi://" + serverIP + ":47805/Server");
                Packet pack = serverCtrl.download(userName, subpathHash);
                if (pack.getSyncTime() == -1) {
                    return; // the file does not exist on the server side
                }
                File file = path.toFile();
                file.getParentFile().mkdirs();
                RandomAccessFile raf = null;
                try {
                    raf = new RandomAccessFile(file, "rw");
                } catch (FileNotFoundException ex) {
                    /* 
                     * the file is being opened by another program and it has been locked
                     * create a copy of it
                     */
                    raf = new RandomAccessFile(FileUtil.createCopyName(file).toFile(), "rw");
                }
                FileLock lock = raf.getChannel().lock();
                /* write the data to the file */
                raf.setLength(0);
                ArrayDeque<byte[]> dataDeque = pack.getDataQueue();
                while( ! dataDeque.isEmpty() ) {
                    raf.write(dataDeque.pop());
                }
                lock.release();
                raf.close();
                updateLocalRecord(pack.getSyncTime());
                client.removeTask(path);
                return;
            } catch (RemoteException ex) {
                /* the server may fail, connect to another server */
                System.out.println("Server: " + serverIP + " has no response. Ask server IP again.");
                serverIP = askServerIP(redirectorList);
            }  catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * update the local record related to the given file
     */
    private void updateLocalRecord(long serverTime) {
        try {
            LocalRecord record = new LocalRecord(subpath, 
                                        Files.getLastModifiedTime(path).toMillis(), 
                                        serverTime);
            File file = client.getMetaPath().resolve(Paths.get(subpathHash)).toFile();
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileLock lock = raf.getChannel().lock();
            FileUtil.writeObjectToFile(file, raf, record);
            lock.release();
            raf.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    /**
     * tell the server to delete a file
     */
    private void delete() {
        while (true) {
            try {
                ServerControl serverCtrl = (ServerControl) Naming.lookup("rmi://" + serverIP + ":47805/Server");
                serverCtrl.deleteFile(userName, subpathHash);
                return;
            } catch (RemoteException ex) {
                /* the server may fail, connect to another server */
                System.out.println("Server: " + serverIP + " has no response. Ask server IP again.");
                serverIP = askServerIP(redirectorList);
            }  catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * ask the redirector for the IP of the least load server
     * @return the server's IP
     */
    public static String askServerIP(ArrayList<String> redirectorList) {
        while(true) {
            String ipAddr = redirectorList.get(whichRedirector % redirectorList.size());
            try {
                RedirectorControl rdCtrl = (RedirectorControl) Naming.lookup("rmi://" + ipAddr + ":51966/Redirect");
                String ip = rdCtrl.redirect();
                return ip;
            } catch (RemoteException ex) {
                /* the server may fail, connect to another server */
                System.out.println("Re-director: " + ipAddr + " has no response. Change to another Re-director.");
                whichRedirector++;
            }  catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
