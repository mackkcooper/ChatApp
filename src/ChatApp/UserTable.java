/*
Author: Mack Cooper
Github: mackkcooper
*/

package ChatApp;

import java.util.Hashtable;
import java.util.Scanner;

class UserTable extends Hashtable {
    //MEMBERS
    int userCount = 0;

    //METHODS
    User add(User user) {
        if(containsKey(user.getUsername()))
            return (User) get(user.getUsername());
        ++userCount;
        return (User) put(user.getUsername(), user);
    }

    User fetch(String username) {
        return (User) get(username);
    }

    //CONSTRUCTORS
    UserTable() {
        super();
    }


    //main for testing
    public static void main(String args[]) {
        UserTable myTable = new UserTable();
        Scanner scan = new Scanner(System.in);
        String u;
        String p;
        User myUser;
        for(int i = 0; i < 100; ++i) {
            System.out.print("\nu: ");
            u = scan.nextLine();
            System.out.print("p: ");
            p = scan.nextLine();
            myUser = new User(u, p);
            myUser = myTable.add(myUser);
            if(myUser == null)
                System.out.println("Success!");
            else
                myUser.display();
            myUser = myTable.fetch(u);
            if(myUser == null)
                System.out.println("null");
            else
                myUser.display();
            System.out.println("\nCount: " + myTable.userCount);
        }
    }
}
