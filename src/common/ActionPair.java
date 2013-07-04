
package common;

import fileutil.FilePair;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * it is the return value of checkFileInfo() on the server side
 * @author
 */
public class ActionPair implements Serializable {
    final private ArrayList<FilePair> fpList;
    final private ActionId actionId;
    
    public ActionPair(ArrayList<FilePair> fpList, ActionId actionId) {
        this.fpList = fpList;
        this.actionId = actionId;
    }
    
    public ArrayList<FilePair> getFilePairList() {
        return fpList;
    }
    
    public ActionId getActionId() {
        return actionId;
    }
}
