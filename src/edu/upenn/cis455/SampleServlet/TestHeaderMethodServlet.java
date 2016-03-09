package edu.upenn.cis455.SampleServlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestHeaderMethodServlet extends HttpServlet {

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		System.out.println("----- TestHeaderMethodServlet:Service(DOPOST)(): Entering");
		
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("Test Header and Method Servlet\n");
		out.println("Method: " + request.getMethod() + "\n");
		out.println("Header: ");
		out.println(request.getHeader("Host"));
		System.out.println("----- TestHeaderMethodServlet:Service(DOPOST)(): Exiting");
	}
	
}
