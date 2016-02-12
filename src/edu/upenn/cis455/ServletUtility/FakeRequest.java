package edu.upenn.cis455.ServletUtility;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import edu.upenn.cis455.webserver.SessionContainer;

/*
 * This class deal with the request parsed in from the constructor
 */
public class FakeRequest implements HttpServletRequest {
	
	public HashMap<String, ArrayList<String>> queryParams 
			= new HashMap<String, ArrayList<String>>();		// Read after path and from POST
	public HashMap<String, ArrayList<String>> headerItems
			= new HashMap<String, ArrayList<String>>();		// Read from http request line 2 and beyond
	
	public final String BASIC_AUTH = "BASIC";

	// master objects parsed in from ResponseHandler
	Socket curSocket;
	BufferedReader curIn;
	String curLine1;				// This is the first line parsed in from RHX2.java
	
	// Request decoding
	String command;
	String uri;
	String protocolAndVersion;
	String postBody; 				// re-read from POST block
	
	SessionContainer curSessionContainer;
	String sessionID = "-1";
	
	/*
	 * Constructor. aiming at initialize the FakeRequest object
	 */
	public FakeRequest(Socket inputSocket, BufferedReader inputIn, 
					   String inputCurLine1, SessionContainer inputSC) {
		System.out.println("----- FakeRequest:FakeRequest() Entering");
		
		this.curSocket = inputSocket;
		this.curIn = inputIn;
		this.curLine1 = inputCurLine1;
		
		this.curSessionContainer = inputSC;
		
		decodeLine1();
		
		// After analyzing the first line, we try to get the second line here
		try {
			getSecondLine();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		
		// In case there are cookies, get the cookies out, and check if there's any 
		// Session information stored in the cookie, if so, get the Session Object out
		Cookie[] cookieList = this.getCookies();
		System.out.println("----- FakeRequest:FakeRequest() To print cookie");
		if (cookieList != null) {
			for (int i = 0; i < cookieList.length; i++) {
				System.out.println(cookieList[i]);
				if (cookieList[i].getName().equals("SessionID")) {
					System.out.println(cookieList[i].getValue());
					this.sessionID = cookieList[i].getValue();
					break;
				}
			}
		}
		
		System.out.println("----- FakeRequest:FakeRequest() SessionID: " + this.sessionID);
		
		if (!sessionID.equals("-1")) {
			System.out.println(this.curSessionContainer.SessionMap);
			this.m_session = this.curSessionContainer.SessionMap.get(Integer.parseInt(this.sessionID));
			System.out.println(m_session);
		}
	}
	
	/*
	 * Decode the input HTTP Request into command, URI and protocolAndVersion
	 */
	void decodeLine1() {
		// Similar to the beginning of RHX2.process API
		String[] httpRequestSplits = new String[3];
		
		try {
			httpRequestSplits = curLine1.split("\\s+");
		}
		
		catch (Exception e) {
			System.out.println("----- FakeRequest:decodeLine1() Unable to decode, throw an exception");
			throw e;
		}
		
		this.command = httpRequestSplits[0];
		this.uri = httpRequestSplits[1];
		this.protocolAndVersion = httpRequestSplits[2];
		
/*		System.out.println("----- FakeRequest:decodeLine1(): command :" + this.command +
				"; uri: " + this.uri + " protocolAndVersion: " + this.protocolAndVersion);*/
		
		// Now we still need to deal with the query part, when command is 'GET'
		if (this.command.equals("GET")) {
			String query = this.getQueryString();
			if (query != null) {
				// Now we need to load all parameters in the query
				this.loadQueriesParams(query);
			}
		}
	}
	
	/*
	 * This method gets the second line for the http request. Also, if this is a post, 
	 * read the post info (additional params) into QueriesParam HashMap
	 */
	void getSecondLine() throws IOException {
		System.out.println("----- FakeRequest:getSecondLine() Entering");
		
		String line2 = this.curIn.readLine();
		while (!line2.equals("")) {
			// Put the header element:queryParams modify information, hosting 
			this.loadLine2(line2);
			line2 = this.curIn.readLine();
		}
		
		// If here, we have the "POST" command, we need to parse out the 
		// content length 
		if (this.command.equals("POST")) {
			System.out.println("----- FakeRequest:getSecondLine(): Get Post Params");
			String contentLengthStr = this.getHeader("Content-Length");
			int contentLength = 0;
			if (contentLengthStr != null) {
				contentLength = Integer.parseInt(contentLengthStr);
			}
			
			// Now we need to read the additional params in and parse them to the 
			// queryParams hashMap
			StringBuilder paramSB = new StringBuilder();
			for (int i = 0; i < contentLength; i++) {
				paramSB.append((char) this.curIn.read());
			}
			
			this.postBody = paramSB.toString();
			this.loadQueriesParams(this.postBody);
			
			System.out.println("----- FakeRequest:getSecondLine(): After post, print QueriesParam");
			System.out.println(this.queryParams.toString());
		}
		
		System.out.println("----- FakeRequest:getSecondLine(): Exiting");
	}
	
	/*
	 * Load queries into hash map queryItems
	 */
	void loadQueriesParams(String query) {
		System.out.println("----- FakeRequest:loadQueriesParams() Entering");
		
		// In order to avoid unnecessary exception, decode query to UTF-8
		try {
			query = URLDecoder.decode(query, "UTF-8");
		} 
		
		catch (UnsupportedEncodingException e) {
			System.out.println("----- FakeRequest:loadQueries(): UTF-8 Decoding fails");
		}
		
		// Split the query string and parse into hash map
		String[] queryItems = query.split("&|\\;");		// Spilt with & or ;
		for (int i = 0; i < queryItems.length; i++) {
//			System.out.println("----- FakeRequest:loadQueries(): " + String.valueOf(i) + ": " + queryItems[i]);
			int idxEqual = queryItems[i].indexOf('=');
			String key = queryItems[i].substring(0, idxEqual);
			String value = queryItems[i].substring(idxEqual) + 1;
			
			if (this.queryParams.containsKey(key)) {
				// Shouldn't be here, continue
				System.out.println("----- FakeRequest:loadQueries(): Duplicate key: " + key);
				this.queryParams.get(key).add(value);
			}
			else {
				this.queryParams.put(key, new ArrayList<String>());
				this.queryParams.get(key).add(value);
			}
		}
		
		System.out.println("----- FakeRequest:loadQueriesParams() Exiting");
	}
	
	/*
	 * Load queries into HashMap headerItems
	 */
	void loadLine2(String line2) {
		System.out.println("----- FakeRequest:loadLine2() Entering");
		
		int idxOfColon = line2.indexOf(':');
		
		// Get the action (host) and its content
		String action = line2.substring(0, idxOfColon).toLowerCase();
		String content = line2.substring(idxOfColon + 1).trim();
		
		if (this.headerItems.get(action) == null) {
			this.headerItems.put(action, new ArrayList<String>());
		}
		
		this.headerItems.get(action).add(content);
		System.out.println(this.headerItems.toString());
		
		System.out.println("----- FakeRequest:loadLine2() Exiting");
	}
	
	/* 
	 * As required just return BASIC_AUTH
	 */
	public String getAuthType() {
		return this.BASIC_AUTH;
	}

	/* 
	 * Get all cookie object out of the HTTP request
	 */
	public Cookie[] getCookies() {
		System.out.println("----- FakeRequest:getCookies() Entering");
		Enumeration<String> cookiesEnum = this.getHeaders("Cookie");
		
		// Quick check and return 
		if (cookiesEnum == null || !cookiesEnum.hasMoreElements()) {
			System.out.println("----- FakeRequest:getCookies() Return null");
			return new Cookie[0];
		}
		
		// Constructor: Cookie(java.lang.String name, java.lang.String value) 
		ArrayList<Cookie> CookieArr = new ArrayList<Cookie>();
		String curName, curVal;
		while (cookiesEnum.hasMoreElements()) {
			// Get all cookies out and parse into Cookie object
			String curCookieStr = cookiesEnum.nextElement();
			System.out.println("----- FakeRequest:getCookies() curCookie: " + curCookieStr);
			int idxOfDelim = curCookieStr.indexOf(';');		// -1 represents for single cookie
			
			if (idxOfDelim == -1) {
				int idxOfKeyValueSep = curCookieStr.indexOf('=');
				curName = curCookieStr.substring(0, idxOfKeyValueSep);
				curVal = curCookieStr.substring(idxOfKeyValueSep + 1);
				Cookie toAdd = new Cookie(curName, curVal);
				CookieArr.add(toAdd);
			}
			
			else {
				String[] cookieSplits = curCookieStr.split(";");
				for (int nCt = 0; nCt < cookieSplits.length; nCt++) {
					String curCookieItemStr = cookieSplits[nCt].trim();
					int idxOfKeyValueSep = curCookieStr.indexOf('=');
					curName = curCookieStr.substring(0, idxOfKeyValueSep);
					curVal = curCookieStr.substring(idxOfKeyValueSep + 1);
					Cookie toAdd = new Cookie(curName, curVal);
					CookieArr.add(toAdd);
				}
			}
		}
		
		System.out.println("----- FakeRequest:getCookies() Print Cookies: ");
		System.out.println(CookieArr.get(0).getName());
		
		// Parse ArrayList to []
		Cookie[] toReturn = new Cookie[CookieArr.size()];
		for (int i = 0; i < CookieArr.size(); i++) {
			toReturn[i] = CookieArr.get(i);
		}
		
		System.out.println("----- FakeRequest:getCookies() Exiting");
		return toReturn;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getDateHeader(java.lang.String)
	 */
	public long getDateHeader(String arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* 
	 * Get the first of the input header name
	 */
	public String getHeader(String arg0) {
		// All keys in headerItems are stored in lower-case
		arg0 = arg0.toLowerCase();
		ArrayList<String> value = this.headerItems.get(arg0);
		
		if (value == null) return null;
		else return value.get(0);
	}

	/* 
	 * This method is similar to getHeader, besides it returns a read-only Enumeration
	 * Enumeration is different from arrayList in that it is read-only
	 * Example of changing arraylist to enumeration:  
	 * 			Enumeration e = Collections.enumeration(arrayList);
	 */
	public Enumeration getHeaders(String arg0) {
		// All keys in headerItems are stored in lower-case
		arg0 = arg0.toLowerCase();
		ArrayList<String> value = this.headerItems.get(arg0);
//		System.out.println("----- FakeRequest: getHeaders(): size: " + value.size());
		if (value == null) return null;
		else return Collections.enumeration(value);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
	 */
	public Enumeration getHeaderNames() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getIntHeader(java.lang.String)
	 */
	public int getIntHeader(String arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getMethod()
	 */
	public String getMethod() {
		return m_method;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getPathInfo()
	 */
	public String getPathInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getPathTranslated()
	 */
	public String getPathTranslated() {
		// For simplification, just return null
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getContextPath()
	 */
	public String getContextPath() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * This method reads into the part after QMark in the URL, if exist query things
	 * return it, otherwise just return null
	 */
	public String getQueryString() {
		int idxOfQMark = this.uri.indexOf('?');
		
		if (idxOfQMark != -1) {
			return this.uri.substring(idxOfQMark + 1);
		}
		else return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
	 */
	public String getRemoteUser() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
	 */
	public boolean isUserInRole(String arg0) {
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	public Principal getUserPrincipal() {
		// For simplification, just return null
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
	 */
	public String getRequestedSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getRequestURI()
	 */
	public String getRequestURI() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getRequestURL()
	 */
	public StringBuffer getRequestURL() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getServletPath()
	 */
	public String getServletPath() {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * Returns the current HttpSession associated with this request or, 
	 * if there is no current session and create is true, returns a new session. 
	 * If create is false and the request has no valid HttpSession, this method returns null.
	 */
	public HttpSession getSession(boolean arg0) {
		System.out.println("----- FakeRequest:getSession() Entering");
		if (arg0) {
			if (! hasSession()) {
				m_session = this.curSessionContainer.startNewSeesion();
			}
		} else {
			if (! hasSession()) {
				m_session = null;
			}
			else {
				long curTime = System.currentTimeMillis();
				m_session.setLastAccessedTime(curTime);
			}
		}
		return m_session;
	}

	/* 
	 * Call the getSession(true) API
	 */
	public HttpSession getSession() {
		if (this.m_session == null) {
			System.out.println("----- FakeRequest:getSession() m_session is null");
			return getSession(true);
		}
		
		else {
			return this.m_session;
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
	 */
	public boolean isRequestedSessionIdValid() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
	 */
	public boolean isRequestedSessionIdFromCookie() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
	 */
	public boolean isRequestedSessionIdFromURL() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
	 */
	public boolean isRequestedSessionIdFromUrl() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String arg0) {
		// TODO Auto-generated method stub
		return m_props.get(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getAttributeNames()
	 */
	public Enumeration getAttributeNames() {
		// TODO Auto-generated method stub
		return m_props.keys();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 */
	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
	 */
	public void setCharacterEncoding(String arg0)
			throws UnsupportedEncodingException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getContentLength()
	 */
	public int getContentLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getContentType()
	 */
	public String getContentType() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getInputStream()
	 */
	public ServletInputStream getInputStream() throws IOException {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
	 */
	public String getParameter(String arg0) {
		return m_params.getProperty(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getParameterNames()
	 */
	public Enumeration getParameterNames() {
		return m_params.keys();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
	 */
	public String[] getParameterValues(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getParameterMap()
	 */
	public Map getParameterMap() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getProtocol()
	 */
	public String getProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getScheme()
	 */
	public String getScheme() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getServerName()
	 */
	public String getServerName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getServerPort()
	 */
	public int getServerPort() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getReader()
	 */
	public BufferedReader getReader() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getRemoteAddr()
	 */
	public String getRemoteAddr() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getRemoteHost()
	 */
	public String getRemoteHost() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#setAttribute(java.lang.String, java.lang.Object)
	 */
	public void setAttribute(String arg0, Object arg1) {
		m_props.put(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(String arg0) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocale()
	 */
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocales()
	 */
	public Enumeration getLocales() {
		return null;
	}

	/* (non-Javadoc)createResponseHeader
	 * @see javax.servlet.ServletRequest#isSecure()
	 */
	public boolean isSecure() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
	 */
	public RequestDispatcher getRequestDispatcher(String arg0) {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
	 */
	public String getRealPath(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getRemotePort()
	 */
	public int getRemotePort() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocalName()
	 */
	public String getLocalName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocalAddr()
	 */
	public String getLocalAddr() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocalPort()
	 */
	public int getLocalPort() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setMethod(String method) {
		m_method = method;
	}
	
	void setParameter(String key, String value) {
		m_params.setProperty(key, value);
	}
	
	void clearParameters() {
		m_params.clear();
	}
	
	public boolean hasSession() {
		if (m_session != null) {
			System.out.println("----- FakeRequest:hasSession(): " + this.m_session == null);
		}
		return ((m_session != null) && m_session.isValid());
	}
		
	private Properties m_params = new Properties();
	private Properties m_props = new Properties();
	private FakeSession m_session = null;
	private String m_method;
}
