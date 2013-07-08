
package rmiClass;

import common.ActionId;
import common.ActionPair;
import fileutil.FileInfo;
import fileutil.Packet;
import fileutil.SmallFile;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 *
 * @author
 */
public interface ServerControl extends Remote{
    public ActionPair checkFileInfo(String userName, FileInfo fileInfo) throws RemoteException;
    public long uploadChunk(ActionId actionid, SmallFile chunk, boolean isLastChunk) throws RemoteException;
    public Packet download(String userName, String subpathHash) throws RemoteException;
    public void deleteFile(String userName, String fileNameHash) throws RemoteException;
    public ArrayList<FileInfo> getRecord(String userName) throws RemoteException;
    public int getLoad() throws RemoteException;
}
