/*
Author: Mack Cooper
Github: mackkcooper
*/

package ChatApp;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatServer extends JFrame implements ActionListener {
    //MEMBERS
    private int port = 12345;
    private int threadLimit = 20;
    private ServerSocket ser;
    private int cliCount = 0;
    private boolean shutdownServer = false;
    private UserTable userTable = new UserTable();

    //GUI MEMBERS
    private String hostname = "HOST";
    private JTextArea display;
    private JTextField text;
    private ThreadList threadList = new ThreadList();
    private JTextArea userDisplay;
    private String onlineUserList;


    //METHODS
    //sets up server
    private void startup() {
        try {
            ser = new ServerSocket(port);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        updateOnlineUserList();
        setVisible(true);
        runServer();
    }

    //begins loop for accepting client join requests
    private void runServer() {
        ExecutorService pool = Executors.newFixedThreadPool(threadLimit);
        while(!shutdownServer) {
            try {
                Socket cli = ser.accept();
                ++cliCount;
                displayToServer("System: New client connected - # " + cliCount);
                ServerThread myThread = new ServerThread(cli, cliCount);
                pool.execute(myThread);
                threadList.add(myThread);
            } catch(Exception e) {
                if(!(e instanceof SocketException)) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
        //SHUTDOWN PORTION
        try {
            threadList.removeAll();
            pool.shutdown();
            ser.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    //closes down server
    private void shutdownServer() {
        displayToServer("System: Shutting down...");
        try {
            ser.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        shutdownServer = true;
    }

    //sets up the GUI for the server
    private void serverGUISetup() {
        setSize(689, 470);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosed(e);
                shutdownServer();
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        add(panel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3,3,3,3);

        userDisplay = new JTextArea(25, 15);
        userDisplay.setLineWrap(false);
        userDisplay.setEditable(false);
        JScrollPane userScroll = new JScrollPane(userDisplay);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 3;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel.add(userScroll, gbc);

        display = new JTextArea(25, 40);
        display.setText("System: Welcome to Mack's Chat Server!");
        display.setLineWrap(true);
        display.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(display);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        panel.add(chatScroll, gbc);

        text = new JTextField(33);
        text.addActionListener(this);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        panel.add(text, gbc);

        JButton send = new JButton("Send");
        send.addActionListener(this);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        panel.add(send, gbc);
    }

    //catches actions performed in GUI
    public void actionPerformed(ActionEvent e) {
        String input = text.getText();
        if(!input.equals("")) {
            text.setText("");
            Command command = new Command();
            if(input.charAt(0) == '@') {
                String dest = parseMessage(input);
                if(dest.equals(hostname)) {
                    displayToServer("System: Cannot send direct message to yourself.");
                    return;
                } else if(dest.equals("")) {
                    displayToServer("System: Could not determine user to send message to.");
                    return;
                }
                ServerThread recipient = threadList.findUser(dest);
                if(recipient == null) {
                    displayToServer("System: Username '" + dest + "' not found.");
                } else {
                    displayToServer(hostname + ": " + input);
                    command.message(hostname, input);
                    recipient.sendCommand(command);
                }
            } else {
                displayToServer(hostname + ": " + input);
                command.message(hostname, input);
                threadList.sendCommandAll(command);
            }
        }
    }

    //interprets message that is being sent to only certain users
    private String parseMessage(String msg) {
        int size = msg.length();
        String dest = "";
        char c;
        for(int i = 1; i < size; ++i) {
            c = msg.charAt(i);
            if(c == ' ')
                return dest;
            else
                dest += c;
        }
        return "";
    }

    //posts given text to server chat room display
    private void displayToServer(String msg) {
        display.append("\n" + msg);
        display.setCaretPosition(display.getDocument().getLength());
    }

    //updates the list of online users
    private void updateOnlineUserList() {
        onlineUserList = threadList.displayOnline();
        userDisplay.setText(onlineUserList);
    }

    //CONSTRUCTORS
    private ChatServer(int port, int threadLimit) {
        super("Chat Server");
        serverGUISetup();
        this.port = port;
        this.threadLimit = threadLimit;
    }


    //SERVER THREAD - instantiated for each client that connects to server
    private class ServerThread implements Runnable {
        //MEMBERS
        Socket socket;
        int identifier;
        User user = null;
        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        ServerThread next = null;
        boolean shutdownThread = false;

        //METHODS
        //loop to catch incoming commands from client
        public void run() {
            while(!shutdownThread) {
                try {
                    Command command = (Command) in.readObject();
                    commandHandler(command);
                } catch(Exception e) {
                    if(e instanceof EOFException)
                        shutdownThread();
                    else
                        e.printStackTrace();
                }
            }
            try {
                out.close();
                in.close();
                socket.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        //handles incoming commands from client
        private void commandHandler(Command c) {
            if(c != null) {
                String commandType = c.command;
                Command command = new Command();
                switch (commandType) {
                    case "register request": //client is requesting to register username and password
                        displayToServer("System: " + c.username + " attempting registration.");
                        User temp = userTable.fetch(c.username); //checks to see if username taken
                        if (temp == null) { //username available
                            temp = new User(c.username, c.password); //creates new user
                            userTable.add(temp); //adds user to user data hash table
                            displayToServer("System: " + c.username + " registered successfully.");
                            command.registerSuccess(temp.getUsername());
                            sendCommand(command);
                        } else { //username taken
                            displayToServer("System: Username '" + c.username + "' taken.");
                            command.error("Username '" + c.username + "' taken.");
                            sendCommand(command);
                        }
                        break;

                    case "login request":
                        displayToServer("System: " + c.username + " attempting to log in...");
                        int loginReturn = login(c.username, c.password);
                        if (loginReturn == 0) { //login success
                            displayToServer("System: " + c.username + " logged in.");
                            updateOnlineUserList();
                            command.loginSuccess(c.username);
                            sendCommand(command);
                            command = new Command();
                            command.onlineChange(c.username + " logged in.", onlineUserList);
                            sendCommandAll(command);
                        } else if (loginReturn == 1) { //username not found
                            displayToServer("System: Username '" + c.username + "' not found.");
                            command.error("Username '" + c.username + "' not found.");
                            sendCommand(command);
                        } else if (loginReturn == 2) { //username already online
                            displayToServer("System: " + c.username + " already online.");
                            command.error(c.username + " already online.");
                            sendCommand(command);
                        } else if (loginReturn == 3) { //incorrect password
                            displayToServer("System: " + c.username + " attempted to login with incorrect password.");
                            command.error("Incorrect password given.");
                            sendCommand(command);
                        }
                        break;

                    case "logout request":
                        displayToServer("System: " + c.username + " attempting to log out...");
                        if (!logout())
                            displayToServer("System: " + c.username + " was already logged out.");
                        else
                            displayToServer("System: " + c.username + " logged out.");
                        updateOnlineUserList();
                        command.onlineChange(c.username + " logged out.", onlineUserList); //notify all users
                        sendCommandAll(command);
                        command.logoutSuccess(c.username); //send confirmation to user logging off
                        sendCommand(command);
                        break;

                    case "message":
                        displayToServer(c.username + ": " + c.message);
                        sendCommandAll(c);
                        break;

                    case "message user":
                        ServerThread dest = threadList.findUser(c.userList);
                        displayToServer("System: " + c.username + " sending message '" + c.message + "' to " + c.userList + ".");
                        if(dest == null) {
                            displayToServer("System: Username '" + c.userList + "' not found.");
                            command.message("System", "Username '" + c.userList + "' not found.");
                            sendCommand(command);
                        } else {
                            displayToServer(c.username + ": " + c.message);
                            command.message(c.username, c.message);
                            dest.sendCommand(command);
                            sendCommand(command);
                        }
                        break;

                    case "close":
                        displayToServer("System: Client # " + identifier + " closed connection.");
                        threadList.remove(identifier);
                        break;

                    default:
                        displayToServer("System: Unknown command type - " + c.command);
                }
            }
        }

        //logs in given username and password
        private int login(String u, String p) {
            User temp = userTable.fetch(u); //see if username is in table
            if(temp == null) //username is not registered
                return 1;
            if(temp.isOnline()) //user is already online
                return 2;
            if(temp.getPassword().equals(p)) { //user found and password matches
                user = temp; //pass user to thread
                user.toggleOnline(); //mark user as online
                return 0; //return success
            } else { return 3; } //incorrect password given
        }

        private boolean logout() {
            if(user == null) //user not logged in anyway
                return false;
            if(user.isOnline()) //user is marked as online
                user.toggleOnline();
            Command command = new Command();
            command.logoutSuccess(user.getUsername());
            sendCommand(command);
            user = null;
            return true; //user is successfully marked as offline and disassociated with thread
        }

        private void sendCommand(Command command) {
            if(command != null) {
                try {
                    out.writeObject(command);
                    out.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendCommandAll(Command command) {
            threadList.sendCommandAll(command);
        }

        private void shutdownThread() {
            shutdownThread = true;
        }

        //CONSTRUCTOR
        ServerThread(Socket s, int id) {
            socket = s;
            identifier = id;
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }


    //THREAD LIST - linear linked list of all clients connected to server
    private class ThreadList {
        //MEMBERS
        int threadCount = 0;
        ServerThread head = null;

        //METHODS
        private boolean add(ServerThread st) {
            if(st == null)
                return false;
            st.next = head;
            head = st;
            ++threadCount;
            return true;
        }

        private boolean remove(int id) {
            ServerThread temp = head;
            ServerThread prev = head;
            while(temp != null) {
                if(id == temp.identifier) {
                    if(temp == head)
                        head = temp.next;
                    else
                        prev.next = temp.next;
                    temp.shutdownThread();
                    --threadCount;
                    return true;
                }
                prev = temp;
                temp = temp.next;
            }
            return false;
        }

        private void removeAll() {
            ServerThread temp;
            while(head != null) {
                temp = head.next;
                head.shutdownThread();
                head = temp;
            }
        }

        private void sendCommandAll(Command command) {
            ServerThread temp = head;
            while(temp != null) {
                temp.sendCommand(command);
                temp = temp.next;
            }
        }

        private ServerThread findUser(String u) {
            if(u == null)
                return null;
            ServerThread temp = head;
            while(temp != null) {
                if(temp.user.getUsername().equals(u))
                    return temp;
                temp = temp.next;
            }
            return null;
        }

        private String displayOnline() {
            String userList = "Online Users:\n" + hostname;
            ServerThread temp = head;
            while(temp != null) {
                if(temp.user != null) {
                    userList = userList + "\n" + temp.user.getUsername();
                }
                temp = temp.next;
            }
            return userList;
        }
    }


    public static void main(String [] args) {
        ChatServer myServer = new ChatServer(12345, 20);
        myServer.startup();
    }
}
