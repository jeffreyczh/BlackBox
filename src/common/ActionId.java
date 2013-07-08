
package common;

import java.io.Serializable;
import java.util.Objects;

/**
 * the class for identify an action
 * @author
 */
public class ActionId implements Serializable {
    private static final long serialVersionUID = 1L;
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

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.userName);
        hash = 79 * hash + (int) (this.actionTime ^ (this.actionTime >>> 32));
        return hash;
    }
}
