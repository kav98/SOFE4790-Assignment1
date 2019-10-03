import java.net.*;
import java.io.*;
import java.util.*;
 
public class AuctionServer {

	//Unique ID for each client
	private static int UID;
	//an array list of current connected clients
	private ArrayList<ClientThread> currUsers;
	//list of bids
	private ArrayList<String> bidList;
	//Current state of the auction [boolean]
	private boolean auctionState;
	//port number
	private int port;
	//GUI
	private ServerGUI sGUI;
	private int itemPrice = 450;
	private String topBid;
	private String topUser;
	private boolean hasBid;
         
		 
	public AuctionServer(int port){
		//calls server with null GUI
		this(port, null);
	}
	
	public AuctionServer(int port, ServerGUI sGUI) {
		//set GUI
		this.sGUI = sGUI;
		//port, resolves to 7777 is none provided
		this.port = port;
		//Array list to track current users
		currUsers = new ArrayList<ClientThread>();
	}

//handles connections and server start stop
	public void start(){
		auctionState = true;//auction is ongoing
		
		//creating server socket and waiting for connections
		try{
		ServerSocket serverSocket = new ServerSocket(port);
		
		
		while(auctionState == true){
			
			
			
			Socket socket = serverSocket.accept();
			
			//when auction ends
			if(auctionState == false){
				break;
			}
			
			ClientThread ct = new ClientThread(socket);
			currUsers.add(ct);
			
			ct.start();
		}
		//if asked to stop, this loop will close the auction and log out each client
		
			serverSocket.close();
			for(int i = 0; i < currUsers.size(); i++){
				ClientThread t = currUsers.get(i);
				try{
					t.in.close();
					
				}catch(IOException e){
					System.out.print("I/O ERROR");
				}
				try{
					t.out.close();
					
				}catch(IOException e){
					System.out.print("I/O ERROR");
				}
				try{
					
					t.cSock.close();
				}catch(IOException e){
					System.out.print("I/O ERROR");
				}
				
			}
		}catch(Exception e) {
           System.out.println("Exception: "+e);
        }
			
		}
		
	
	
	protected void stop() {
		auctionState = false;
		// connect to myself as Client to exit statement 
		// Socket socket = serverSocket.accept();
		try {
			new Socket("localhost", port);
		}
		catch(Exception e) {
			// nothing I can really do
		}
	}
	
	
	
		
/*
	 * Display an event (not a message) to the console or the GUI
	 */
	public void display(String msg) {
		
		if(sGUI == null)
			System.out.println(msg);
		else
			sGUI.appendEvent(msg + "\n");
	}
	/*
	 *  to broadcast a message to all Clients
	 */
	private synchronized void broadcast(String msg) {
		
		String message = msg+ "\n";
		// display message on console or GUI
		if(sGUI == null)
			System.out.print(message);
		else
			sGUI.appendRoom(message);     // append in the room window
		
		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for(int i = currUsers.size(); --i >= 0;) {
			ClientThread ct = currUsers.get(i);
			// try to write to the Client if it fails remove it from the list
			if(!ct.writeMsg(message)) {
				currUsers.remove(i);
				display("Disconnected Client " + ct.username + " removed from list.");
			}
		}
	}

	// for a client who logoff using the LOGOUT message
	synchronized void remove(int id) {
		// scan the array list until we found the Id
		for(int i = 0; i < currUsers.size(); ++i) {
			ClientThread ct = currUsers.get(i);
			// found it
			if(ct.clientID == id) {
				currUsers.remove(i);
				return;
			}
		}
	}		
    public static void main(String[] args) {
		
		int port = 7777;
       switch(args.length) {
			case 1:
				try {
					port = Integer.parseInt(args[0]);
				}
				catch(Exception e) {
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Server [portNumber]");
					return;
				}
			case 0:
				break;
			default:
				System.out.println("Usage is: > java Server [portNumber]");
				return;
				
		}
        AuctionServer auc = new AuctionServer(port);
        auc.start();
     }

         
  


class ClientThread extends Thread {
    Socket cSock;
    ObjectOutputStream out;
    ObjectInputStream in;
	int clientID;//unique id for each client
	String username; // client username
	UserMessage userMsg; 
	
