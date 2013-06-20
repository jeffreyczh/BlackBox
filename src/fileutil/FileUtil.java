package fileutil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
            byte[] totalBytes = null; // total bytes of the file
            int chunksCount = 0; // how many chunks the file can be splited up to
            try {
               totalBytes = Files.readAllBytes(Paths.get(path));
            } catch (OutOfMemoryError outEx) {
                System.out.println("[Error]The file " + path + "is too large to load: ");
                return null;
            } catch (IOException ex) {
                System.out.println("IOException: fail to read the file:" + path);
                return null;
            }
            if (totalBytes.length <= CHUNK_SIZE) {
                chunksCount = 1;
            } else {
                int rest = totalBytes.length;
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
                    chunkSize = totalBytes.length - i * CHUNK_SIZE;
                } else {
                    chunkSize = CHUNK_SIZE;
                }
                chunks[i] = new byte[chunkSize];
                /* copy all bytes */
                for (int j = 0; j < chunkSize; j++) {
                    chunks[i][j] = totalBytes[i * CHUNK_SIZE + j];
                }
            }
            return chunks;
	}

}
