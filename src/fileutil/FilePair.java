package fileutil;

/**
 * This is the object that matchs a file name with its MD5
 * @author
 */
public class FilePair {
    private String fileName;
    private String md5Str;
    
    public FilePair(String fileName, String md5Str) {
        this.fileName = fileName;
        this.md5Str = md5Str;
    }
    /**
     * compare with another file pair
     * @param pair
     * @return 0 if both file names and MD5 are same; -1 if file names are
     * different; 1 if file names are same but MD5 are different
     */
    public int equalsTo(FilePair pair) {
        if ( ! fileName.equals(pair.getFileName())) {
            return -1;
        }
        if (md5Str.equals(pair.getMD5())) {
            return 0;
        }
        return 1;
    }
    public String getFileName() {
        return fileName;
    }
    public String getMD5() {
        return md5Str;
    }
}
