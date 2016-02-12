package edu.upenn.cis455.webserver;

import java.util.HashMap;

import edu.upenn.cis455.ServletUtility.FakeSession;

/*
 * Since they are multiple sessions saved in the server, so we use a class
 * to store multiple sessions into the sessionContainer
 */
public class SessionContainer {
	
	static int SessionID;
	public HashMap<Integer, FakeSession> SessionMap;
	
	public SessionContainer() {
		SessionMap = new HashMap<Integer, FakeSession>();
	}
	
	public FakeSession startNewSeesion() {
		this.SessionID++;
		FakeSession curFS = new FakeSession(this.SessionID);
		SessionMap.put(this.SessionID, curFS);
		System.out.println("-- SessionContainer.startNewSeesion(): New Session: " + this.SessionID + " Added");
		return curFS;
	}
}
