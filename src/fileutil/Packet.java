
package fileutil;

import java.io.Serializable;
import java.util.ArrayDeque;

/**
 * a packet containing all chunks of a file and a timestamp of the server
 * it is used for downloading a file
 * @author
 */
public class Packet implements Serializable {
    private ArrayDeque<byte[]> dataQueue = new ArrayDeque<>();
    final private long syncTime;
    
    public Packet(ArrayDeque<byte[]> dataQueue, long syncTime) {
        this.dataQueue = dataQueue;
        this.syncTime = syncTime;
    }
    public ArrayDeque<byte[]> getDataQueue() {
        return dataQueue;
    }
    public long getSyncTime() {
        return syncTime;
    }
}
