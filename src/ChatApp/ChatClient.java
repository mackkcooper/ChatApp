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

public class ChatClient extends JFrame implements ActionListener {
    //MEMBERS
    private String username = null;
    private String ip = "localhost";
    private int port = 12345;
    private Socket cli;
    private boolean loggedIn = false;
    private boolean shutdown = false;

    //GUI MEMBERS
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private JTextArea userDisplay;
    private JTextArea display;
    private JTextField text;
    private LoginMenu loginMenu;


    //METHODS
    //starts up the client program
    private void startup() {
        try {
            cli = new Socket(ip, port);
            out = new ObjectOutputStream(cli.getOutputStream());
            in = new ObjectInputStream(cli.getInputStream());
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        chatGUISetup();
        runClient();
    }

    private void runClient() {
        while(!shutdown) {
            try {
                Command command = (Command) in.readObject();
                commandHandler(command);
            } catch (Exception e) {
                if(e instanceof EOFException)
                    shutdown();
                else
                    e.printStackTrace();
            }
        }
        //SHUTDOWN PORTION
        try {
            in.close();
            out.close();
            cli.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private void openChat() {
        loginMenu.closeLogin();
        setTitle("Chat Client - Chat Room - " + username);
        display.setText("System: Welcome to Mack's Chat!");
        setVisible(true);
    }

    private void closeChat() {
        display.setText("");
        setVisible(false);
        loggedIn = false;
        username = null;
        loginMenu.openLogin();
    }

    private void logout() {
        Command command = new Command();
        command.logoutRequest(username);
        sendCommand(command);
    }

    private void shutdown() {
        shutdown = true;
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

    private void chatGUISetup() {
        setSize(689, 470);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosed(e);
                logout();
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

    //sends user input from text field in command to server
    public void actionPerformed(ActionEvent e) {
        String input = text.getText();
        if(!input.equals("")) {
            text.setText("");
            Command command = new Command();
            if(input.charAt(0) == '@') {
                String dest = parseMessage(input);
                if(dest.equals(username)) {
                    displayToChat("System: Cannot send direct message to yourself.");
                    return;
                } else if(dest.equals("")) {
                    displayToChat("System: Could not determine user to send message to.");
                    return;
                }
                command.messageUser(username, input, dest);
                sendCommand(command);
            } else {
                command.message(username, input);
                sendCommand(command);
            }
        }
    }

    //interprets message that is being sent to only certain users
    private String parseMessage(String msg) {
        if(msg == null)
            return null;
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

    //posts given String with line break appended to chat window
    private void displayToChat(String msg) {
        display.append("\n" + msg);
        display.setCaretPosition(display.getDocument().getLength());
    }

    private void commandHandler(Command c) {
        if(c != null) {
            String commandType = c.command;
            switch (commandType) {
                case "register success": //server successfully registered user
                    if (!loggedIn)
                        loginMenu.displayFeedback(c.username + " successfully registered.");
                    break;

                case "login success": //server successfully logged in user
                    if (!loggedIn) {
                        loginMenu.displayFeedback(c.username + " successfully logged in.");
                        username = c.username;
                        loginMenu.closeLogin();
                        loggedIn = true;
                        openChat();
                    }
                    break;

                case "logout success":
                    if (loggedIn) {
                        if (c.username.equals(username)) {
                            closeChat();
                        }
                    }
                    break;

                case "message":
                    if (loggedIn)
                        displayToChat(c.username + ": " + c.message);
                    break;

                case "online change":
                    if (loggedIn) {
                        displayToChat("System: " + c.message);
                        userDisplay.setText(c.userList);
                    }
                    break;

                case "error":
                    if (!loggedIn)
                        loginMenu.displayFeedback(c.message);
                    break;

                case "close":
                    shutdown();
                    break;

                default:
                    if (loggedIn)
                        displayToChat("System: Unknown command type - " + c.command);
                    else
                        loginMenu.displayFeedback("Unknown command type - " + c.command);
            }
        }
    }

    //CONSTRUCTORS
    private ChatClient(String ipAddress, int port) {
        super("Chat Client - Chat Room");
        ip = ipAddress;
        this.port = port;
        loginMenu = new LoginMenu();
        loginMenu.openLogin();
    }


    //LOGIN MENU
    private class LoginMenu extends JFrame implements ActionListener {
        //MEMBERS
        JTextArea feedback;
        JTextField usernameField;
        JPasswordField passwordField;
        JButton loginButton;
        JButton registerButton;
        JButton clearButton;


        //METHODS
        //catches actionEvents generated by user interacting with GUI
        public void actionPerformed(ActionEvent e) {
            if(e.getSource() == usernameField || e.getSource() == passwordField || e.getSource() == loginButton) {
                String usernameText = usernameField.getText();
                String passwordText = new String(passwordField.getPassword());
                if(!usernameText.equals("") && !passwordText.equals(""))
                    login(usernameText, passwordText);
            } else if(e.getSource() == registerButton) {
                String usernameText = usernameField.getText();
                String passwordText = new String(passwordField.getPassword());
                if(!usernameText.equals("") && !passwordText.equals(""))
                    register(usernameText, passwordText);
            } else if(e.getSource() == clearButton) {
                clearFields();
            }
        }

        //clears input fields for login window
        private void clearFields() {
            usernameField.setText("");
            passwordField.setText("");
        }

        //displays feedback to the user in login display
        private void displayFeedback(String msg) {
            feedback.append("\n" + msg);
            feedback.setCaretPosition(feedback.getDocument().getLength());
        }

        //sends register request to server w/ given username and password
        private void register(String u, String p) {
            Command command = new Command();
            command.registerRequest(u, p);
            displayFeedback("Attempting to register...");
            sendCommand(command);
        }

        //sends login request to server w/ given username and password
        private void login(String u, String p) {
            Command command = new Command();
            command.loginRequest(u, p);
            displayFeedback("Attempting to sign in...");
            sendCommand(command);
        }

        //sets up the GUI as part of constructor
        private void loginGUISetup() {
            setSize(500,200);
            setResizable(false);
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    super.windowClosed(e);
                    Command command = new Command();
                    command.close();
                    sendCommand(command);
                    shutdown();
                }
            });

            JPanel panel = new JPanel();
            add(panel);

            feedback = new JTextArea(3,40);
            feedback.setLineWrap(true);
            feedback.setEditable(false);
            JScrollPane scroll = new JScrollPane(feedback);
            panel.add(scroll);

            JLabel usernameLabel = new JLabel("Username");
            panel.add(usernameLabel);

            usernameField = new JTextField();
            usernameField.setColumns(33);
            usernameField.addActionListener(this);
            panel.add(usernameField);

            JLabel passwordLabel = new JLabel("Password");
            panel.add(passwordLabel);

            passwordField = new JPasswordField();
            passwordField.setColumns(33);
            passwordField.addActionListener(this);
            panel.add(passwordField);

            loginButton = new JButton("Login");
            loginButton.addActionListener(this);
            panel.add(loginButton);


            registerButton = new JButton("Register");
            registerButton.addActionListener(this);
            panel.add(registerButton);

            clearButton = new JButton("Clear");
            clearButton.addActionListener(this);
            panel.add(clearButton);
        }

        //opens and makes visible the login window
        private void openLogin() {
            feedback.setText("Welcome to Mack's Chat Application!");
            clearFields();
            setVisible(true);
        }

        //closes and makes invisible the login window
        private void closeLogin() {
            usernameField.setText("");
            passwordField.setText("");
            setVisible(false);
        }

        //CONSTRUCTOR
        LoginMenu() {
            super("Chat Client - Login/Register");
            loginGUISetup();
            displayFeedback("Successfully connected to server!\nIP: " + ip + "\nPort: " + port);
        }
    }


    public static void main(String [] args) {
        ChatClient myClient = new ChatClient("localhost", 12345);
        myClient.startup();
    }
}
