package org.omancode.r.types;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REXPVector;

/**
 * Static utility class for working with {@link REXP}s.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public final class REXPUtil {

	private REXPUtil() {
		// no instantiation
	}

	/**
	 * Convert array to {@link REXPVector}.
	 * 
	 * @param array
	 *            array to convert
	 * @return {@link REXPVector}.
	 */
	public static REXPVector toVector(double[] array) {
		return new REXPDouble(array);
	}

	/**
	 * Convert array to {@link REXPVector}.
	 * 
	 * @param array
	 *            array to convert
	 * @return {@link REXPVector}.
	 */
	public static REXPVector toVector(int[] array) {
		return new REXPInteger(array);
	}

	/**
	 * Convert array to {@link REXPVector}.
	 * 
	 * @param array
	 *            array to convert
	 * @return {@link REXPVector}.
	 */
	public static REXPVector toVector(boolean[] array) {
		return new REXPLogical(array);
	}

	/**
	 * Convert array to {@link REXPVector}.
	 * 
	 * @param array
	 *            array to convert
	 * @return {@link REXPVector}.
	 */
	public static REXPVector toVector(String[] array) {
		return new REXPString(array);
	}

	/**
	 * Convert array object to {@link REXPVector}, or {@link REXPNull} if null.
	 * 
	 * @param array
	 *            object that is null or an array
	 * @return if object is null, returns {@link REXPNull}, otherwise if object
	 *         is a primitive array returns an {@link REXPVector}, otherwise
	 *         throws an {@link IllegalArgumentException}.
	 */
	public static REXP toVector(Object array) {
		if (array == null) {
			return new REXPNull();
		}
	
		Class<?> arrayClass = array.getClass();
	
		if (arrayClass == double[].class) {
			return new REXPDouble((double[]) array);
		} else if (arrayClass == int[].class) {
			return new REXPInteger((int[]) array);
		} else if (arrayClass == String[].class) {
			return new REXPString((String[]) array);
		} else if (arrayClass == boolean[].class) {
			return new REXPLogical((boolean[]) array);
		} else {
			throw new IllegalArgumentException("Cannot convert "
					+ arrayClass.getCanonicalName() + " to R object");
		}
	}

	/**
	 * Convert Object to REXP.
	 * 
	 * @param value
	 *            object
	 * @return REXP
	 */
	public static REXP toREXP(Object value) {
	
		if (value == null) {
			return new REXPNull();
		} else if (value instanceof Double) {
			return new REXPDouble((Double) value);
		} else if (value instanceof Integer) {
			return new REXPInteger((Integer) value);
		} else if (value instanceof String) {
			return new REXPString((String) value);
		} else if (value instanceof Boolean) {
			return new REXPLogical((Boolean) value);
		} else if (value instanceof Character) {
			return new REXPString(((Character) value).toString());
		} else {
			throw new IllegalArgumentException("Cannot convert "
					+ value.getClass().getCanonicalName() + " to R object");
		}
	
	}
	
}
