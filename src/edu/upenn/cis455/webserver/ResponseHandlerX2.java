package edu.upenn.cis455.webserver;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import edu.upenn.cis455.SampleServlet.BusyServlet;
import edu.upenn.cis455.ServletUtility.FakeContext;
import edu.upenn.cis455.ServletUtility.FakeRequest;
import edu.upenn.cis455.ServletUtility.FakeResponse;
import edu.upenn.cis455.ServletUtility.TestHarness.Handler;



/*
 * This is the class dealing with the responses, in milestone 2, besides taking
 * in the same object as Milestone 1, we would parse additional servlet objects
 * (servlet container, session container, handler, XML directory and context) 
 * into the project.
 */

public class ResponseHandlerX2 {

	int threadNumber;
	Socket curSocket;
	SingleThreadHandlerX2[] curThreadPoolList;
	String rootDirectory;
	DaemonThreadX2 curDT;
	DataOutputStream dataOutStream;
	OutputStream outStream;
	
	boolean isHost;
	boolean isIfModified;
	boolean isIfUnmodified;
	String sInputModifiedDate;
	long lInputModifiedDate;   		// Representing the modified or unmodified date
	boolean toShutDown;				// Whether to shut down the server
	
	// Milestone 2 additional object
	String curWebXMLDirec;
	Handler curHandler;
	HashMap<String,HttpServlet> curServelets;
	SessionContainer curServletSessionContainer;
	FakeContext curServletContext;
	
	// Additional utility objects
	BufferedReader in;				// Input stream from client side
	String curURL;
	String httpVersion;
	float httpVersionNo;
	
	// Time utility objects
	final String DateTimeFormat = "EEE, d MMM yyyy HH:mm:ss Z";
	final SimpleDateFormat sdfFormat = new SimpleDateFormat(DateTimeFormat);		
	Calendar cal = Calendar.getInstance();
	String curTime;
	
	// We still need these objects for quick check return (not "GET" and "POST" situation)
	String statusCodePhrase;
	String contentType;
	int contentLength;
	
	public ResponseHandlerX2(int inputThreadNumber,
					       Socket inputSocket,
					       SingleThreadHandlerX2[] inputThreadPoolList,
					       String inputRootDirectory,
					       DaemonThreadX2 inputDT,
					       
					       // Update inputs in Milestone 2
						   String inputWebXMLDirec,
						   Handler inputHandler,
						   HashMap<String,HttpServlet> inputServelets,
						   SessionContainer inputServletSessionContainer,
						   FakeContext inputServletContext
						   // Update ends here
					       ) throws IOException {
		this.threadNumber = inputThreadNumber;
//		System.out.println("---- ResponseHandler:ResponseHandler() Entering: Thread " + this.threadNumber);
		
		this.curSocket = inputSocket;
		this.curThreadPoolList = inputThreadPoolList;
		this.rootDirectory = inputRootDirectory;
		this.curDT = inputDT;
		
		this.in = new BufferedReader(
				  new InputStreamReader(curSocket.getInputStream()));
		this.outStream = curSocket.getOutputStream();
		this.dataOutStream = new DataOutputStream(outStream);
		
		// New object in Milestone 2
		this.curWebXMLDirec = inputWebXMLDirec;
		this.curHandler = inputHandler;
		this.curServelets = inputServelets;
		this.curServletSessionContainer = inputServletSessionContainer;
		this.curServletContext = inputServletContext;
		this.toShutDown = false;
		
		this.isHost = false;  		// Initialized as false, waiting for Host: to flip
		this.isIfModified = false;
		this.isIfUnmodified = false;
		
//		System.out.println("---- ResponseHandler:ResponseHandler() Entering: Thread " + this.threadNumber);
	}
	
