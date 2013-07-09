
package rmiClass;

import fileutil.FileInfo;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import server.SyncPacket;

/**
 * for synchronizing among servers
 * @author
 */
public interface ServerSyncControl extends Remote{
    public ArrayList<FileInfo> checkUpdate(String userName) throws RemoteException;
    public SyncPacket getUpdate(String userName, String recordName) throws RemoteException;
    public void sendUpdate(String userName, SyncPacket pack, boolean toDelete) throws RemoteException;
}
