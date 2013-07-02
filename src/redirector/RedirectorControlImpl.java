package redirector;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


import rmiClass.RedirectorControl;

public class RedirectorControlImpl extends UnicastRemoteObject implements RedirectorControl {
    
    private ArrayList<String> userList = new ArrayList<>(); // the list of all users
    /*
     * table of the IP of each server and its load
     * load means the chunks remained for the server to transfer
     */
    private HashMap<String, Integer> serverTable = new HashMap<>();
    /*
     * table of the IP of each server and its lost contact times
     * if the lost contact times reaches three
     * the server will be considered as being shut down
     * and remove it from the tables
     */
    private HashMap<String, Integer> heartbeatTable = new HashMap<>();

    public RedirectorControlImpl() throws RemoteException {
        super();
        userList.add("user1"); // mainly for testing
        userList.add("user2");
    }

    @Override
    public boolean login(String userName) throws RemoteException {
        if (userList.indexOf(userName) != -1) {
            return true;
        }
        return false;
    }
    
    @Override
    public String redirect() throws RemoteException {
        /* redirector to the server with minimum load */
        return minLoad();
    }
    
    /**
     * 
     * @param load
     * @return the server with least load before the new server added. it is used for the 
     * new server to choose a already running server to sync when it power on
     * @throws RemoteException 
     */
    @Override
    public String heartbeat(Integer load) throws RemoteException {
        String IPAddr = new String();
        String minLoadServer = null;
        try {
            IPAddr = RemoteServer.getClientHost();
        } catch (ServerNotActiveException ex) {
            ex.printStackTrace();
        }
        minLoadServer = updateHeartbeatTable(IPAddr, load);
        synchronized(serverTable) {
            serverTable.put(IPAddr, load);
        }
        return minLoadServer;
    }
    /**
     * update the heartbeat table and detect the failure server
     * @param IPAddr the ip address of the server that sent the heartbeat
     * @return the ip of the server which has least load before the new server added
     */
    private String updateHeartbeatTable(String IPAddr, int load) {
        synchronized(serverTable) {
            
            if ( !heartbeatTable.containsKey(IPAddr) ) {
                String minLoadServer = minLoad();
                heartbeatTable.put(IPAddr, load);
                System.out.println("Server " + IPAddr + " is now added.");
                return minLoadServer;
            }

            Iterator it = heartbeatTable.keySet().iterator();
            while (it.hasNext()) {
                String ip = (String) it.next();
                int lostCount = heartbeatTable.get(ip);
                if (ip.equals(IPAddr)) {
                    lostCount = 0;
                } else {
                    lostCount++;
                    if ( lostCount == heartbeatTable.size() ) {
                        /* connection is lost, remove from tables */
                        heartbeatTable.remove(ip);
                        serverTable.remove(ip);
                        System.out.println("Server " + ip + " is failed.");
                        continue;
                    }
                }
                heartbeatTable.put(ip, lostCount);
            }
        }
        return null;
    }
    /**
     * find the server with the minimum load
     * @return the IP Address of the server
     */
    private String minLoad() {
        synchronized(serverTable) {
            ArrayList<Integer> values = new ArrayList<>(serverTable.values());
            int minLoad = values.get(0);
            for (int i = 1; i < values.size(); i++) {
                minLoad = Math.min(minLoad, values.get(i));
            }
            Iterator it = serverTable.keySet().iterator();
            while (it.hasNext()) {
                String ip = (String) it.next();
                if (serverTable.get(ip).equals(minLoad)) {
                    return ip;
                }
            }
        }
        return null;
    }
}
