package org.omancode.r.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omancode.r.RFace;
import org.omancode.r.RFaceException;
import org.omancode.r.types.RVector;
import org.omancode.r.types.UnsupportedTypeException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPVector;

public class RVectorTest {

	private static RFace rInterface;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			rInterface = RFace.getInstance(null);
		} catch (RFaceException e) {
			System.err.println(e.getMessage());
			System.err.println("Check: ");
			System.err
					.println("1) the location of jri.dll is specified, eg: -Djava.library.path=\"C:\\Program Files\\R\\R-2.11.1\\library\\rJava\\jri\"");
			System.err
					.println("2) R bin dir is on the path, eg: PATH=%PATH%;C:\\Program Files\\R\\R-2.11.1\\bin");
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testAsMap() throws RFaceException, UnsupportedTypeException,
			REXPMismatchException {
		REXP rexp = rInterface.eval("c('key1'='A','key2'='B')");
		RVector vardesc = new RVector("vardesc", (REXPVector) rexp);

		Map<String, ?> map = vardesc.asMap();

		String[] values = map.values().toArray(new String[0]);
		String[] keys = map.keySet().toArray(new String[0]);
		Arrays.sort(values);
		Arrays.sort(keys);

		assertArrayEquals(new String[] { "A", "B" }, values);
		assertArrayEquals(new String[] { "key1", "key2" }, keys);

	}

}
