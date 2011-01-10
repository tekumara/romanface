package org.omancode.r;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Static utility class of R related functions.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public final class RUtil {

	private RUtil() {
		// no instantiation
	}

	/**
	 * Returns contents of an R file. Removes "\r" in the string, because R
	 * doesn't like them.
	 * 
	 * @param file
	 *            text file
	 * @return contents of text file with "\r" removed
	 * @throws IOException
	 *             if file cannot be read.
	 */
	public static String readRFile(File file) throws IOException {
		FileReader reader = new FileReader(file);
		String expr = IOUtils.toString(reader);

		// release file after loading,
		// instead of waiting for VM exit/garbage collection
		reader.close();

		// strip "\r" otherwise we will get parse errors
		return StringUtils.remove(expr, "\r");
	}

	/**
	 * Returns contents of an {@link InputStream}. Removes "\r" in the string,
	 * because R doesn't like them.
	 * 
	 * @param stream
	 *            stream
	 * @return contents of text file with "\r" removed
	 * @throws IOException
	 *             if file cannot be read.
	 */
	public static String readRStream(InputStream stream) throws IOException {
		String expr = IOUtils.toString(stream);

		// release file after loading,
		// instead of waiting for VM exit/garbage collection
		stream.close();

		// strip "\r" otherwise we will get parse errors
		return StringUtils.remove(expr, "\r");
	}

	/**
	 * Returns a string representing the boolean value in R.
	 * 
	 * @param bool
	 *            boolean
	 * @return "TRUE", or "FALSE"
	 */
	public static String rBoolean(boolean bool) {
		return bool ? "TRUE" : "FALSE";
	}

	/**
	 * Returns a vector expression string, eg: {@code c("a","b","c")} from the
	 * supplied strings.
	 * 
	 * @param strs
	 *            string array to turn into vector expression
	 * @return vector expression
	 */
	public static String toVectorExprString(String[] strs) {
		if (strs == null) {
			return "c()";
		}

		StringBuffer sb = new StringBuffer();
		sb.append("c(");

		int i = 0;
		for (; i < strs.length - 1; i++) {
			sb.append('\"');
			sb.append(strs[i]);
			sb.append("\",");
		}

		sb.append('\"');
		sb.append(strs[i]);
		sb.append("\")");

		return sb.toString();
	}
}
