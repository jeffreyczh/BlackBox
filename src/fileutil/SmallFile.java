
package fileutil;

/**
 * The "file" which is splitted up from the original file
 * @author
 */
public class SmallFile {
    private FilePair pair;
    private byte[] data;
    
    public SmallFile(FilePair pair, byte[] data) {
        this.pair = pair;
        this.data = data;
    }
   
    public FilePair getFilePair() {
        return pair;
    }
    
    public byte[] getData() {
        return data;
    }
}
