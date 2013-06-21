package fileutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
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
            int chunksCount = 0; // how many chunks the file can be splited up to
            File file = new File(path);
            long fileSize = file.length(); // total bytes of the file
            FileInputStream fileIs;
            try {
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
        /**
         * watch the status of all files under the specified folder
         * status includes creation, modification and deletion
         * @param folderPath the path of the folder that is watched
         */
        public static void watchFiles(String folderPath) {
            try {
                // temporary put this function here, may change
                final WatchService watchService = FileSystems.getDefault().newWatchService();
                /*
                 *  match the key with the path
                 *  so that the full path of the folder which triggers events can be known
                 */
                final HashMap<WatchKey, Path> keyMap = new HashMap<>();
                /* traverse the given folder and register the watch keys for all files in it */
                registerWatchService(Paths.get(folderPath).toAbsolutePath(),
                                     keyMap,
                                     watchService);
                while(true) {
                    WatchKey key = watchService.take(); // wait for an event happends
                    for (WatchEvent<?> event: key.pollEvents()) {
                        Path tempFileFullPath = Paths.get(keyMap.get(key).toString(), event.context().toString());
                        File tempFile = tempFileFullPath.toFile();
                        if ( tempFile.isDirectory() ) {
                            if ( event.kind().name().equals("ENTRY_CREATE") ) {
                                /* if there is a new folder created, register for it */
                                registerWatchService(tempFile.toPath(),
                                                     keyMap,
                                                     watchService);
                                continue;
                            }
                            if ( event.kind().name().equals("ENTRY_MODIFY") ) {
                                /* ignore the folder modification event */
                                continue;
                            }
                        }
                        /* belows are how we gonna deal with each event */
                        System.out.println(tempFileFullPath + " has: " + event.kind());
                    }
                    key.reset();
                }
            } catch (IOException ex) {
                System.out.println("IOException: fail to traverse the folder:" + folderPath);
            } catch (InterruptedException ex) {
                System.out.println("InterruptedException: fail to watch the folder:" + folderPath);
            }
        }
        /**
         * traverse the directory and register watch service for all files
         * under this directoy and sub-directory
         * @param dir the directory needs to be traversed
         * @param keyMap the hashmap the matches the watch keys and paths
         * @param watchService 
         */
        public static void registerWatchService( Path dir, 
                                                    final HashMap<WatchKey, Path> keyMap,
                                                    final WatchService watchService) throws IOException {
            Files.walkFileTree(Paths.get(dir.toString()), 
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir,
                            BasicFileAttributes attrs) throws IOException {
                       WatchKey key = dir.register(watchService, 
                                          StandardWatchEventKinds.ENTRY_CREATE,
                                          StandardWatchEventKinds.ENTRY_MODIFY,
                                          StandardWatchEventKinds.ENTRY_DELETE);
                        keyMap.put(key, dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
        }
}
