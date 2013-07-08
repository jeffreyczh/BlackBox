package fileutil;

import java.io.Serializable;

/**
 * This is the object that matchs a file name with its MD5
 * @author
 */
public class FilePair implements Serializable {
    private String chunkFileName; // the file name of the chunk (in MD5 format)
    private String md5Str; // the hash of the chunk
    
    public FilePair(String chunkFileName, String md5Str) {
        this.chunkFileName = chunkFileName;
        this.md5Str = md5Str;
    }
    /**
     * compare with another file pair
     * @param pair
     * @return 0 if both file names and MD5 are same; -1 if file names are
     * different; 1 if file names are same but MD5 are different
     */
    public int equalsTo(FilePair pair) {
        if ( ! chunkFileName.equals(pair.getChunkFileName())) {
            return -1;
        }
        if (md5Str.equals(pair.getMD5())) {
            return 0;
        }
        return 1;
    }
    public String getChunkFileName() {
        return chunkFileName;
    }
    public String getMD5() {
        return md5Str;
    }
}
