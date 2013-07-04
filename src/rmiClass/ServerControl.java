
package rmiClass;

import common.ActionPair;
import fileutil.FileInfo;
import fileutil.SmallFile;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author
 */
public interface ServerControl extends Remote{
    public ActionPair checkFileInfo(String userName, FileInfo fileInfo) throws RemoteException;
    public void sendChunk(ActionPair actionPair, SmallFile chunk, boolean isLastChunk) throws RemoteException;
    public int getLoad() throws RemoteException;
}
