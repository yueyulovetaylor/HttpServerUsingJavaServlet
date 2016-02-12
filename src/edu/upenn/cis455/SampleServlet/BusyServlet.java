package edu.upenn.cis455.SampleServlet;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.*;

// Test with Chrome will incur Content mismatch problem:
// TODO: Dynamically change content length and header before write

public class BusyServlet extends HttpServlet {
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		System.out.println("----- BusyServlet:Service(DOGET)(): Entering");
		
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<HTML><HEAD><TITLE>Busy Servlet</TITLE></HEAD><BODY>");
		out.println("<P>Starting work...</P>");
		for (int j = 1; j < 3; ++j) {
			for (int i = 0; i < Integer.MAX_VALUE; ++i) {
			}
		}
		out.println("<P>Done!</P>");
		out.println("</BODY></HTML>\n");
		System.out.println("----- BusyServlet:Service(DOGET)(): Exiting");
	}
}
