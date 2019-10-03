import java.io.*;
/*
 * This class defines the different type of messages that will be exchanged between the
 * Clients and the Server. 
 */
public class UserMessage implements Serializable {

	private static final long serialVersionUID = 77L;

	// The different types of message sent by the Client
	// CURRENTUSERS to receive the list of the users connected
	
	// BID an object that contains the amount being bid
	// LOGOUT to disconnect from the Server
	static final int CURRENTUSERS = 0, BID = 1,LOGOUT = 2;
	private int type;
	private String msg;
	
	// constructor
	UserMessage(int type, String msg) {
		this.type = type;
		this.msg = msg;
	}
	
	// getters
	int getType() {
		return type;
	}
	String getMessage() {
		return msg;
	}
}
