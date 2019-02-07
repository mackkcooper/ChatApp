/*
Author: Mack Cooper
Github: mackkcooper
*/

package ChatApp;

import java.io.Serializable;

class Command implements Serializable {
    //MEMBERS
    String command;
    String message;
    String username;
    String password;
    String userList;

    //METHODS
    //client -> system
    void loginRequest(String u, String p) {
        wipe();
        command = "login request";
        username = u;
        password = p;
    }

    //system -> client
    void loginSuccess(String u) {
        wipe();
        command = "login success";
        username = u;
    }

    //client -> system
    void logoutRequest(String u) {
        wipe();
        command = "logout request";
        username = u;
    }

    //system -> client
    void logoutSuccess(String u) {
        wipe();
        command = "logout success";
        username = u;
    }

    //client -> system
    void registerRequest(String u, String p) {
        wipe();
        command = "register request";
        username = u;
        password = p;
    }

    //system -> client
    void registerSuccess(String u) {
        wipe();
        command = "register success";
        username = u;
    }

    //client -> system & system -> all clients
    void message(String u, String msg) {
        wipe();
        command = "message";
        username = u; //person sending message
        message = msg;
    }

    //client -> system & system -> client
    void messageUser(String u, String msg, String dest) {
        wipe();
        command = "message user";
        username = u;
        message = msg;
        userList = dest;
    }

    //client -> system & system -> all clients
    void close() {
        wipe();
        command = "close";
    }

    //system -> client
    void error(String msg) {
        wipe();
        command = "error";
        message = msg;
    }

    //system -> all clients
    void onlineChange(String msg, String ul) {
        wipe();
        command = "online change";
        message = msg;
        userList = ul;
    }

    //clears all data from Command
    private void wipe() {
        command = null;
        message = null;
        username = null;
        password = null;
        userList = null;
    }
}
