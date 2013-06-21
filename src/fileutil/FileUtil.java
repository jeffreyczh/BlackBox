package fileutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**

 */
/**
 * This class contains all utility for dealing with files
 * @author 
 */
public class FileUtil {
	final public static int CHUNK_SIZE = 4 * 1000000; // 4MB per chunk
	
        /**
         * split up the file to chunks
         * every chunk is 4MB
         * @param path the full file path
         * @return an array of chunk
         * @throws IOException 
         */
	public static byte[][] splitFile(String path) {
            byte[][] chunks = null;
            long fileSize = 0; // total bytes of the file
            int chunksCount = 0; // how many chunks the file can be splited up to
            File file;
            FileInputStream fileIs;
            try {
                file = new File(path);
                fileSize = file.length();
                fileIs = new FileInputStream(file);
            } catch (IOException ex) {
                System.out.println("IOException: fail to open inputstream for the file:" + path);
                return null;
            }
            if (fileSize <= CHUNK_SIZE) {
                chunksCount = 1;
            } else {
                long rest = fileSize;
                while (rest >= CHUNK_SIZE) {
                    chunksCount++;
                    rest -= CHUNK_SIZE;
                }
                if (rest > 0) {
                    chunksCount++;
                }
            }
            chunks = new byte[chunksCount][];
            for (int i = 0; i < chunksCount; i++) {
                int chunkSize;
                if ( i + 1 == chunksCount) {
                    chunkSize = (int) (fileSize - i * CHUNK_SIZE);
                } else {
                    chunkSize = CHUNK_SIZE;
                }
                chunks[i] = new byte[chunkSize];
                try {
                    /* copy all bytes */
                    fileIs.read(chunks[i]);
                } catch (IOException ex) {
                    System.out.println("IOException: fail to read the file:" + path);
                }
            }
            return chunks;
	}

}
