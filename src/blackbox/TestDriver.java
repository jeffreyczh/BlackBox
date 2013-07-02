/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package blackbox;

import fileutil.FileUtil;
import fileutil.SmallFile;

/**
 * 
 * @author 
 */
public class TestDriver {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
       SmallFile[] result = FileUtil.createSmalFiles("D:\\blackboxsync\\adobe_jfsky_Photoshop8.01.zip", "user1");
       for (int i = 0; i < result.length; i++) {
           System.out.println(result[i].getFilePair().getFileName());
       }
    }
}
