package edu.upenn.cis455.SampleServlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GetParamServlet extends HttpServlet {
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// Request: GET /getParam?key1=thisVal1&key2=thisVal2 HTTP/1.0
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		
		String val1 = request.getParameter("key1");
		String val2 = request.getParameter("key2");
		
		out.println("GET getParam\n");
		out.println("key1=" + val1 + "\n");
		out.println("key2=" + val2 + "\n");
	}
	
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// Request: POST /getParam HTTP/1.0
		// content-length: 35
		// BODY: keyPOST1=valPOST1&keyPOST2=valPOST2
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		
		String val1 = request.getParameter("keyPOST1");
		String val2 = request.getParameter("keyPOST2");
		
		out.println("GET getParam\n");
		out.println("key1=" + val1 + "\n");
		out.println("key2=" + val2 + "\n");
	}
}
