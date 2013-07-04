
package server;

import common.ActionId;
import common.ActionPair;
import fileutil.FileInfo;
import fileutil.FilePair;
import fileutil.SmallFile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import rmiClass.ServerControl;

/**
 *
 * @author
 */
public class ServerControlImpl extends UnicastRemoteObject implements ServerControl{
    
    private Integer load = 0; // the chunks that currently need to be transferred
    private HashMap<ActionId, Timer> timerMap = new HashMap<>(); // the map of timers to detect the transfer time out for each user
    final public static int TIME_OUT = 600000; // the time out is set to 10 min
    private HashMap<ActionId, SmallFile[]> chunksMap = new HashMap<>(); // the map for the buffer holds the chunks transferred by each user
    private HashMap<ActionId, Integer> loadMap = new HashMap<>(); // the map for the remained load for each transfer action
    
    public ServerControlImpl() throws RemoteException {
        super();
    }
    
    /**
     * compare the general file info and see which parts of the file need to be updated
     * @return the info of the file parts that needs to be updated. 
     * For the filepair list inside the ActionPair: 
     * if length is 0, the file does not need to be updated
     * @throws RemoteException 
     */
    @Override
    public ActionPair checkFileInfo(String userName, FileInfo fileInfo) throws RemoteException {
        ActionId actionId = new ActionId(userName, new Date().getTime());
        ArrayList<FilePair> resultList = new ArrayList<>();
        File localFile = Paths.get(userName, fileInfo.getFileName()).toFile();
        if ( localFile.exists() ) {
            try {
                /* lock the file */
                FileLock lock = new RandomAccessFile(localFile, "rw").getChannel().lock();
                /* read the general file info from the file */
                ObjectInputStream ois =  new ObjectInputStream(
                                                new FileInputStream(localFile));
                 FileInfo localFileInfo = (FileInfo) ois.readObject();
                 ois.close();
                /* compare the MD5 of each part */
                ArrayList<FilePair> localPartList = localFileInfo.getPartList();
                ArrayList<FilePair> clientPartList = fileInfo.getPartList();
                if (localPartList.size() > clientPartList.size()) {
                    /* delete the extra parts */
                    for ( int i = clientPartList.size(); i < localPartList.size(); i++ ) {
                        String path = localPartList.remove(i).getFileName();
                        try {
                            Files.delete(Paths.get(path));
                        } catch (IOException ex) {
                            System.out.println("[Error] Fail to remove the extra parts of file: " + path);
                        }
                    }
                    /* update the info */
                    localFileInfo.setPartList(localPartList);
                    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(localFile));
                    oos.writeObject(localFileInfo);
                    oos.close();
                }
                for ( int i = 0; i < clientPartList.size(); i++ ) {
                    try {
                        FilePair localfp = localPartList.get(i);
                        FilePair clientfp = clientPartList.get(i);
                        if ( clientfp.equalsTo(localfp) == 1 ) {
                            resultList.add(clientfp);
                        }
                    } catch (IndexOutOfBoundsException indexEx) {
                        /* more chunks are in the client */
                        resultList.add(clientPartList.get(i));
                    }
                }
                lock.release();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        } else {
            /* the file does not exist on the server, need to upload everything */
            resultList = new ArrayList<>(fileInfo.getPartList());
        }
        ActionPair actionPair = new ActionPair(resultList, actionId);
        /* prepare for the transfer */
        prepareTransfer(actionPair);
        return actionPair;
    }
    
    @Override
    public int getLoad() {
        return load;
    }

    public void addLoad(int load) {
        synchronized(this.load) {
            this.load += load;
        }
    }
 
    public void reduceLoad(int load) {
        synchronized(this.load) {
            this.load -= load;
        }
    }

    @Override
    public void sendChunk(ActionPair actionPair, SmallFile chunk, boolean isLastChunk) throws RemoteException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private void prepareTransfer(ActionPair actionPair) {
        int load = actionPair.getFilePairList().size();
        if (load == 0) {
            /* no transfer is needed */
            return;
        }
        ActionId actionId = actionPair.getActionId();
        SmallFile[] smallfiles = new SmallFile[load];
        chunksMap.put(actionId, smallfiles);
        loadMap.put(actionId, load);
        setupTimer(actionId);
    }
    
    private void setupTimer(final ActionId actionId) {
        Timer timer = new Timer(TIME_OUT, new ActionListener() {
           public void actionPerformed(ActionEvent evt) {
               /*
                * if time is out, the client may lose connection
                * clear the load and the buffer
                */
               reduceLoad(loadMap.get(actionId));
               loadMap.remove(actionId);
               chunksMap.remove(actionId);
           }
        });
        timer.setRepeats(false);
        timerMap.put(actionId, timer);
        timer.start();
    }
    
}
