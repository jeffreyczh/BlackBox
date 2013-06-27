package server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;


import rmiClass.LoginControl;

public class LoginControlImpl extends UnicastRemoteObject implements LoginControl {

	private ArrayList<String> userList = new ArrayList<>(); // the list of all users
	
	public LoginControlImpl() throws RemoteException {
            super();
            userList.add("user1"); // mainly for testing
            userList.add("user2");
	}
	
	@Override
	public boolean validate(String userName) throws RemoteException {
            if (userList.indexOf(userName) != -1) {
                return true;
            }
            return false;
        }
}
