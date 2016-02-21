package test.edu.upenn.cis455.hw1;

import java.io.DataOutputStream;
import java.io.OutputStream;

import edu.upenn.cis455.ServletUtility.FakePrintWriter;
import junit.framework.TestCase;

public class FakePrintWriterTest extends TestCase {
	FakePrintWriter testFPW;
	DataOutputStream dataOut;
	
	public void setUp() throws Exception {	
		OutputStream outStream = null;
		dataOut = new DataOutputStream(outStream);
	}
	
	public void testPrintlnString() {
		testFPW = new FakePrintWriter(dataOut, null);
		
		testFPW.bHeaderWritten = true;			// No need to write header
		testFPW.println("Hello");
		
		String expected0 = new String(this.testFPW.buffer).substring(0, 5);
		String actual0 = "Hello";
		boolean equalsOrNot = expected0.equals(actual0);
		assertEquals("Print not end unsuccessful", equalsOrNot, true);
	}

	public void testSetBufferSize() {
		testFPW = new FakePrintWriter(dataOut, null);
		
		int expextedInit = 1000;
		int actualInit = this.testFPW.getBufferSize();
		
		assertEquals("Initial size incorrect", actualInit, expextedInit);
		
		testFPW.setBufferSize(50);
		
		int expextedSet = 50;
		int actualSet = this.testFPW.getBufferSize();
		
		assertEquals("Reset unsuccessfully", actualSet, expextedSet);
	}

	public void testResetBuffer() {
		testFPW = new FakePrintWriter(dataOut, null);
		
		testFPW.bHeaderWritten = true;			// No need to write header
		testFPW.println("Hello");
		
		String expected0 = new String(this.testFPW.buffer).substring(0, 5);
		String actual0 = "Hello";
		boolean equalsOrNot = expected0.equals(actual0);
		assertEquals("Print not end unsuccessful", equalsOrNot, true);
		
		testFPW.resetBuffer();
		int expected1 = testFPW.curBufferSize;
		int actual1 = 0;
		assertEquals("Reset successfully", expected1, actual1);
	}

}
