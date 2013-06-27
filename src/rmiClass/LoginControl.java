/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rmiClass;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author
 */
public interface LoginControl extends Remote {
    public boolean validate(String userName) throws RemoteException; 
}
