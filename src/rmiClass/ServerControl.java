
package rmiClass;

import fileutil.FileInfo;
import fileutil.FilePair;
import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 *
 * @author
 */
public interface ServerControl extends Remote{
    public ArrayList<FilePair> checkFileInfo(FileInfo fileInfo) throws RemoteException;
    public void sendFile(File file) throws RemoteException;
    public void addLoad(int load) throws RemoteException;
    public int getLoad() throws RemoteException;
}
