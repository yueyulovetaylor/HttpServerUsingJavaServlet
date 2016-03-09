package edu.upenn.cis455.SampleServlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AddHeaderAfterWriteServlet extends HttpServlet {
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		
		out.println("Add Header after writing begins:\n");
		response.addHeader("addHeader1", "val1");
		out.println("Contain addHeader1 is: ");
		boolean contains = response.containsHeader("addHeader1");
		out.println(contains + "\n");
	}
}
