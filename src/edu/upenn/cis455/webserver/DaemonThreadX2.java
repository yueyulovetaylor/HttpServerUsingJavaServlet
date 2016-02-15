package edu.upenn.cis455.webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import javax.servlet.http.HttpServlet;

import edu.upenn.cis455.ServletUtility.FakeContext;
import edu.upenn.cis455.ServletUtility.TestHarness;
import edu.upenn.cis455.ServletUtility.TestHarness.Handler;

/*
 * Daemon Thread is the class deals with information (Port Number and 
 * directory) parsed in from HttpServer.main(args[])
 */

public class DaemonThreadX2 extends Thread {
	public static int portNumber;
	
	String directory;
	ServerSocket server;
	SingleThreadHandlerX2 [] threadPoolList;	
	final int threadPoolSize = 1;
	final int socketQueueSize = 1;
	Queue<Socket> socketQueue;
	boolean isDaemonRunning;
	
	// Updated in Milestone 2
	String webXMLDirec;
	Handler curHandler;
	HashMap<String,HttpServlet> curServelets;
	SessionContainer curServletSessionContainer;
	FakeContext curServletContext;
		
	public DaemonThreadX2(int inputPortNumber, String inputDirectory, String inputXMLDirec) throws Exception {
		System.out.println("-- DeamonThread.DaemonThread() " + inputPortNumber + " Entering");
		
		// Initiation the server and directory as what is parsed in
		this.portNumber = inputPortNumber;
		
		this.directory = inputDirectory;
		try {
			server = new ServerSocket(inputPortNumber);
		} 
		catch (IOException e) {
			System.out.println("-- DeamonThread.DaemonThread(): Cannot initiate server with current port number");
			e.printStackTrace();
		}
		
		threadPoolList = new SingleThreadHandlerX2[threadPoolSize];
		socketQueue = new LinkedList<Socket>();
		isDaemonRunning = true;
		
		// Initialize ServletContext, Servlet, ServletSession
		// Add web.XML directory here and parse it to the new curHandler object
		this.webXMLDirec = inputXMLDirec;
		try {
			System.out.println("-- DeamonThread.DaemonThread(): to initiate handler");
			curHandler =  TestHarness.parseWebdotxml(this.webXMLDirec);
		} 
		
		catch (Exception e) {
			System.out.println("-- DeamonThread.DaemonThread(): Cannot parse web.xml file! Exiting with exception");
			e.printStackTrace();
			throw e;
		}
		
		this.curServletContext = TestHarness.createContext(curHandler);
		this.curServelets = TestHarness.createServlets(curHandler, curServletContext);
		
		this.curServletSessionContainer = new SessionContainer();
		SessionContainer.SessionID = 0;				// Reset sessionID when create daemon thread
		
		System.out.println("-- DeamonThread.DaemonThread()" + inputPortNumber + " Exiting");
	}
	
	public void run() {
		System.out.println("-- DeamonThread.run() Entering");
		
		// The first thing to do is to build up the thread pool
		for (int i = 0; i < threadPoolSize; i++) {
			threadPoolList[i] = new SingleThreadHandlerX2(this.directory, 
					this.socketQueue, 
					this, 
					this.threadPoolList, 
					this.webXMLDirec,
					this.curHandler,
					this.curServelets,
					this.curServletSessionContainer,
					this.curServletContext,
					i);
			threadPoolList[i].start();
		}
		
		// If can accept current request, push the accepted socket into the queue
		// This part should be put in a while, since it should be continuously taking in thread
		while (isDaemonRunning) {
			System.out.println("-- DeamonThread.run() isDaemonRunning " + this.isDaemonRunning);
			try {
				acceptReqAndPushToQueue();
			} catch (InterruptedException | IOException e1) {
				System.out.println("-- DeamonThread.run() isDaemonRunning: Cannot push into queue");
				e1.printStackTrace();
				System.out.println("-- DeamonThread.run() isDaemonRunning(L61): " + this.isDaemonRunning);
			}
		}
		
		for (int i = 0; i < threadPoolSize; i++) {
			try {
				// Wait for all threads completing their execution
				threadPoolList[i].join();
			} catch (InterruptedException e) {
				System.out.println("Thread " + i + " cannot join.");
				e.printStackTrace();
			}
		}
		
		System.out.println("-- DeamonThread.run() Exiting");
	}
	
	private void acceptReqAndPushToQueue() throws InterruptedException, IOException {
		// Just print something if reaching here for testing purpose
		System.out.println("-- DeamonThread.acceptReqAndPushToQueue() Entering");
		
		if (!isDaemonRunning) return;
		while (socketQueue.size() == socketQueueSize && this.isDaemonRunning) {
			// We will have to wait over here, just to be notified if 
			// queue has already deal with the very first 
			System.out.println("-- DeamonThread.acceptReqAndPushToQueue() Into while block");
			synchronized (socketQueue) {
				// Here, we wait for the release from another place where 
				// a socket has been resolved and a spot in the queue 
				// is empty
				System.out.println("-- DeamonThread.acceptReqAndPushToQueue() Start waiting");
				socketQueue.wait();
			}
		}
		
		System.out.println("-- DeamonThread.acceptReqAndPushToQueue() After waiting");
		Socket curSocket = null;
		try {
			System.out.println("-- DeamonThread.acceptReqAndPushToQueue() Initiate socket");
			System.out.println(server.toString());
			curSocket = server.accept();
			System.out.println("-- DeamonThread.acceptReqAndPushToQueue() Initiation successful");
		} catch (IOException e) {
			System.out.println("-- DeamonThread.acceptReqAndPushToQueue() Socket cannot be accepted");
			throw e;
		}
		System.out.println("-- DeamonThread.acceptReqAndPushToQueue() After socket initiation");
		
		// Wait again for socketQueue to get synchronized, and push curSocket
		// into the queue
		synchronized (socketQueue) {
			System.out.println("-- DeamonThread.acceptReqAndPushToQueue() Socket add to Queue");
			socketQueue.add(curSocket);
			socketQueue.notifyAll();
			System.out.println("-- " + socketQueue.toString());
		}
		
		System.out.println("-- DeamonThread.acceptReqAndPushToQueue() Exiting");
	}
	
	public synchronized void shutDown() {
		System.out.println("-- DeamonThread.shutDown() Entering");
		// First, flip running flag
		this.isDaemonRunning = false;
		
		
		// Then, stop socket server from taking in new Socket
		try {
			System.out.println("-- DeamonThread.shutDown() Shutting down Socket Server");
			server.close();
		} catch (IOException e) {
			System.out.println("-- DeamonThread.shutDown() Socket Server cannot be closed");
			e.printStackTrace();
		}
		
		// Finally, we kill every thread, just set the running inside SingleThreadHandler as false
		for (int i = 0; i < this.threadPoolSize; i++) {
			this.threadPoolList[i].closeCurrentThread();
		}
		
		// Notify all queue stop waiting 
		synchronized (socketQueue) {
			this.socketQueue.notifyAll();
		}
		System.out.println("-- DeamonThread.shutDown() Exiting");
	}
}
