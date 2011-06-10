package org.omancode.r.types;

import java.util.Arrays;

import org.omancode.r.RFaceException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;

/**
 * Static utility class for getting attributes of {@link REXP}s.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public final class REXPAttr {

	private REXPAttr() {
		// no instantiation
	}

	/**
	 * Returns the R class attribute of rexp.
	 * 
	 * @param rexp
	 *            expression to test
	 * @return a comma separated string of the rexp classes, eg:
	 *         {@code "[xtabs, table]"}. If it has no class attribute, returns
	 *         the name of the java class instead (eg: {@code REXPDouble}).
	 */
	public static String getClassAttribute(REXP rexp) {

		REXPString classAttribute = (REXPString) rexp.getAttribute("class");

		String[] clazz = null;
		if (classAttribute == null) {
			// If the object does not have a class attribute,
			// it has an implicit class, "matrix", "array" or the
			// result of mode(x) (except that integer vectors have implicit
			// class "integer")

			// return the java type instead
			clazz = new String[] { rexp.getClass().getSimpleName() };
		} else {
			clazz = classAttribute.asStrings();
		}

		return Arrays.toString(clazz);
	}

	/**
	 * Get the {@code names} attribute of rexp. Like the R function
	 * {@code names}, this will return {@code dimnames[[1]]} for a
	 * one-dimensional array.
	 * 
	 * @param rexp
	 *            r expression
	 * @return names attribute
	 * @throws RFaceException
	 *             if problem determining {@code dimnames} attribute.
	 */
	public static String[] getNamesAttribute(REXP rexp) throws RFaceException {

		REXPString namesAttribute = (REXPString) rexp.getAttribute("names");

		String[] names = null;
		if (namesAttribute != null) {
			names = namesAttribute.asStrings();
		} else if (getDimensions(rexp) == 1) {
			names = getDimNames(rexp)[0];
		}

		return names;
	}

	/**
	 * Get dimension (dim) attribute from rexp.
	 * 
	 * @param rexp
	 *            rexp
	 * @return number of dimensions
	 */
	public static int getDimensions(REXP rexp) {
		int[] dims = rexp.dim();
		return dims == null ? 0 : dims.length;
	}

	/**
	 * Return the {@code names} attribute of the {@code dimnames} attribute.
	 * 
	 * @param rexp
	 *            rexp
	 * @return {@code names} of {@code dimnames}, or {@code null} if there is no
	 *         {@code names} attribute.
	 */
	public static String[] getNamesDimNames(REXP rexp) {

		REXP dimNames = rexp.getAttribute("dimnames");

		if (dimNames == null) {
			return null;
		}

		REXPString namesDimNames =
				(REXPString) dimNames.getAttribute("names");

		if (namesDimNames == null) {
			return null;
		}

		return namesDimNames.asStrings();

	}

	/**
	 * Return the {@code dimnames} attribute.
	 * 
	 * @param rexp
	 *            rexp
	 * @return {@code dimnames}or {@code null} if there is no {@code dimnames}
	 *         attribute.
	 * @throws RFaceException
	 *             if problem determining {@code dimnames} attribute.
	 */
	public static String[][] getDimNames(REXP rexp) throws RFaceException {

		REXPGenericVector dimNames =
				(REXPGenericVector) rexp.getAttribute("dimnames");

		if (dimNames == null) {
			return null;
		}

		RList dimNamesList = dimNames.asList();

		int numDims = dimNamesList.size();

		@SuppressWarnings("unchecked")
		Object[] dimNamesElements = dimNamesList.toArray(new Object[numDims]);

		String[][] dimNamesAsStrings = new String[numDims][];

		for (int i = 0; i < numDims; i++) {
			Object dnelement = dimNamesElements[i];

			if (dnelement instanceof REXPNull) {
				dimNamesAsStrings[i] = null;
			} else if (dnelement instanceof REXPString) {
				dimNamesAsStrings[i] = ((REXPString) dnelement).asStrings();
			} else {
				throw new RFaceException(rexp,
						" has a dimnames element that is not "
								+ "a REXPNull or REXPString");
			}

		}

		return dimNamesAsStrings;

	}
}
