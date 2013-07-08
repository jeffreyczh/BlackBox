/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import fileutil.FileInfo;
import java.io.Serializable;
import java.util.ArrayDeque;

/**
 *
 * @author
 */
public class SyncPacket implements Serializable {
    private FileInfo fileinfo;
    private ArrayDeque<byte[]> queue;
    public SyncPacket(FileInfo fileinfo, ArrayDeque<byte[]> queue) {
        this.fileinfo = fileinfo;
        this.queue = queue;
    }
    public FileInfo getFileInfo() {
        return fileinfo;
    }
    public ArrayDeque<byte[]> getDataQueue() {
        return queue;
    }
}
