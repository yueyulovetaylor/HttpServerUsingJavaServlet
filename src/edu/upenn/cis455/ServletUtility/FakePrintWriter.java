// Similar to Read4 in Leetcode
package edu.upenn.cis455.ServletUtility;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class FakePrintWriter extends PrintWriter {
	
//	String header;
	int BUFFER_MAX_SIZE = 1000;
	DataOutputStream curOut;
	public int curBufferSize;
//	byte[] outputByteBuffer;
	boolean bHeaderWritten;
//	StringBuilder bodyBuffer = new StringBuilder();
	
	// Since we have flushbuffer here, so we will be needing Buffer
	// Here we set buffer as char
	char[] buffer = new char[BUFFER_MAX_SIZE];
	FakeResponse curFR;
	public boolean bCommited;
	
	public FakePrintWriter(OutputStream out, FakeResponse inputFR) {
		super(out);
		
		// Set input parameter and buffer
//		this.header = inputHeader;
		this.curFR = inputFR;
		this.curOut = new DataOutputStream(out);
		this.curBufferSize = 0;
		this.bCommited = false;
//		this.bHeaderWritten = false;
	}

	/*
	 * Save 
	 */
	@Override
	public void println(String inputStr) {
		System.out.println("----- FakePrintWriter:println(): Entering");
		
		// Chrome content-length dismatch, probably need to dynamically adjust content-length 
		// and update header.
		
		// Write header if didn't write
		if (!bHeaderWritten) {
			System.out.println("----- FakePrintWriter:println(): Write header");
			String header = this.curFR.createResponseHeader();
			byte[] headerB = header.getBytes(StandardCharsets.UTF_8);
			
			try {
				this.curOut.write(headerB);
			} 
			
			catch (IOException e) {
				System.out.println("----- FakePrintWriter:println(): Unable to write header");
				e.printStackTrace();
			}
			
			bHeaderWritten = true;
			this.bCommited = true;
		}
		
/*		byte[] b = inputStr.getBytes(StandardCharsets.UTF_8);
		try {
			this.curOut.write(b);
		} 
		
		catch (IOException e) {
			System.out.println("----- FakePrintWriter:println(): Unable to println");
			e.printStackTrace();
		}*/
		
		// Push all char into buffer, if buffer is full, flushbuffer once
		for (int i = 0; i < inputStr.length(); i++) {
			if (this.curBufferSize == 0) {
				// Buffer starts to be updated
				this.bCommited = false;
			}
			buffer[this.curBufferSize] = inputStr.charAt(i);
			this.curBufferSize++;
			if (this.curBufferSize == this.BUFFER_MAX_SIZE) {
				this.flushBuffer();
			}
		}
		
		// flushBuffer if it is the end of the HTML
		if (inputStr.contains("/HTML")) {
			this.flushBuffer();
		}
		
		System.out.println("----- FakePrintWriter:println(): Exiting");
	}
	
	/*
	 * If current buffer is full or flushBuffer is called by FakeResponse,
	 * this method out print all current buffer to socket and flip isCommited 
	 * to true
	 */
	public void flushBuffer() {
		byte[] toOut = new String(this.buffer).getBytes(StandardCharsets.UTF_8);
		try {
			this.curOut.write(toOut);
		} 
		
		catch (IOException e) {
			System.out.println("----- FakePrintWriter:flushBuffer(): Cannot write to Out: Bad Exiting");
			e.printStackTrace();
		}
		
		// Successfully out print, 
//		this.buffer = new char[this.BUFFER_MAX_SIZE];
//		this.curBufferSize = 0;
//		this.bCommited = true;
		this.resetBuffer();
	}
	
	/*
	 * Called by FakeResponse, to reset buffer size, 
	 * copy current buffer to a tmp and new it, finally copy back
	 */
	public void setBufferSize(int arg0) {
		// Copy to tmp
		char[] tmpBuffer = new char[this.BUFFER_MAX_SIZE];
		for (int i = 0; i < this.curBufferSize; i++) {
			tmpBuffer[i] = this.buffer[i];
		}
		
		// Update MAX_BUFFER_SIZE and buffer
		this.BUFFER_MAX_SIZE = arg0;
		this.buffer = new char[this.BUFFER_MAX_SIZE];
		
		// Copy back
		for (int i = 0; i < this.curBufferSize; i++) {
			this.buffer[i] = tmpBuffer[i];
		}
	}
	
	/*
	 * Reset buffer to a new char and status parameter, called by Response object
	 * and flushBuffer
	 */
	public void resetBuffer() {
		// clear up buffer, reset size and flip bCommit to true
		this.buffer = new char[this.BUFFER_MAX_SIZE];
		this.curBufferSize = 0;
		this.bCommited = true;
	}
	
}
