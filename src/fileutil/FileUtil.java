package fileutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
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
         * and make all these chunks to files
         * every chunk is 4MB
         * @param path the full file path
         * @return an array of small files
         * @throws IOException 
         */
	public static byte[][] splitFile(String path) throws IOException {
            byte[][] chunks = null;
            int chunksCount = 0; // how many chunks the file can be splited up to
            File file = new File(path);
            long fileSize = file.length(); // total bytes of the file
            FileInputStream fileIs = new FileInputStream(file);
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
                /* copy all bytes */
                fileIs.read(chunks[i]);
            }
            fileIs.close();
            return chunks;
	}
        /**
         * watch the status of all files under the specified folder
         * status includes creation, modification and deletion
         * @param folderPath the path of the folder that is watched
         */
        public static void watchFiles(String folderPath, FileOps fileOps) {
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
                        if ( event.kind().name().equals("ENTRY_MODIFY") ) {
                            /*
                             * a file is (being) modified
                             * check whether the modification completes or not
                             */
                            try {
                                /* if the function fails, the file is still being modified */
                                Thread.sleep(1000);
                                 FileInputStream fis = new FileInputStream(tempFile);
                                 fis.close();
                            } catch (FileNotFoundException fileEx) {
                                continue;
                            }

                        }
                        /* belows are how we gonna deal with each event */
                        fileOps.setEventName(event.kind().name());
                        fileOps.setFile(tempFile);
                        new Thread(fileOps).start();
                    }
                    key.reset();
                }
            } catch (IOException ex) {
                System.out.println("IOException: fail to traverse the folder:" + folderPath);
                ex.printStackTrace();
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
        /**
         * parse the path of the client file
         * eg. D:\blackboxsync\hello\test.txt will become
         * \hello\test.txt
         * now we just assume all clients run on Windows
         * @param path
         * @param userName
         * @return 
         */
        public static String parsePath(String path) {
            /* remove the root and the blackboxsync folder from the path */
            String newPath = path.substring(15);
            return newPath;
        }
        
        public static SmallFile[] createSmallFiles(String path) {
            byte[][] chunks = null;
            try {
                chunks = splitFile(path);
            } catch (IOException ex) {
                System.out.println("Fail to split up the file :" + path);
                return null;
            }
            SmallFile[] smallfiles = new SmallFile[chunks.length];
            String newPath = parsePath(path);
            for ( int i = 0; i < chunks.length; i++ ) {
                /* name the small files */
                Integer pieceIndex = i + 1;
                String fileName = newPath + ".td" + pieceIndex.toString(); // the format is "full file name.td1", for example
                FilePair pair = new FilePair(MD5Calculator.getMD5(fileName.getBytes()), 
                                             MD5Calculator.getMD5(chunks[i]));
                smallfiles[i] = new SmallFile(pair, 
                                              MD5Calculator.getMD5(path.getBytes()), 
                                              chunks[i]);
            }
            return smallfiles;
        }
}
