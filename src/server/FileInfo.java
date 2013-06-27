package server;

import fileutil.FilePair;
import java.util.ArrayList;
import java.util.Date;

/**
 * This stores the overall information of a file
 */
public class FileInfo {
    private String fileName; // file name
    private ArrayList<FilePair> partList; // list of all parts of this file
    private long syncTime; // the server time of the last synchronization time
    
    public FileInfo(String fileName, ArrayList<FilePair> partList) {
        this.fileName = fileName;
        this.partList = partList;
    }
    public String getFileName() {
        return fileName;
    }
    public ArrayList<FilePair> getPartList() {
        return partList;
    }
    /**
     * update the synchronization time
     * this method should be called on server side
     * never on client side
     */
    public void updateSyncTime() {
        Date date = new Date();
        syncTime = date.getTime();
    }
    public long getSyncTime() {
        return syncTime;
    }
}
