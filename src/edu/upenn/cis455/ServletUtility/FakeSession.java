package edu.upenn.cis455.ServletUtility;

import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/*
 * Hash Value of Session Container
 */
public class FakeSession implements HttpSession {

	int sessionID;
	long createTime;
	long lastAccessTime; 		
	boolean bValid;
	
	/*
	 * Constructor: initiate a new Session Object, called by startNewSession
	 * of SessionContainer
	 */
	public FakeSession(int inputSessionID) {
		this.sessionID = inputSessionID;
		this.createTime = System.currentTimeMillis();
		this.bValid = true;
	}
	
	/* 
	 * return long createTime
	 */
	public long getCreationTime() {
		return this.createTime;
	}

	/* 
	 * return sessionID
	 */
	public String getId() {
		return Integer.toString(sessionID);
	}
	
	/*
	 * Actually, it's last updated by FakeRequest.getSession(false)
	 */
	public void setLastAccessedTime(long inputModifiedTime) {
		this.lastAccessTime = inputModifiedTime;
	}
	
	/* 
	 * Get the value in previous API
	 */
	public long getLastAccessedTime() {
		return this.lastAccessTime;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getServletContext()
	 */
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#setMaxInactiveInterval(int)
	 */
	public void setMaxInactiveInterval(int arg0) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getMaxInactiveInterval()
	 */
	public int getMaxInactiveInterval() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getSessionContext()
	 */
	public HttpSessionContext getSessionContext() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String arg0) {
		// TODO Auto-generated method stub
		return m_props.get(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getValue(java.lang.String)
	 */
	public Object getValue(String arg0) {
		// TODO Auto-generated method stub
		return m_props.get(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getAttributeNames()
	 */
	public Enumeration getAttributeNames() {
		// TODO Auto-generated method stub
		return m_props.keys();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getValueNames()
	 */
	public String[] getValueNames() {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * Put arg0 and arg1 to attributes object
	 */
	public void setAttribute(String arg0, Object arg1) {
		m_props.put(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#putValue(java.lang.String, java.lang.Object)
	 */
	public void putValue(String arg0, Object arg1) {
		m_props.put(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(String arg0) {
		m_props.remove(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#removeValue(java.lang.String)
	 */
	public void removeValue(String arg0) {
		m_props.remove(arg0);
	}

	/* 
	 * Invalidates this session then unbinds any objects bound to it.
	 * java.lang.IllegalStateException - if this method is called on an already invalidated session
	 */
	public void invalidate() {
		if (!this.bValid) {
			throw new IllegalStateException();
		}
		m_valid = false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#isNew()
	 */
	public boolean isNew() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isValid() {
		return m_valid;
	}
	
	public int getPropertiesSize() {
		return this.m_props.size();
	}
	
	private Properties m_props = new Properties();
	private boolean m_valid = true;
}
