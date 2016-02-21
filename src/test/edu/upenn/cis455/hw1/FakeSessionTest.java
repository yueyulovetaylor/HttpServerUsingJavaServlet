package test.edu.upenn.cis455.hw1;

import edu.upenn.cis455.ServletUtility.FakeSession;
import junit.framework.TestCase;

/*
 * JUnit test for Session Class
 */
public class FakeSessionTest extends TestCase {

	// Test if Set and Get Attribute API works
	public void testGetAttribute() {
		FakeSession FF = new FakeSession(1);
		FF.setAttribute("Attr1", "Val1");
		
		Object OutputObj = FF.getAttribute("Attr1");
		String actual = OutputObj.toString();
		String expected = "Val1";
		
		int actualSize = FF.getPropertiesSize();
		int expectSize = 1;
		
		assertEquals("Attribute String not equal to what is set", actual, expected);
		assertEquals("Attribute Size incorrect", actual, expected);
	}

	// Test if remove Attribute API works
	public void testRemoveAttribute() {
		FakeSession FF = new FakeSession(1);
		FF.setAttribute("Attr1", "Val1");
		
		Object OutputObj = FF.getAttribute("Attr1");
		String actual = OutputObj.toString();
		String expected = "Val1";
		
		assertEquals("Object Not successfully added", actual, expected);
		
		FF.removeAttribute("Attr1");
		int actualSize = FF.getPropertiesSize();
		int expectSize = 0;
		
		assertEquals("Remove unsuccessfully", actualSize, expectSize);
	}

	public void testIsValid() {
		FakeSession FF = new FakeSession(1);
		
		boolean expected0 = true;
		boolean actual0 = FF.isValid();
		
		assertEquals("Object Initially invalid", actual0, expected0);
		
		FF.invalidate();
		
		boolean expected1 = false;
		boolean actual1 = FF.isValid();
		
		assertEquals("Fail to invalidate", expected1, actual1);
	}

}
