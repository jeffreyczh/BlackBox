
package fileutil;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The "file" which is splitted up from the original file
 * @author
 */
public class SmallFile implements Serializable{
    private FilePair pair;
    private String subpath; // the subpath of the file that this chunk belongs to
    private String recordName; // MD5 of the subpath
    private byte[] data;
    
    public SmallFile(FilePair pair, Path subpath, byte[] data) {
        this.pair = pair;
        this.subpath = subpath.toString();
        recordName = MD5Calculator.getMD5(subpath.toString().getBytes());
        this.data = data;
    }
   
    public FilePair getFilePair() {
        return pair;
    }
    
    public byte[] getData() {
        return data;
    }
    public Path getSubPath() {
        return Paths.get(subpath);
    }
    public String getRecordName() {
        return recordName;
    }
}
