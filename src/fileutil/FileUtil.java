package fileutil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**

 */
/**
 * This class contains all utility for dealing with files
 * @author 
 */
public class FileUtil {
	//final public static int CHUNK_SIZE = 4 * 1000000; // 4MB per chunk
         final public static int CHUNK_SIZE = 200; // 200 Byte per chunk
	
        /**
         * split up the file to chunks
         * and make all these chunks to files
         * every chunk is 4MB
         * @param path the full file path
         * @return an array of small files, null if the file length is 0 bytes
         * @throws IOException 
         */
	public static byte[][] splitFile(Path path) throws IOException {
            byte[][] chunks = null;
            int chunksCount = 0; // how many chunks the file can be splited up to
            File file = path.toFile();
            long fileSize = file.length(); // total bytes of the file
            if ( fileSize == 0 ) {
                return null;
            }
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
         * parse the path of the client file
         * eg. D:\blackboxsync\hello\test.txt will become
         * hello\test.txt
         * @param path
         * @param userName
         * @return 
         */
        public static Path parsePath(Path path) {
            /* remove the root and the blackboxsync folder from the path */
            return path.subpath(1, path.getNameCount());
        }
        
        /**
         * split up the file to chunks and pack each chunk to a SmallFile
         * @param path
         * @return Array of SmallFile. null if the file length is 0 bytes.
         */
        public static SmallFile[] createSmallFiles(Path path) {
            byte[][] chunks = null;
            try {
                chunks = splitFile(path);
                if ( chunks == null ) {
                    /* the file length is 0 */
                    return null;
                }
            } catch (IOException ex) {
                System.out.println("Fail to split up the file :" + path);
                return null;
            }
            SmallFile[] smallfiles = new SmallFile[chunks.length];
            Path subpath = parsePath(path);
            for ( int i = 0; i < chunks.length; i++ ) {
                /* name the small files */
                Integer pieceIndex = i + 1;
                String fileName = subpath + ".td" + pieceIndex.toString(); // the format is "subpath.td1", for example
                FilePair pair = new FilePair(MD5Calculator.getMD5(fileName.getBytes()), 
                                             MD5Calculator.getMD5(chunks[i]));
                smallfiles[i] = new SmallFile(pair, 
                                              subpath, 
                                              chunks[i]);
            }
            return smallfiles;
        }
        /**
         * search a small file with the given file pair in it
         * @param smallfiles
         * @param fp
         * @return null if no result found
         */
        public static SmallFile searchSmallFile(SmallFile[] smallfiles, FilePair fp) {
            for ( int i = 0; i < smallfiles.length; i++ ) {
                FilePair tmpfp = smallfiles[i].getFilePair();
                if ( tmpfp.equalsTo(fp) == 0 ) {
                    return smallfiles[i];
                }
            }
            return null;
        }
        /**
         * read an object from the file
         * should ensure locking before call this method
         * @param raf
         * @return 
         */
        public static Object readObjectFromFile(File file, RandomAccessFile raf) {
            try {
                raf.seek(0);
                byte[] bytes = new byte[(int) file.length()];
                raf.read(bytes);
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(in);
                Object object = ois.readObject();
                ois.close();
                in.close();
                return object;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }
        public static void writeObjectToFile(File file, RandomAccessFile raf, Object obj) {
            
            try {
                raf.setLength(0);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(out);
                oos.writeObject(obj);
                byte[] bytes = out.toByteArray();
                oos.close();
                out.close();
                raf.write(bytes);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        /**
         * create a name for the copy of an existing file
         * @param file
         * @return the path of the copy
         */
        public static Path createCopyName(File file) {
            String name = new String();
            String originalName = file.getName();
            String suffix = new String();
            int index = originalName.lastIndexOf(".");
            if (index == -1) {
                name = originalName;
            } else {
                name = originalName.substring(0, index);
                suffix = originalName.substring(index, originalName.length());
            }
            Path parent = file.getParentFile().toPath();
            for (int i = 1; ; i++) {
                String newName = name + "(" + i + ")" + suffix;
                Path newpath = parent.resolve(Paths.get(newName));
                if ( ! Files.exists(newpath) ){
                    return newpath;
                }
            }
        }
}
