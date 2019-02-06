package ChatApp;

import java.io.Serializable;

public class Message implements Serializable {
    // MEMBERS
    public String cmd; // command
    public String msg; // message
    public String usr; // username
    public String pwd; // password
    public String lst; // list of online users
    public String rec; // recipient

    // METHODS
    /**
     * Client -> Server
     * @param user - username of user logging in
     * @param password - password of user logging in
     */
    public void loginRequest(String user, String password) {
        wipe();
        cmd = "ir"; //login request
        usr = user;
        pwd = password;
    }

    /**
     * Server -> Client
     * @param user - password of user logging in
     */
    public void loginSuccess(String user) {
        wipe();
        cmd = "is"; //login success
        usr = user;
    }

    /**
     * Client -> Server
     * @param user - username of user logging out
     */
    public void logoutRequest(String user) {
        wipe();
        cmd = "or"; //logout request
        usr = user;
    }

    /**
     * Server -> Client
     * @param user - username of user logging out
     */
    public void logoutSuccess(String user) {
        wipe();
        cmd = "os"; //logout success
        usr = user;
    }

    /**
     * Client -> Server
     * @param user - username of user registering
     * @param password - password of user registering
     */
    public void registerRequest(String user, String password) {
        wipe();
        cmd = "rr"; //register request
        usr = user;
        pwd = password;
    }

    /**
     * Server -> Client
     * @param user - username of user registering
     */
    public void registerSuccess(String user) {
        wipe();
        cmd = "rs"; //register success
        usr = user;
    }

    /**
     * Client -> Server & Server -> All Clients
     * @param user - username of user sending message
     * @param message - message being sent
     */
    public void messageAll(String user, String message) {
        wipe();
        cmd = "ma";
        usr = user; //person sending message
        msg = message;
    }

    /**
     * Client <-> Server
     * @param sender - username of message sender
     * @param message - message being sent
     * @param recipient - username of message recipient
     */
    public void messageUser(String sender, String message, String recipient) {
        wipe();
        cmd = "mu";
        usr = sender;
        msg = message;
        rec = recipient;
    }

    /**
     * Client -> Server & Server -> All Clients
     */
    public void close() {
        wipe();
        cmd = "c";
    }

    /**
     * Server -> Client
     * @param message - error message
     */
    public void error(String message) {
        wipe();
        cmd = "e"; //error
        msg = message;
    }

    /**
     * Server -> All Clients
     * @param message - holds message about who signed on or off
     * @param userList - list of all online users
     */
    public void statusChange(String message, String userList) {
        wipe();
        cmd = "sc"; //status change
        msg = message;
        lst = userList;
    }

    /**
     * clears all data from Command object
     */
    private void wipe() {
        cmd = null;
        msg = null;
        usr = null;
        pwd = null;
        lst = null;
        rec = null;
    }
}
