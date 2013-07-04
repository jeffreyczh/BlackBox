
package fileutil;

/**
 * The "file" which is splitted up from the original file
 * @author
 */
public class SmallFile {
    private FilePair pair;
    private String fileBelongsTo; // the full name of the file that this chunk belongs to (MD5)
    private byte[] data;
    
    public SmallFile(FilePair pair, String fileBelongsTo, byte[] data) {
        this.pair = pair;
        this.fileBelongsTo = fileBelongsTo;
        this.data = data;
    }
   
    public FilePair getFilePair() {
        return pair;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public String getFileBelongsTo() {
        return fileBelongsTo;
    }
}
