
package fileutil;

import java.io.File;

/**
 * The operation taken to take care of file status change event
 * it is used in WatchFiles class
 * and both clients and servers should implement it based on their needs
 * @author
 */
public abstract class FileOps implements Runnable {
    protected String eventName;
    protected File file; // the file that triggers this event
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
    public void setFile(File file) {
        this.file = file;
    }
    /**
     * actions taken for file modification
     */
    protected abstract void fileModified();
    /**
     * actions taken for directory modification (simply deletion)
     */
    protected abstract void dirModified();
}