	public void processHttpRequest() throws IOException, ServletException {
//		System.out.println("---- ResponseHandler:processHttpRequest() Entering: Thread " + this.threadNumber);
		
		// Get the Http Request from the client and split them into different parts: command,
		// URL, Http version
		String httpRequest = in.readLine();
		String[] httpRequestSplits = new String[3];
		
		try {
			httpRequestSplits = httpRequest.split("\\s+");
		}
		
		catch (Exception e) {
			return;
		}
		
		// Analyze the splits
		String command = httpRequestSplits[0].trim();
		this.curURL = httpRequestSplits[1].trim();
		this.httpVersion = httpRequestSplits[2].trim();
		this.httpVersionNo = Float.parseFloat(httpVersion.substring(5));
		
		// Still, in MS2, we need to hard check whether method is "GET" or "POST"
		if (!command.equals("GET") && !command.equals("POST")) {
			// Our Http server only needs to support GET and HEAD methods, all others are illegal
			// It is a quick return situation, illegal method, 
			// Put this string on the webpage will be fine "HTTP/1.x 501 Not Implemented"
//			System.out.println("---- ResponseHandler:processHttpRequest() Illegal method name: Thread " + this.threadNumber);
			this.statusCodePhrase = "501 NOT IMPLEMENTED";
			
			String headerHTML = constructHtmlHeader(statusCodePhrase + ": UNDEFINED METHOD");
			
			this.contentType = "text/html";
			this.curTime = sdfFormat.format(cal.getTime());
			this.contentLength = headerHTML.length();
			
			String responseStr = constructResponseStr();
			
			dataOutStream.writeBytes(responseStr);
			dataOutStream.writeBytes(headerHTML);
			return;
		}
		
		// The format of the URL here can be simplified as: [path]?[query], 
		// so we first need to get rid of the query and obtain pureURL
		String curURLWithoutField = null;
		int idxOfQMark = this.curURL.indexOf('?');
		if (idxOfQMark == -1) {
			// Query not exists
			curURLWithoutField = this.curURL;
		}
		else {
			curURLWithoutField = this.curURL.substring(0, idxOfQMark);
		}
		
		// /* wildcard matching situation: reference: 
		// https://support.google.com/customsearch/answer/71826?hl=en
		boolean bWildcard = false;
		String URLIfWC = null;
		for (String url : this.curHandler.m_urlPatterns.keySet()) {
			if (url.endsWith("/*")) {
				// Potentially Wildcard matching
				String urlWithoutWC = url.substring(0, url.length() - 2);
				if (curURLWithoutField.equals(urlWithoutWC) || 
					curURLWithoutField.contains(urlWithoutWC + '/')) {
					// Matched
//					System.out.println("---- ResponseHandler:processHttpRequest() URL wildcard matching!");
					bWildcard = true;
					URLIfWC = urlWithoutWC + "/*";
				}
			}
		}
		
//		System.out.println("---- ResponseHandler:processHttpRequest() URL: " + curURLWithoutField);
		
		if (this.curHandler.m_urlPatterns.containsKey(curURLWithoutField) || bWildcard) {
//			System.out.println("---- ResponseHandler:processHttpRequest(): " +
//							   "handle request and response if found in servlet");
			String servletName = this.curHandler.m_urlPatterns.get(curURLWithoutField);
			String pathInfo = null;
			
			// URL if WC, get it's servlet name
			if (bWildcard && servletName == null) {
				servletName = this.curHandler.m_urlPatterns.get(URLIfWC);
				pathInfo = curURLWithoutField.substring(URLIfWC.length() - 2);
			}
			
//			System.out.println(servletName);
			
			// Get the Servlet and Request and Response class
//			System.out.println(this.curServelets.toString());
			
			FakeRequest toRequest = new FakeRequest(pathInfo, this.curSocket, this.in, 
									httpRequest, this.curServletSessionContainer);
			FakeResponse toResponse = new FakeResponse(this.dataOutStream, toRequest);
			
//			System.out.println(servletName);
//			System.out.println(this.curServelets.get(servletName));
			try {
				if (command.compareTo("GET") == 0 || command.compareTo("POST") == 0) {
					toRequest.setMethod(command);
				}
				this.curServelets.get(servletName).service(toRequest, toResponse);
				toResponse.flushBuffer();
			}
			
			catch (Exception e) {
//				System.out.println("---- ResponseHandler:processHttpRequest(): Cannot service");
				e.printStackTrace();
			}
			
			return;
		}
		
		// Start of Milestone 1
		// If Milestone 2 cannot handle Servlet issues, just use Milestone 1
		// Detect if there are another input here, (used for HTTP/1.1 or If-Modified-Since)
		String hostModified;
		while (!(hostModified = in.readLine()).equals("")) {
			// There is a second line indicating hosting
//			System.out.println("---- ResponseHandler:processHttpRequest() readin: " + hostModified);
			String[] hostModifiedSplits = hostModified.split("\\s+");
//			System.out.println("---- ResponseHandler:processHttpRequest() command: " + hostModifiedSplits[0]);
					
			if (hostModifiedSplits[0].trim().equals("Host:")) {
//				System.out.println("---- ResponseHandler:processHttpRequest() HOSTED");
				this.isHost = true;
			}
					
			else if (hostModifiedSplits[0].trim().equals("If-Modified-Since:")
				  || hostModifiedSplits[0].trim().equals("If-Unmodified-Since:")) {
				// Flip one of them
				if (hostModifiedSplits[0].trim().equals("If-Modified-Since:")) {
//					System.out.println("---- ResponseHandler:processHttpRequest() MODIFIED");
					this.isIfModified = true;
				}
						
				if (hostModifiedSplits[0].trim().equals("If-Unmodified-Since:")) {
//					System.out.println("---- ResponseHandler:processHttpRequest() UNMODIFIED");
					this.isIfUnmodified = true;
				}
						
				this.sInputModifiedDate = hostModified.substring(hostModifiedSplits[0].length() + 1);
										// Don't forget to skip the blank space
						
				try {
					this.lInputModifiedDate = sdfFormat.parse(this.sInputModifiedDate).getTime();
				} 
						
				catch (ParseException e) {
//					System.out.println("---- ResponseHandler:processHttpRequest() Illegal (un)modified time format: Thread " + this.threadNumber);
					this.statusCodePhrase = "500 INTERNAL SERVER ERROR";
							
					String headerHTML = constructHtmlHeader(this.statusCodePhrase + ": ILLEGAL (UN)MODIFIED TIME FORMAT");
							
					this.contentType = "text/html";
					this.curTime = sdfFormat.format(cal.getTime());
					this.contentLength = headerHTML.length();
							
					String responseStr = constructResponseStr();
							
					dataOutStream.writeBytes(responseStr);
					dataOutStream.writeBytes(headerHTML);
					return;
				}
			}
		}
		
		// If http version is 1.1, but there is no header parsed in; it's a bad request
		// Float.compare(f1, f2) return an integer, if positive, f1 > f2; if zero, f1 = f2; if negative, f1 < f2
		if (Float.compare(httpVersionNo, (float) 1.1) == 0 && !this.isHost) {
//			System.out.println("---- ResponseHandler:processHttpRequest() HTTP1.1 require Host: Thread " + this.threadNumber);
			this.statusCodePhrase = "400 BAD REQUEST";
					
			String headerHTML = constructHtmlHeader(this.statusCodePhrase + ": HTTP1.1 REQUIRE HOST");
					
			this.contentType = "text/html";
			this.curTime = sdfFormat.format(cal.getTime());
			this.contentLength = headerHTML.length();
					
			String responseStr = constructResponseStr();
					
			dataOutStream.writeBytes(responseStr);
			dataOutStream.writeBytes(headerHTML);
			return;
		}
		
		// If curURLWithoutField = "/shutdown"
		if (curURLWithoutField.equals("shutdown")) {
			this.toShutDown = true;
//			System.out.println("---- ResponseHandler:processHttpRequest(): to Shut Down server: Thread " + this.threadNumber);
			this.statusCodePhrase = "200 OK";
					
			String headerHTML = constructHtmlHeader(this.statusCodePhrase + ": SHUT DOWN SERVER");
					
			this.contentType = "text/html";
			this.curTime = sdfFormat.format(cal.getTime());
			this.contentLength = headerHTML.length();
					
			String responseStr = constructResponseStr();
					
			dataOutStream.writeBytes(responseStr);
			dataOutStream.writeBytes(headerHTML);
			return;
		}
		
		// If curURLWithoutField = "/control"
		if (curURLWithoutField.equals("control")) {
//			System.out.println("---- ResponseHandler:processHttpRequest() to Show control");
			this.statusCodePhrase = "200 OK";
					
			// Construct name and seas login
			StringBuilder sbControl = new StringBuilder();
			sbControl.append("<h1>Name: Yue Yu; SEAS Login: yueyu</h1>");
					
			// Parsing all related Thread information
			SingleThreadHandlerX2[] threadPool = this.curThreadPoolList;
			for (int i = 0; i < threadPool.length; i++) {
				sbControl.append("<p>Thread No. " + i + ": State: " + threadPool[i].getState().toString());
				if (threadPool[i].getState() == Thread.State.RUNNABLE) {
					// If is runnable we need to add the URL
					sbControl.append("; URL: " + threadPool[i].curRH.curURL);
				}
				sbControl.append("</p>");
			}
					
			// Append shut down button
			sbControl.append("<a href=\"/shutdown\"><button type=\"button\">Shutdown</button></a>");
					
			String headerHTML = sbControl.toString();
			this.contentType = "text/html";
			this.curTime = sdfFormat.format(cal.getTime());
			this.contentLength = headerHTML.length();
					
			String responseStr = constructResponseStr();
					
			dataOutStream.writeBytes(responseStr);
			dataOutStream.writeBytes(headerHTML);
			return;
		}
		
		// At this place, we need to open the directory to get all the files
//		System.out.println("---- ResponseHandler:processHttpRequest() Open directory/file: Thread " + this.threadNumber);
		String completeURL = this.rootDirectory + this.curURL;
//		System.out.println("---- ResponseHandler:processHttpRequest() Open directory/file: " + completeURL + " in Thread " + this.threadNumber);
		Path curPaths = Paths.get(completeURL);
		
		// Verifying the Existence of a File or Directory
		if (!Files.exists(curPaths)) {
			// If curPaths does not exist, not found!
//			System.out.println("---- ResponseHandler:processHttpRequest() Path doesn't exist: Thread " + this.threadNumber);
			this.statusCodePhrase = "404 NOT FOUND";
					
			String headerHTML = constructHtmlHeader(this.statusCodePhrase + ": ILLEGAL PATH");
				
			this.contentType = "text/html";
			this.curTime = sdfFormat.format(cal.getTime());
			this.contentLength = headerHTML.length();
					
			String responseStr = constructResponseStr();
			
			dataOutStream.writeBytes(responseStr);
			dataOutStream.writeBytes(headerHTML);
			return;
		}
				
		File curFile = new File(curPaths.toString());
		String toCheckIfGetParentRoot = curFile.getCanonicalPath();
		
		if (toCheckIfGetParentRoot.indexOf(this.rootDirectory) == -1) {
			// If we cannot find rootDirectory inside toCheckIfGetParentRoot, which means 
			// rootDirectory is not parent of toCheckIfGetParentRoot, Forbid to visit
//			System.out.println("---- ResponseHandler:processHttpRequest() Path is forbidden: Thread " + this.threadNumber);
			this.statusCodePhrase = "403 FORBIDDEN";
			
			String headerHTML = constructHtmlHeader(this.statusCodePhrase + ": CANNOT VISIT PARENT PATH");
			
			this.contentType = "text/html";
			this.curTime = sdfFormat.format(cal.getTime());
			this.contentLength = headerHTML.length();
			
			String responseStr = constructResponseStr();
			
			dataOutStream.writeBytes(responseStr);
			dataOutStream.writeBytes(headerHTML);
			return;
		}
		
		// Modified and unmodified situation
		if (this.httpVersionNo == (float) 1.1) {
			long fileLastModified = curFile.lastModified();
			if (this.isIfModified && fileLastModified < this.lInputModifiedDate) {
				// "IF-MODIFIED-SINCE" Situation: if no modified
//				System.out.println("---- ResponseHandler:processHttpRequest() isIfModified violation: Thread " + this.threadNumber);
				String isIfModifiedViolationResponse = 
						"HTTP/1.1 304 Not MOdified \n" +
						"Date: " + this.sInputModifiedDate + "\n";
				dataOutStream.writeBytes(isIfModifiedViolationResponse);
				return;	
			}
					
			if (this.isIfUnmodified && fileLastModified > this.lInputModifiedDate) {
				// "IF-UNMODIFIED-SINCE" Situation
				// "IF-MODIFIED-SINCE" Situation: if no modified
//				System.out.println("---- ResponseHandler:processHttpRequest() isIfUnmodified violation: Thread " + this.threadNumber);
				String isIfModifiedViolationResponse = 
						"HTTP/1.1 412 Precondition Failed \n";
				dataOutStream.writeBytes(isIfModifiedViolationResponse);
				return;	
			}
		}
		
		if (curFile.isDirectory()) {
			// Directory output to web page (in firefox)
//			System.out.println("---- ResponseHandler:processHttpRequest() Output Directory: Thread " + this.threadNumber);
			
			File[] fileList = curFile.listFiles();
			StringBuilder sbDirectory = new StringBuilder();
			sbDirectory.append("<h1>File List under: " + toCheckIfGetParentRoot + "</h1>");	// We use canonical Path, which is preferred
			
//			System.out.println("---- ResponseHandler:processHttpRequest() curURL: " + this.curURL + " Thread " + this.threadNumber);
//			System.out.println("---- ResponseHandler:processHttpRequest() curURL: " + this.curURL);
			
			for (int i = 0; i < fileList.length; i++) {
				// Two situations here,  for href purpose, 1. if it's not root directory; 2. it is root directory
				// If combined together, will cause an error
				if (!this.curURL.equals("/")) {
					sbDirectory.append("<p><a href=\"" + this.curURL + "/" + fileList[i].getName() 
							+ "\">" + fileList[i].getName().toString() + "</a></p>");
				}
				
				else {
					sbDirectory.append("<p><a href=\"/" + fileList[i].getName() 
							+ "\">" + fileList[i].getName().toString() + "</a></p>");
				}
			}
			
			String headerHTML = sbDirectory.toString();
			this.statusCodePhrase = "200 OK";
			this.contentType = "text/html";
			this.curTime = sdfFormat.format(cal.getTime());
			this.contentLength = headerHTML.length();
			
			String responseStr = constructResponseStr();
			
			try {
				dataOutStream.writeBytes(responseStr);
				dataOutStream.writeBytes(headerHTML);
			}
			 
			catch (Exception e) {
				// Just do nothing with it
			} 
			
//			System.out.println("---- ResponseHandler:processHttpRequest() Exiting: Thread " + this.threadNumber);
			return;
		}
		
		else {
			// Pure File output to web page (in firefox)
//			System.out.println("---- ResponseHandler:processHttpRequest() Output File: Thread " + this.threadNumber);
			
			// Read file into an InputStream and output to a ByteStreamOutput
			InputStream fileIn = new FileInputStream(curFile);
			ByteArrayOutputStream fileOutBytesStream = new ByteArrayOutputStream();
			int b;
			while ((b = fileIn.read()) != -1) {
				fileOutBytesStream.write(b);
			}
			
			byte[] outputInByteArray = fileOutBytesStream.toByteArray();
			
			// For the type, we will need the file's extension and return the correct type
			String curExtension = completeURL.substring(completeURL.lastIndexOf('.') + 1);
			this.statusCodePhrase = "200 OK";
			this.contentType = getFileType(curExtension);
			this.curTime = sdfFormat.format(cal.getTime());
			this.contentLength = outputInByteArray.length;
			
			String responseStr = constructResponseStr();
			
			try {
				dataOutStream.writeBytes(responseStr);
				if (command.equals("GET")) {
					dataOutStream.write(outputInByteArray);
				}
			}
			
			catch (Exception e) {
				// Just do nothing with it
			} 
//			System.out.println("---- ResponseHandler:processHttpRequest() Exiting: Thread " + this.threadNumber);
			return;
		}
	}
	
	private String constructHtmlHeader(String headerString) {
		return "<html><h2>" + headerString + "</h2></html>";
	}
	
	private String constructResponseStr() {
		String result = this.httpVersion + " " + this.statusCodePhrase + "\n"
				      + "Content-type: " + this.contentType + "\n"
				      + "Date: " + this.curTime + "\n" 
				      + "content-length: " + this.contentLength + "\n"
				      + "Server: HTTP Server" + "\n\n";
		return result;
	}
	
	private String getFileType(String extension) {
		switch(extension) {
			case "txt": {
				return "text/plain";
			}
			case "html": {
				return "text/html";
			}
			case "jpg": {	
				return "image/jpeg";
			}
			case "png": {
				return "image/png";
			}
			case "gif": {
				return "image/gif";
			}
			default: return "";
		}
	}
}
