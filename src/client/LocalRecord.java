package client;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This indicates general information of a local file
 * including a local time for editting version
 * and a server time for synchronization version
 * it should be store in a file under .metadata folder
 */
public class LocalRecord implements Serializable {
    final private String subpath; // the subpath of the file under the sync folder
    private long lastModifiedTime; // the last local modified time
    private long lastSyncTime; // the last server synchronization time
    
    public LocalRecord(Path path, long lastEditTime, long lastSyncTime) {
        this.subpath = path.toString();
        this.lastModifiedTime = lastEditTime;
        this.lastSyncTime = lastSyncTime;
    }
    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }
    public void setLastSyncTime(long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }
    public long getLastSyncTime() {
        return lastSyncTime;
    }
    /**
     * get the subpath of the file under the sync folder
     * 
     * @return 
     */
    public Path getSubPath() {
        return Paths.get(subpath);
    }
}
