/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package blackbox;

import fileutil.FileUtil;

/**
 * 
 * @author 
 */
public class TestDriver {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
       FileUtil.watchFiles("D:\\test");
    }
}
