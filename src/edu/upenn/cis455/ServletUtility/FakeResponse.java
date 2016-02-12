package edu.upenn.cis455.ServletUtility;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Yue Yu, FakeResponse to construct http response regarding servlet
 */
public class FakeResponse implements HttpServletResponse {

	DataOutputStream curDataOutStream;
	int statusCode;
	String statusContent;
	String contentType;
	int contentLength;
	String CharacterEncoding;
	
	HashMap <String, ArrayList<String>> headerHash 
			= new HashMap <String, ArrayList<String>>();		// Store header
	FakeRequest curFR;
	
	// Time utility objects
	final String DateTimeFormat = "EEE, d MMM yyyy HH:mm:ss Z";
	final SimpleDateFormat sdfFormat = new SimpleDateFormat(DateTimeFormat);		
	Calendar cal = Calendar.getInstance();
	long curTime;
	FakePrintWriter fPW;
	
	public FakeResponse(DataOutputStream inputDataOutStream, FakeRequest inputFR) {
		System.out.println("----- FakeResponse:FakeResponse(): Entering");
		
		this.curDataOutStream = inputDataOutStream;
		this.statusCode = 200;
//		this.contentType = "text/html";
//		this.contentLength = 1000;
		this.setContentType("text/html");
//		this.setContentLength(1000);
		this.curTime = System.currentTimeMillis();
		this.addDateHeader("Date", this.curTime);
//		this.curTime = sdfFormat.format(cal.getTime());
		this.curFR = inputFR;
		this.fPW = new FakePrintWriter(this.curDataOutStream, this);
		
		System.out.println("----- FakeResponse:FakeResponse(): Exiting");
	}
	
	public String createResponseHeader() {
		
		// Check if there is session existing in Request, if so add it to Cookie
		if (this.curFR.hasSession()) {
			System.out.println("----- FakeResponse:createResponseHeader(): Find Cookie!");
			System.out.println(this.curFR.curSessionContainer.SessionMap);
			FakeSession toParseIntoCookie = (FakeSession) this.curFR.getSession(false);
			String sessionID = toParseIntoCookie.getId();
			Cookie sessionIDCookie = new Cookie("SessionID", sessionID);
			sessionIDCookie.setMaxAge(3600);		// Let sessionID exists for an hour
			this.addCookie(sessionIDCookie);
		}
		
		StringBuilder repSB = new StringBuilder();
		repSB.append("HTTP/1.1 ");
		repSB.append(this.statusCode + " " + this.getCodeStatus(this.statusCode) + "\n");
//		repSB.append("Content-type: " + this.contentType + "\n");
//		repSB.append("Date: " + this.curTime + "\n");
//		repSB.append("content-length: " + this.contentLength + "\n");
		
		for (String keyItem: this.headerHash.keySet()) {
			ArrayList<String> curContent = this.headerHash.get(keyItem);
			for (int i = 0; i < curContent.size(); i++) {
				repSB.append(keyItem + ": " + curContent.get(i) + "\n");
			}
		}
		
		repSB.append("Server: HTTP Server" + "\n\n");
		
		return repSB.toString();
	}
	
	private String getCodeStatus(int code) {
		switch(code) {
		case 200:
			return "OK";
		}
		
		return null;
	}
	
	private String dateLong2Str(long inputLong) {
		Date curD = new Date(inputLong);
		SimpleDateFormat tmpToUse = this.sdfFormat;
		tmpToUse.setTimeZone(TimeZone.getTimeZone("GMT"));
		return this.sdfFormat.format(curD);			
	}
	
	/* 
	 * Add input cookie to header
	 */
	public void addCookie(Cookie arg0) {
		System.out.println("----- FakeResponse:FakeResponse(): Add cookie: " 
					+ arg0.getName() + " MaxAge: " + arg0.getMaxAge());
		
		String cookieStr = arg0.getName() + "=" + arg0.getValue();
		
		if (arg0.getMaxAge() != -1) {
			// Cookie has expiry time
			Long expiryTime = System.currentTimeMillis() + arg0.getMaxAge() * 1000;
			System.out.println("----- FakeResponse:FakeResponse(): MaxAge: " + arg0.getMaxAge());
			String expiryTimeStr = this.dateLong2Str(expiryTime);
			cookieStr += "; Expires=" + expiryTimeStr;
		}
		
			System.out.println("----- FakeResponse:FakeResponse(): content: " + cookieStr);
			
		if (this.headerHash.containsKey("Set-Cookie")) {
			this.headerHash.get("Set-Cookie").add(cookieStr);
		}
		
		else {
			ArrayList<String> toInsert = new ArrayList<String>();
			toInsert.add(cookieStr);
			this.headerHash.put("Set-Cookie", toInsert);
		}
	}

