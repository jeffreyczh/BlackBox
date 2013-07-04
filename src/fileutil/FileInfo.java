package fileutil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * This stores the overall information of a file
 */
public class FileInfo implements Serializable {
    private String fileName; // file name (MD5)
    private String originalFileName; // the original file name
    private ArrayList<FilePair> partList; // list of all parts of this file
    private long syncTime; // the server time of the last synchronization time
    
    public FileInfo(String fileName, ArrayList<FilePair> partList) {
        originalFileName = fileName;
        this.fileName = MD5Calculator.getMD5(originalFileName.getBytes());
        this.partList = partList;
    }
    public FileInfo(String fileName, SmallFile[] pieces) {
        this.fileName = fileName;
        partList = new ArrayList<>(pieces.length);
        for ( int i = 0; i < pieces.length; i++ ) {
            partList.add(pieces[i].getFilePair());
        }
    }
    public String getOriginalFileName() {
        return originalFileName;
    }
    public String getFileName() {
        return fileName;
    }
    public ArrayList<FilePair> getPartList() {
        return partList;
    }
    public void setPartList(ArrayList<FilePair> partList) {
        this.partList = partList;
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
