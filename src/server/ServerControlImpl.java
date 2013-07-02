
package server;

import fileutil.FileInfo;
import fileutil.FilePair;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import rmiClass.ServerControl;

/**
 *
 * @author
 */
public class ServerControlImpl extends UnicastRemoteObject implements ServerControl{
    
    private Integer load = 0; // the chunks that currently need to be transferred
    
    public ServerControlImpl() throws RemoteException {
        super();
    }
    /**
     * compare the general file info and see which parts of the file need to be updated
     * @param fileInfo
     * @return the info of the file parts that needs to be updated. Null if the file
     * does not exist on the server, and if length is 0, the file does not need to be updated
     * @throws RemoteException 
     */
    @Override
    public ArrayList<FilePair> checkFileInfo(FileInfo fileInfo) throws RemoteException {
        File localFile = new File(fileInfo.getFileName());
        if ( !localFile.exists() ) {
            return null;
        }
        try {
            /* lock the file */
            FileLock lock = new RandomAccessFile(localFile, "rw").getChannel().lock();
            ArrayList<FilePair> resultList = new ArrayList<>();
            FileInfo localFileInfo = null;
            try {
                ObjectInputStream ois =  new ObjectInputStream(
                                                new FileInputStream(localFile));
                 localFileInfo = (FileInfo) ois.readObject();
                 ois.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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
            
            lock.release();
            return resultList;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
        
        
    }

    @Override
    public void sendFile(File file) throws RemoteException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public int getLoad() {
        return load;
    }

    @Override
    public void addLoad(int load) throws RemoteException {
        synchronized(this.load) {
            this.load += load;
        }
    }
}