	/* 
	 * If current header exists in the hash Map
	 */
	public boolean containsHeader(String arg0) {
		if (this.headerHash.containsKey(arg0)) return true;
		else return false;
	}

	/* 
	 * No todo sign, don't care
	 */
	public String encodeURL(String arg0) {
		return arg0;
	}

	/* 
	 * No todo sign, don't care
	 */
	public String encodeRedirectURL(String arg0) {
		return arg0;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#encodeUrl(java.lang.String)
	 */
	public String encodeUrl(String arg0) {
		return arg0;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#encodeRedirectUrl(java.lang.String)
	 */
	public String encodeRedirectUrl(String arg0) {
		// TODO Auto-generated method stub
		return arg0;
	}

	/* 
	 * Sends an error response to the client using the specified status. 
	 * The server defaults to creating the response to look like an HTML-formatted server error page
	 * If committed, send an IllegalStateException
	 */
	public void sendError(int arg0, String arg1) throws IOException {
		if (this.fPW.bCommited) {
			throw new IllegalStateException();
		}

		this.setStatus(arg0);
		this.setContentType("text/html");
		
		String toOutput = constructHtmlHeader(arg0 + ": " + arg1);
		byte[] toOut = toOutput.getBytes(StandardCharsets.UTF_8);
		try {
			this.curDataOutStream.write(toOut);
		} 
		
		catch (IOException e) {
			System.out.println("----- FakePrintWriter:flushBuffer(): Cannot write to Out: Bad Exiting");
			e.printStackTrace();
		}
	}

	private String constructHtmlHeader(String headerString) {
		return "<html><h2>" + headerString + "</h2></html>";
	}
	
	/* 
	 * Similar to sendError(two parameters), besides we need to get arg1 from arg0
	 */
	public void sendError(int arg0) throws IOException {
		if (this.fPW.bCommited) {
			throw new IllegalStateException();
		}

		this.setStatus(arg0);
		this.setContentType("text/html");
		String arg1 = this.getCodeStatus(arg0);
		
		String toOutput = constructHtmlHeader(arg0 + ": " + arg1);
		byte[] toOut = toOutput.getBytes(StandardCharsets.UTF_8);
		try {
			this.curDataOutStream.write(toOut);
		} 
		
		catch (IOException e) {
			System.out.println("----- FakePrintWriter:flushBuffer(): Cannot write to Out: Bad Exiting");
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#sendRedirect(java.lang.String)
	 */
	public void sendRedirect(String arg0) throws IOException {
		System.out.println("[DEBUG] redirect to " + arg0 + " requested");
		System.out.println("[DEBUG] stack trace: ");
		Exception e = new Exception();
		StackTraceElement[] frames = e.getStackTrace();
		for (int i = 0; i < frames.length; i++) {
			System.out.print("[DEBUG]   ");
			System.out.println(frames[i].toString());
		}
	}

	/* 
	 * Always set the first pos of Hash's arraylist, same as set the first one
	 */
	public void setDateHeader(String arg0, long arg1) {
		String setTime = this.dateLong2Str(arg1);
		if (this.containsHeader(arg0)) {
			// Set pos 0
			this.headerHash.get(arg0).set(0, setTime);
		}
		else {
			ArrayList<String> toInsert = new ArrayList<String>();
			toInsert.add(setTime);
			this.headerHash.put(arg0, toInsert);
		}
	}

	/* 
	 * appending to the corresponding arraylist
	 */
	public void addDateHeader(String arg0, long arg1) {
		String setTime = this.dateLong2Str(arg1);
		if (this.containsHeader(arg0)) {
			// Set pos 0
			this.headerHash.get(arg0).add(setTime);
		}
		else {
			ArrayList<String> toInsert = new ArrayList<String>();
			toInsert.add(setTime);
			this.headerHash.put(arg0, toInsert);
		}

	}

	/* 
	 * Always set the first pos of Hash's arraylist, same as set the first one
	 */
	public void setHeader(String arg0, String arg1) {
		if (this.containsHeader(arg0)) {
			// Set pos 0
			this.headerHash.get(arg0).set(0, arg1);
		}
		else {
			ArrayList<String> toInsert = new ArrayList<String>();
			toInsert.add(arg1);
			this.headerHash.put(arg0, toInsert);
		}
	}

	/* 
	 * appending to the corresponding arraylist
	 */
	public void addHeader(String arg0, String arg1) {
		if (this.containsHeader(arg0)) {
			// Set pos 0
			this.headerHash.get(arg0).add(arg1);
		}
		else {
			ArrayList<String> toInsert = new ArrayList<String>();
			toInsert.add(arg1);
			this.headerHash.put(arg0, toInsert);
		}
	}

	/* 
	 * Always set the first pos of Hash's arraylist, same as set the first one
	 */
	public void setIntHeader(String arg0, int arg1) {
		String intStr = Integer.toString(arg1);
		
		if (this.containsHeader(arg0)) {
			// Set pos 0
			this.headerHash.get(arg0).set(0, intStr);
		}
		else {
			ArrayList<String> toInsert = new ArrayList<String>();
			toInsert.add(intStr);
			this.headerHash.put(arg0, toInsert);
		}
	}

	/* 
	 * appending to the corresponding arraylist
	 */
	public void addIntHeader(String arg0, int arg1) {
		String intStr = Integer.toString(arg1);

		if (this.containsHeader(arg0)) {
			// Set pos 0
			this.headerHash.get(arg0).add(intStr);
		}
		else {
			ArrayList<String> toInsert = new ArrayList<String>();
			toInsert.add(intStr);
			this.headerHash.put(arg0, toInsert);
		}
	}

	/* 
	 * Set statusCode 
	 */
	public void setStatus(int arg0) {
		this.statusCode = arg0;
	}

	/* 
	 * Set statusCode and statusContent
	 */
	public void setStatus(int arg0, String arg1) {
		this.statusCode = arg0;
		this.statusContent = arg1;
	}

	/* 
	 * return CharacterEncoding
	 */
	public String getCharacterEncoding() {
		return this.CharacterEncoding;
	}

	/* 
	 * return ContentType
	 */
	public String getContentType() {
		return this.contentType;
	}

	/* 
	 * No todo signal, don't care
	 */
	public ServletOutputStream getOutputStream() throws IOException {
		return null;
	}

	/* 
	 * Return the printerWriter object
	 */
	public PrintWriter getWriter() throws IOException {
		return this.fPW;
	}

	/* 
	 * Set CharacterEncoding
	 */
	public void setCharacterEncoding(String arg0) {
		this.CharacterEncoding = arg0;
	}

	/* 
	 * Set contentLength not as default
	 */
	public void setContentLength(int arg0) {
		this.contentLength = arg0;
		Integer length = (Integer) arg0;
		
		if (this.containsHeader("content-length")) {
			// Always set pos 0 as the content-length
			this.headerHash.get("Content-length").set(0, length.toString());
		}
		
		else {
			ArrayList<String> toInsert = new ArrayList<String>();
			toInsert.add(length.toString());
			this.headerHash.put("Content-length", toInsert);
		}
	}

	/* 
	 * Set the contentType of the response and add to the Hash Map if necessary
	 */
	public void setContentType(String arg0) {
		this.contentType = arg0;
		
		if (this.containsHeader("Content-type")) {
			// Always set pos 0 as the content-type
			this.headerHash.get("Content-type").set(0, arg0);
		}
		
		else {
			ArrayList<String> toInsert = new ArrayList<String>();
			toInsert.add(arg0);
			this.headerHash.put("Content-type", toInsert);
		}
	}

	/* 
	 * Call set bufferSize API in fakePW object
	 */
	public void setBufferSize(int arg0) {
		this.fPW.setBufferSize(arg0);
	}

	/* 
	 * return the curBufferSize in PrintWriter object
	 */
	public int getBufferSize() {
		return this.fPW.curBufferSize;
	}

	/* 
	 * Call flushBuffer() API in PrintWriter object
	 */
	public void flushBuffer() throws IOException {
		this.fPW.flushBuffer();
	}

	/* 
	 * Call resetBuffer() API in PrintWriter Object
	 */
	public void resetBuffer() {
		this.fPW.resetBuffer();

	}

	/* 
	 * check bCommited in PrintWriter Object
	 */
	public boolean isCommitted() {
		return this.fPW.bCommited;
	}

	/* 
	 * Clears any data that exists in the buffer as well as the status code and headers. 
	 * If the response has been committed, this method throws an IllegalStateException.
	 */
	public void reset() {
		if (this.fPW.bCommited) {
			throw new IllegalStateException();
		}
		
		// Reset everything
		this.headerHash.clear();
		this.fPW.resetBuffer();
	}

	/* 
	 * As required in assignment handouts, no need to care
	 */
	public void setLocale(Locale arg0) {
		return;
	}

	/* 
	 * As required in assignment handouts, no need to care
	 */
	public Locale getLocale() {
		return null;
	}

}
