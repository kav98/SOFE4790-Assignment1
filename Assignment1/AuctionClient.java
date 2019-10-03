import java.io.*;
import java.net.*;
import java.util.*;
 
public class AuctionClient {
	
	//opening object I/O streams
	private ObjectInputStream in; //read from socket (from server)
	private ObjectOutputStream out; //write to socket (send to server)
	private Socket socket;
	
	//Client GUI
	private ClientGUI cGUI;
	
		// the server, the port and the username
	private String server;
	private String username;
	private int port;

	/*
	 *  Constructor called by console mode
	 *  server: the server address
	 *  port: the port number
	 *  username: the username
	 */
	public AuctionClient(String server, int port, String username) {
		// which calls the common constructor with the GUI set to null
		this(server, port, username, null);
	}

	/*
	 * Constructor call when used from a GUI
	 * in console mode the ClienGUI parameter is null
	 */
	public AuctionClient(String server, int port, String username, ClientGUI cGUI) {
		this.server = server;
		this.port = port;
		this.username = username;
		// save if we are in GUI mode or not
		this.cGUI = cGUI;
	}
	
	/*
	 * To start the dialog
	 */
	public boolean start() {
		// try to connect to the server
		try {
			socket = new Socket(server, port);
		} 
		// if it failed not much I can so
		catch(Exception ec) {
			GUImsg("Error connectiong to server:" + ec);
			return false;
		}
		
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		GUImsg(msg);
		GUImsg("This is a silent auction. Therefore the winner will not be announced until all users have placed their bids.");
		GUImsg("The item we are auctioning off is an antique dining set made of mahoghany wood and decorated with brass fixtures. Bidding will begin at $450.");
	
		/* Creating both Data Stream */
		try
		{
			in  = new ObjectInputStream(socket.getInputStream());
			out = new ObjectOutputStream(socket.getOutputStream());
		}
		catch (IOException eIO) {
			GUImsg("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// creates the Thread to listen from the server 
		new ListenFromServer().start();
		// Send our username to the server this is the only message that we
		// will send as a String. All other messages will be UserMessage objects
		try
		{
			out.writeObject(username);
		}
		catch (IOException eIO) {
			GUImsg("Exception doing login : " + eIO);
			disconnect();
			return false;
		}
		// success we inform the caller that it worked
		return true;
	}
	//method to streamline sending message through GUI
	private void GUImsg(String msg) {
		cGUI.append(msg + "\n");		// display message to client
	}
	//talk to server
	void Smsg(UserMessage msg) {	//message for the server
		try {
			out.writeObject(msg);
		}
		catch(IOException e) {
			GUImsg("Exception writing to server: " + e);
		}
	}
	
	

	private void disconnect() {
		try { 
			if(in != null) in.close();
		}
		catch(Exception e) {} // not much else I can do
		try {
			if(out != null) out.close();
		}
		catch(Exception e) {} // not much else I can do
        try{
			if(socket != null) socket.close();
		}
		catch(Exception e) {} // not much else I can do
		
		// inform the GUI
		if(cGUI != null)
			cGUI.connectionFailed();
			
	}
    public static void main(String[] args) {
		
		//in case no values are given, these are the defaults
		int portNum = 7777;
		String hostname = "localHost";
		String user = "Anonymous";
		/*
		if (args.length == 0){
						
			
		}else if (args.length == 1){
			
		}else if (args.length == 2){
		
			
        if (args.length != 2) {
            System.err.println(
                "Usage: java AuctionClient <host name> <port number>");
            System.exit(1);
        }
 
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
		*/
		// depending of the number of arguments provided we fall through
		switch(args.length) {
			// > javac AuctionClient username portNumber serverAddr
			case 3:
				hostname = args[2];
			// > javac AuctionClient username portNumber
			case 2:
				try {
					portNum = Integer.parseInt(args[1]);
				}
				catch(Exception e) {
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
					return;
				}
			// > javac AuctionClient username
			case 1: 
				user = args[0];
			// > java AuctionClient
			case 0:
				break;
			// invalid number of arguments
			default:
				System.out.println("Usage is: > java Client [username] [portNumber] {serverAddress]");
			return;
		}
 /*
        try {
            Socket auctionSocket = new Socket(hostName, portNumber);
            PrintWriter out =
                new PrintWriter(auctionSocket.getOutputStream(), true);
            BufferedReader in =
                new BufferedReader(
                    new InputStreamReader(auctionSocket.getInputStream()));
            BufferedReader stdIn =
                new BufferedReader(
                    new InputStreamReader(System.in));
        
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
                System.out.println("Auction Server Sent: " + in.readLine());
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                hostName);
            System.exit(1);
        } 
    }
}
*/

		AuctionClient client = new AuctionClient(hostname, portNum, user);
		// test if we can start the connection to the Server
		// if it failed nothing we can do
		if(!client.start())
			return;
		
		// wait for messages from user
		Scanner scan = new Scanner(System.in);
		// loop forever for message from the user
		while(true) {
			System.out.print("> ");
			// read message from user
			String msg = scan.nextLine();
			// logout if message is LOGOUT
			if(msg.equalsIgnoreCase("LOGOUT")) {
				client.Smsg(new UserMessage(UserMessage.LOGOUT, ""));
				// break to do the disconnect
				break;
			}
			// message CurrentUsers
			else if(msg.equalsIgnoreCase("CURRENTUSERS")) {
				client.Smsg(new UserMessage(UserMessage.CURRENTUSERS, ""));				
			}
			else {				// send a bid
				client.Smsg(new UserMessage(UserMessage.BID, msg));
			}
		}
		// done disconnect
		client.disconnect();	
	}
	class ListenFromServer extends Thread {

		public void run() {
			while(true) {
				try {
					String msg = (String) in.readObject();
					// if console mode print the message and add back the prompt
					if(cGUI == null) {
						System.out.println(msg);
						System.out.print("> ");
					}
					else {
						cGUI.append(msg);
					}
				}
				catch(IOException e) {
					GUImsg("Server has close the connection: " + e);
					if(cGUI != null) 
						cGUI.connectionFailed();
					break;
				}
				// can't happen with a String object but need the catch anyhow
				catch(ClassNotFoundException e2) {
				}
			}
		}
	}
}