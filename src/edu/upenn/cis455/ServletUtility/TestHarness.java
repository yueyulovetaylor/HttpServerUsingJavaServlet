package edu.upenn.cis455.ServletUtility;

import java.io.File;
import java.util.HashMap;

import javax.servlet.http.HttpServlet;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/*
 * Revised by Yue Yu
 */
public class TestHarness {	
	public static class Handler extends DefaultHandler {
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			if (qName.compareTo("servlet-name") == 0) {
				m_state = 1;
			} 
			
			else if (qName.compareTo("servlet-class") == 0) {
				m_state = 2;
			} 
			
			else if (qName.compareTo("context-param") == 0) {
				m_state = 3;
			} 
			
			else if (qName.compareTo("init-param") == 0) {
//				System.out.println("--- TestHarness:handler.startElement: find_init_param");
				m_state = 4;
			} 
			
			else if (qName.compareTo("url-pattern") == 0) {
				// This when the XML includes "url-pattern" tag
				m_state = 5;
			}
			
			else if (qName.compareTo("param-name") == 0) {
//				m_state = (m_state == 3) ? 10 : 20;
				switch(m_state) {
				case 3: 
					m_state = 10;
					break;
				case 5:
					m_state = 20;
					break;
				case 4:
					m_state = 30;			// Self-definition
					break;
				default:
					System.err.println("State Error: shouldn't have param in this block");
					System.exit(-1);
				}
			} 
			
			else if (qName.compareTo("param-value") == 0) {
				//m_state = (m_state == 10) ? 11 : 21;
				switch(m_state) {
				case 10:
					m_state = 11;
					break;
				case 20:
					m_state = 21;
					break;
				case 30:
					m_state = 31;
					break;
				default:
					System.err.println("State Error: shouldn't have value in this block");
					System.exit(-1);
				}
			}
		}
		
		public void characters(char[] ch, int start, int length) {
			String value = new String(ch, start, length);
			if (m_state == 1) {
				m_servletName = value;
				m_state = 0;
			} 
			
			else if (m_state == 2) {
				m_servlets.put(m_servletName, value);
				m_state = 0;
			} 
			
			else if (m_state ==  5) {
				// Put servlet name and its url pattern name into the hashMap
				m_urlPatterns.put(value, m_servletName);
				m_state = 0;
			}
			
			else if (m_state == 10 || m_state == 20 || m_state == 30) {
				m_paramName = value;
			} 
			
			else if (m_state == 11) {
				if (m_paramName == null) {
					System.err.println("Context parameter value '" + value + "' without name");
					System.exit(-1);
				}
				m_contextParams.put(m_paramName, value);
				m_paramName = null;
				m_state = 0;
			} 
			
			else if (m_state == 21) {
				if (m_paramName == null) {
					System.err.println("Servlet parameter value '" + value + "' without name");
					System.exit(-1);
				}
				HashMap<String,String> p = m_servletParams.get(m_servletName);
				if (p == null) {
					p = new HashMap<String,String>();
					m_servletParams.put(m_servletName, p);
				}
				p.put(m_paramName, value);
				m_paramName = null;
				m_state = 0;
			}
			
			else if (m_state == 31) {
				if (m_paramName == null) {
					System.err.println("Context parameter value '" + value + "' without name");
					System.exit(-1);
				}
				
//				System.out.println("--- TestHarness.handler.characters(): parse init_param into: " + this.m_servletName);
//				System.out.println(m_paramName + ": " + value);
//				System.out.println("Servlet name: " + this.m_servletName);
//				System.out.println("Servlet contains: " + this.m_servletParams.entrySet());

				if (this.m_servletParams.containsKey(this.m_servletName)) {
//					System.out.println("--- TestHarness.handler.characters(): putin HashMap: " + m_paramName);
					this.m_servletParams.get(this.m_servletName).put(m_paramName, value);
				}
				
				else {
//					System.out.println("--- TestHarness.handler.characters(): initiate HashMap: " + m_paramName);
					HashMap<String,String> curInitParam = new HashMap<String,String>();
					curInitParam.put(m_paramName, value);
					this.m_servletParams.put(this.m_servletName, curInitParam);
				}
				
//				System.out.println("After insertion: Servlet contains: " + this.m_servletParams.entrySet());
				
//				FakeContext fContext = new FakeContext();
//				fContext.setAttribute(m_paramName, value);
//				FakeConfig fConf = new FakeConfig(value, fContext);
//				this.m_servlets.get(this.m_servletName);
				
				m_paramName = null;
				m_state = 0;
			}
		}
		private int m_state = 0;
		private String m_servletName;
		private String m_paramName;
		
		// We need a new hashMap here to store the url pattern in the xml tag
		// and the init_param
		public HashMap<String,String> m_urlPatterns = new HashMap<String,String>();
		
		HashMap<String,String> m_servlets = new HashMap<String,String>();
		HashMap<String,String> m_contextParams = new HashMap<String,String>();
//		HashMap<String,String> m_initParam = new HashMap<String,String>();
		HashMap<String,HashMap<String,String>> m_servletParams 
								= new HashMap<String,HashMap<String,String>>();
	}
		
	public static Handler parseWebdotxml(String webdotxml) throws Exception { 
//		System.out.println("--- TestHarness.parseWebdotxml(): Entering");
		
		Handler h = new Handler();
		File file = new File(webdotxml);
		if (file.exists() == false) {
			System.err.println("--- TestHarness.parseWebdotxml(): cannot find " + file.getPath());
			System.exit(-1);
		}
		SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		parser.parse(file, h);
		
//		System.out.println("--- TestHarness.parseWebdotxml(): Exiting");
		return h;
	}
	
	public static FakeContext createContext(Handler h) {
		FakeContext fc = new FakeContext();
		
//		System.out.println("--- TestHarness.createContext() contextParams: " + h.m_contextParams.entrySet());
		
		for (String param : h.m_contextParams.keySet()) {
//			System.out.println(param + ": " + h.m_contextParams.get(param));
			fc.setInitParam(param, h.m_contextParams.get(param));
		}
		return fc;
	}
	
	public static HashMap<String,HttpServlet> createServlets(Handler h, FakeContext fc) throws Exception {
//		System.out.println("--- TestHarness.createServlets(): Entering");
		HashMap<String,HttpServlet> servlets = new HashMap<String,HttpServlet>();
		for (String servletName : h.m_servlets.keySet()) {
			FakeConfig config = new FakeConfig(servletName, fc);
			String className = h.m_servlets.get(servletName);
			Class servletClass = Class.forName("edu.upenn.cis455.SampleServlet." + className);
			HttpServlet servlet = (HttpServlet) servletClass.newInstance();
			
			HashMap<String,String> servletParams = h.m_servletParams.get(servletName);
//			System.out.println("Servlet parameter size: " + servletParams.size());
			
			if (servletParams != null) {
				for (String param : servletParams.keySet()) {
//					System.out.println("Set parameters: " + param);
					config.setInitParam(param, servletParams.get(param));
				}
			}
			
//			System.out.println(config);
			
			servlet.init(config);
//			System.out.println(servletName + ": " + servlet);
//			System.out.println("TestParam: " + servlet.getServletConfig().getInitParameter("TestParam"));
			servlets.put(servletName, servlet);
		}
		
//		System.out.println("--- TestHarness.createServlets(): Exiting");
		return servlets;
	}

	private static void usage() {
		System.err.println("usage: java TestHarness <path to web.xml> " 
				+ "[<GET|POST> <servlet?params> ...]");
	}
}
 
