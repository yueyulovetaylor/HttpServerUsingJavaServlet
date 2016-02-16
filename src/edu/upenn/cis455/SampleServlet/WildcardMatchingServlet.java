package edu.upenn.cis455.SampleServlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WildcardMatchingServlet extends HttpServlet {
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
//		System.out.println("----- BusyServlet:Service(DOGET)(): Entering");
		
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<HTML><HEAD><TITLE>Busy Servlet</TITLE></HEAD><BODY>");
		out.println("<h1>This is a wildcard matching test page. Welcom and well done!</h1>");
		out.println("<p>Get pathinfo: " + request.getPathInfo() + "</p>");
		out.println("</BODY></HTML>\n");
//		System.out.println("----- BusyServlet:Service(DOGET)(): Exiting");
	}
}
