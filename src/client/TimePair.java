package client;

/**
 * This indicates time information of a file
 * including a local time for editting version
 * and a server time for synchronization version
 */
public class TimePair {
    private long lastEditTime; // the last local modified time
    private long lastSyncTime; // the last server synchronization time
    /* for the comparatoin result */
    final public static int BOTH_SAME = 0; // both lastEditTime and lastSyncTime are the same to the one in another one
    final public static int SYNC_SAME = 1; // only the lastSyncTime are the same
    final public static int EDIT_SAME = 2;
    final public static int BOTH_DIFFERENT = -1;
    
    public TimePair(long lastEditTime, long lastSyncTime) {
        this.lastEditTime = lastEditTime;
        this.lastSyncTime = lastSyncTime;
    }
    public void setLastEditTime(long lastEditTime) {
        this.lastEditTime = lastEditTime;
    }
    public long getLastEditTime() {
        return lastEditTime;
    }
    public void setLastSyncTime(long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }
    public long getLastSyncTime() {
        return lastSyncTime;
    }
    /**
     * compare with another time pair
     * to see which time is different
     * @param anotherTimePair
     * @return 
     */
    public int compareTo(TimePair anotherTimePair) {
        if (lastSyncTime == anotherTimePair.getLastSyncTime() &&
            lastEditTime == anotherTimePair.getLastEditTime()) {
            return BOTH_SAME;
        }
        if (lastSyncTime == anotherTimePair.getLastSyncTime() &&
            lastEditTime != anotherTimePair.getLastEditTime()) {
            return SYNC_SAME;
        }
        if (lastEditTime == anotherTimePair.getLastEditTime()) {
            return EDIT_SAME;
        }
        return BOTH_DIFFERENT;
    }
}
