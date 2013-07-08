package fileutil;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

/**
 * This stores the overall information of a file
 */
public class FileInfo implements Serializable {
    private String subpath; // the subpath of the file under the sync folder
    private String MD5Name; // the MD5 of the subpath
    private ArrayList<FilePair> chunkList; // list of all parts of this file
    private long syncTime; // the server time of the last synchronization time
    
    public FileInfo(Path path, ArrayList<FilePair> chunkList) {
        this.subpath = path.toString();
        MD5Name = initMD5Name();
        this.chunkList = chunkList;
    }
    public FileInfo(Path path, ArrayList<FilePair> chunkList, long syncTime) {
        this.subpath = path.toString();
        MD5Name = initMD5Name();
        this.chunkList = chunkList;
        this.syncTime = syncTime;
    }
    public FileInfo(Path path, SmallFile[] pieces) {
        this.subpath = path.toString();
        MD5Name = initMD5Name();
        if ( pieces == null ) {
            chunkList = new ArrayList<>(0);
        } else {
            chunkList = new ArrayList<>(pieces.length);
            for ( int i = 0; i < pieces.length; i++ ) {
                chunkList.add(pieces[i].getFilePair());
            } 
        }
    }
    private String initMD5Name() {
        return MD5Calculator.getMD5(subpath.toString().getBytes());
    }
    public String getMD5Name() {
        return MD5Name;
    }
    public Path getSubpath() {
        return Paths.get(subpath);
    }
    public ArrayList<FilePair> getChunkList() {
        return chunkList;
    }
    public void setChunkList(ArrayList<FilePair> chunkList) {
        this.chunkList = chunkList;
    }
    /**
     * update the synchronization time
     * this method should be called on server side
     * never on client side
     */
    public void updateSyncTime(long syncTime) {
        this.syncTime = syncTime;
    }
    public long getSyncTime() {
        return syncTime;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass() == FileInfo.class) {
            FileInfo another = (FileInfo) obj;
            if (this.getMD5Name().equals(another.getMD5Name())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.MD5Name);
        return hash;
    }
}
