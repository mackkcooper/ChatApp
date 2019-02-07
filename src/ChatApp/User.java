/*
Author: Mack Cooper
Github: mackkcooper
*/

package ChatApp;

class User {
    //members
    private String username;
    private String password;
    private boolean online = false;

    //methods
    String getUsername() {
        return username;
    }
    String getPassword() {
        return password;
    }
    boolean isOnline() {
        return online;
    }
    void display() {
        System.out.print("\nUser: " + username + " Pass: " + password + " Online: " + online);
    }
    void toggleOnline() {
        online = !online;
    }

    //constructors
    User(String u, String p) {
        username = u;
        password = p;
    }
}
