/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import fileutil.FileInfo;
import fileutil.FilePair;
import fileutil.FileUtil;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import rmiClass.ServerSyncControl;

/**
 *
 * @author
 */
public class ServerSyncControlImpl extends UnicastRemoteObject implements ServerSyncControl{
    private Server server;
    public ServerSyncControlImpl(Server server) throws RemoteException {
        super();
        this.server = server;
    }
    @Override
    public ArrayList<FileInfo> checkUpdate(String userName) throws RemoteException {
        ArrayList<FileInfo> recordList = new ArrayList<>();
        /* get all metadata and return it to the requesting server */
        File[] records = Paths.get(userName, ".metadata").toFile().listFiles();
        for ( int i = 0; i < records.length; i++ ) {
            File record = records[i];
            try {
                RandomAccessFile raf = new RandomAccessFile(record, "rw");
                FileLock lock = raf.getChannel().lock();
                recordList.add((FileInfo) FileUtil.readObjectFromFile(record, raf));
                lock.release();
                raf.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return recordList;
    }

    @Override
    public SyncPacket getUpdate(String userName, String recordName) throws RemoteException {
        ArrayDeque<byte[]> dataQueue = new ArrayDeque<>();
        /* check the file record to get chunks information */
        File serverRecord = Paths.get(userName, ".metadata", recordName).toFile();
        FileInfo fileinfo = null;
        if (serverRecord.exists()) {
            try {
                RandomAccessFile raf = new RandomAccessFile(serverRecord, "rw");
                FileLock lock = raf.getChannel().lock();
                fileinfo = (FileInfo) FileUtil.readObjectFromFile(serverRecord, raf);
                /* read all chunks of this file */
                dataQueue = server.getChunks(userName, fileinfo);
                lock.release();
                raf.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return new SyncPacket(fileinfo, dataQueue);
    }
    /**
     * send the file which was just updated by the client to other servers, one at a time
     * it happens automatically right after the file has been updated
     * @param userName
     * @param pack
     * @param toDelete tell if this file needs to be deleted, true if yes
     * @throws RemoteException 
     */
    @Override
    public void sendUpdate(String userName, SyncPacket pack, boolean toDelete) throws RemoteException {
        FileInfo record = pack.getFileInfo();
        File localRecordFile = Paths.get(userName, ".metadata", record.getMD5Name()).toFile();
        if ( toDelete && localRecordFile.exists() ) {
            try {
                /* delete this file */
                server.deleteLocalFile(userName, record.getMD5Name());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }
        ArrayDeque<byte[]> dataQueue = pack.getDataQueue();
        try {
            RandomAccessFile raf = new RandomAccessFile(localRecordFile, "rw");
            FileLock lock = raf.getChannel().lock();
            if (localRecordFile.length() != 0) {
                FileInfo localRecord = (FileInfo) FileUtil.readObjectFromFile(localRecordFile, raf);
                if (localRecord.getSyncTime() >= record.getSyncTime()) {
                    /* abort the update if the version in local machine is newer */
                    lock.release();
                    raf.close();
                    return;
                }
                /* delete the all chunks of this local file first */
                ArrayList<FilePair> fpList = localRecord.getChunkList();
                for ( int i = 0; i < fpList.size(); i++ ) {
                    String chunkName = fpList.get(i).getChunkFileName();
                    Files.delete(Paths.get(userName, chunkName));
                }
            }
            /* copy the chunks to local location */
            ArrayList<FilePair> chunkList = record.getChunkList();
            for ( int i = 0; !dataQueue.isEmpty(); i++ ) {
                String chunkName = chunkList.get(i).getChunkFileName();
                Files.write(Paths.get(userName, chunkName), dataQueue.pop());
            }
            /* update the record */
            FileUtil.writeObjectToFile(localRecordFile, raf, record);
            lock.release();
            raf.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
}
