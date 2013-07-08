/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import fileutil.FileInfo;
import fileutil.FilePair;
import fileutil.FileUtil;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import rmiClass.ServerSyncControl;

/**
 *
 * @author
 */
public class ServerSyncControlImpl extends UnicastRemoteObject implements ServerSyncControl{
    public ServerSyncControlImpl() throws RemoteException {
        super();
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
                ArrayList<FilePair> chunkList = fileinfo.getChunkList();
                /* read all chunks of this file */
                for (int i = 0; i < chunkList.size(); i++) {
                    FilePair fp = chunkList.get(i);
                    byte[] b = Files.readAllBytes(Paths.get(userName, fp.getChunkFileName()));
                    dataQueue.add(b);
                }
                lock.release();
                raf.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return new SyncPacket(fileinfo, dataQueue);
    }
    
}
