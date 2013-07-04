
package common;

import java.io.Serializable;

/**
 * the class for identify an action
 * @author
 */
public class ActionId implements Serializable {
    final private String userName;
    final private long actionTime; // the server time when the action happens
    
    public ActionId(String userName, long actionTime) {
        this.userName = userName;
        this.actionTime = actionTime;
    }
    
    public String getUser() {
        return userName;
    }
    
    public long getActionTime() {
        return actionTime;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && obj.getClass() == ActionId.class) {
            ActionId anotherAID = (ActionId) obj;
            if (this.getUser().equals(anotherAID.getUser()) &&
                this.getActionTime() == anotherAID.getActionTime()) {
                return true;
            }
        }
        return false;
    }
}