    public ClientThread(Socket cSock) { // constructor
       this.cSock = cSock;
	   clientID = ++UID;
	
	//attempting to create data streams
		try
			{
				// create output first
				
				out = new ObjectOutputStream(cSock.getOutputStream());
				in  = new ObjectInputStream(cSock.getInputStream());
				
				// read the username
				username = (String) in.readObject();
				
				display(username + " just connected.");
			
				
			}
			catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
				catch (ClassNotFoundException e) {
				display("Class Not Found Exception: " + e);
			}
           
		}
/*
       try {
           out = new PrintWriter(client.getOutputStream(), true);                   
           in = new BufferedReader(
                new InputStreamReader(client.getInputStream()));
       } catch (IOException e) {
           try {
             client.close();
           } catch (IOException ex) {
             System.out.println("Error while getting socket streams.."+ex);
           }
           return;
       }
        this.start(); // Thread starts here...this start() will call run()
    }
 */
    public void run() {
		
      try {

         boolean auctionState = true;
		
         while (auctionState) {
                // read a String (which is an object)
				try {
					userMsg = (UserMessage) in.readObject();
				}
				catch (IOException e) {
					display(username + " Exception reading Streams: " + e);
					break;				
				}
				catch(ClassNotFoundException e2) {
					break;
				}
				// the messaage part of the UserMessage
				String message = userMsg.getMessage();

				// Switch on the type of message receive
				switch(userMsg.getType()) {

				case UserMessage.BID:
					bid(username, message);
					break;
				case UserMessage.LOGOUT:
					display(username + " disconnected with a LOGOUT message.");
					auctionState = false;
					break;
				case UserMessage.CURRENTUSERS:
					writeMsg("List of the users connected" + "\n");
					// scan currUsers the users connected
					for(int i = 0; i < currUsers.size(); ++i) {
						ClientThread ct = currUsers.get(i);
						writeMsg((i+1) + ") " + ct.username);
					}
					break;
				}
         }
         remove(clientID);
		try{
			in.close();
		}catch(IOException e){
		System.out.print("I/O ERROR");
		}
		try{
			out.close();
		
		}catch(IOException e){
		System.out.print("I/O ERROR");
		}
		try{
				
			cSock.close();
		}catch(IOException e){
		System.out.print("I/O ERROR");
		}
	  
       } catch(Exception e) {
           System.out.println("Exception: "+e);
        }
	   }
		public boolean writeMsg(String msg) {
			// if Client is still connected send the message to it
			if(!cSock.isConnected()) {
				try{
			in.close();
		}catch(IOException e){
		System.out.print("I/O ERROR");
		}
		try{
			out.close();
		
		}catch(IOException e){
		System.out.print("I/O ERROR");
		}
		try{
				
			cSock.close();
		}catch(IOException e){
		System.out.print("I/O ERROR");
		}
				return false;
			}
			// write the message to the stream
			try {
				out.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch(IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		 
    }
	public void bid(String u, String msg){
		int bid = 0;
		
		try{
			bid = Integer.parseInt(msg);
			
			
		
			if (bid >= itemPrice){
			display(u+ " has placed a bid for $"+ bid+"\n");
			writeMsg("You have placed a bid for $"+bid+"\n");
			hasBid = true;
			}else{
				writeMsg("Bid is below starting price.");
				hasBid = false;
			}
			if(hasBid){
				
				for(int i = 0;i<bidList.size()-1;i=i+2){
		if(bidList.get(i)==u){//make sure you aren't bidding twice
		writeMsg("You have already placed a bid for this item. This means your most recent bid will be ignored"+"\n");
		
		}
		else{
			bidList.add(i,u);//usernames in first index
		bidList.add(i+1,String.valueOf(bid));//bid amount in second
		//display(Arrays.toString(bidList));
			break;
		}
				}
			}
			if (bidList.size() == currUsers.size()*2){
			for(int i = 0; i < bidList.size();i++)
		{
			if(Integer.valueOf(bidList.get(i)) > Integer.valueOf(topBid))
			{
				topBid = bidList.get(i);
				topUser = bidList.get(i+1);
				
			}
			
		}
			
			broadcast("User "+topUser+"has won the auction with a bid of "+topBid);
				auctionState = false;
			}
			
		}catch(NumberFormatException ex){
			writeMsg("Bid must be entered as an integer");
		}
		
		
		}
}
}
