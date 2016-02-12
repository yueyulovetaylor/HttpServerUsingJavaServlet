package edu.upenn.cis455.webserver;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Queue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import edu.upenn.cis455.ServletUtility.FakeContext;
import edu.upenn.cis455.ServletUtility.TestHarness.Handler;



/*
 * This class is just a transition class, it represents for one single thread,
 * while do nothing with dealing with the thread, i.e. deal with the http request
 * parsed into the socket
 */

public class SingleThreadHandlerX2 extends Thread {

	String directory;
	Queue<Socket> socketQueue;
	int threadNumber;			// Will be deleted in the end
	DaemonThreadX2 daemonThread;
	boolean isRunning;
	Socket toParse;
	SingleThreadHandlerX2[] curTPList;
	ResponseHandlerX2 curRH;
	
	// Milestone 2 updates
	String curWebXMLDirec;
	Handler curHandler;
	HashMap<String,HttpServlet> curServelets;
	SessionContainer curServletSessionContainer;
	FakeContext curServletContext;
	
	public SingleThreadHandlerX2(String inputDirectory, 
							   Queue<Socket> inputSocketQueue,
							   DaemonThreadX2 inputDaemonThread,
							   SingleThreadHandlerX2[] inputThreadPoolList,
							   
							   // Update inputs in Milestone 2
							   String inputWebXMLDirec,
							   Handler inputHandler,
							   HashMap<String,HttpServlet> inputServelets,
							   SessionContainer inputServletSessionContainer,
							   FakeContext inputServletContext,
							   // Update ends here
							   
							   int inputThreadNumber) {
		this.threadNumber = inputThreadNumber;
		System.out.println("--- SingleThreadHandler:SingleThreadHandler() Entering: thread No." + this.threadNumber);
		
		this.directory = inputDirectory;
		this.socketQueue = inputSocketQueue;
		this.daemonThread = inputDaemonThread;
		this.curTPList = inputThreadPoolList;
		this.isRunning = true;
		
		// Update in Milestone 2
		this.curWebXMLDirec = inputWebXMLDirec;
		this.curHandler = inputHandler;
		this.curServelets = inputServelets;
		this.curServletSessionContainer = inputServletSessionContainer;
		this.curServletContext = inputServletContext;
		
		System.out.println("--- SingleThreadHandler:SingleThreadHandler() Exiting: thread No." + this.threadNumber);
	}
	
	public void run() {
		System.out.println("--- SingleThreadHandler:run() Entering: thread No." + this.threadNumber);	
		
		while (isRunning) {
			try {
				// Get one socket from the queue and parse into the lower level classes
				toParse = ReadSocketFromQueue();
				if (toParse == null) {
					System.out.println("--- SingleThreadHandler:run() Error: No Socket read from the queue thread No." + this.threadNumber);	
					return;
				}
				
				// We start to deal with the socket at this place 
/*				this.curRH = new ResponseHandler(this.threadNumber, 
						toParse, this.curTPList, this.directory, this.daemonThread);
				this.curRH.processHttpRequest();
				
				
				if (this.curRH.getIfToShutDown()) {
					this.daemonThread.shutDown();
					System.out.println("--- SingleThreadHandler:run() isRunning: " + this.isRunning);
					return;
				}*/
				
				// In Mile Stone 2, we use a new ResponseHandlerX2 object
				this.curRH = new ResponseHandlerX2(
						this.threadNumber,
						this.toParse,
						this.curTPList,
						this.directory,
						this.daemonThread,
						this.curWebXMLDirec,
						this.curHandler,
						this.curServelets,
						this.curServletSessionContainer,
						this.curServletContext);
				this.curRH.processHttpRequest();
				
			} 
			
			catch (InterruptedException | IOException e) {
				System.out.println("--- SingleThreadHandler:run() Error1: ReadSocketFromQueue Error thread No." + this.threadNumber);
				e.printStackTrace();
				return;
			} 
			
			catch (ServletException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
/*			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			
			finally {
				if (toParse != null) {
					try {
						toParse.close();
					} catch (IOException e) {
						System.out.println("--- SingleThreadHandler:run() Error: Unable to close socket thread No." + this.threadNumber);
						e.printStackTrace();
					}
				}
			}
		}
		
	}
	
	public Socket ReadSocketFromQueue() throws InterruptedException {
		synchronized (socketQueue) {
			System.out.println("--- SingleThreadHandler:ReadSocketFromQueue() Entering");
			// Synchronized socketQueue first before pop anything out of it
			System.out.println("--- SingleThreadHandler:ReadSocketFromQueue() thread No.:" + this.threadNumber + " Queue empty?" + socketQueue.isEmpty());
			while (socketQueue.isEmpty() && this.isRunning) {
				System.out.println(socketQueue.toString());
				if (!this.isRunning) return null;
				// Just wait until something get into the socket
				System.out.println("--- SingleThreadHandler:ReadSocketFromQueue() Queue waiting Thread No." + this.threadNumber);
				socketQueue.wait();	
			}
			
			// When we had something in the queue, don't forget to notify all 
			// before return the retrieved socket
			Socket toReturn = socketQueue.poll();
			
			System.out.println("--- SingleThreadHandler:ReadSocketFromQueue() Exiting");
			socketQueue.notifyAll();
			return toReturn;	
		}
	}
	
	public void closeCurrentThread() {
		this.isRunning = false;
	}
}
