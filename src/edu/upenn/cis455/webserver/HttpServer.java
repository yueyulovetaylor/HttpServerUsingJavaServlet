package edu.upenn.cis455.webserver;

public class HttpServer {
	public static void main(String args[]) throws Exception {
		// There are two quick return situations here: 
		// 1. if no commands parsed in, print full name and SEAS login name; 
		// 2. if two or three commands, the first one is port to listen for 
		// connection, and root directory of the static web pages; 
		// 3. the directory of web.XML
//		System.out.println("- HttpServer.main() entering: num of args" + args.length);
		
		if (args.length != 3) {
			System.out.println("*** Author: Yue Yu (yueyu), exit!");
			return;
		}
		
		int portNumber = Integer.parseInt(args[0]);
		String directory = args[1];
		String webXMLDirec = args[2];
		
		DaemonThreadX2 curDT = new DaemonThreadX2(portNumber, directory, webXMLDirec);
		
		curDT.start();
		
		try {
			curDT.join();
		} catch (InterruptedException e) {
//			System.out.println("HttpServer.main() Current Thread cannot be initiated.");
			e.printStackTrace();
		}
	}
}
