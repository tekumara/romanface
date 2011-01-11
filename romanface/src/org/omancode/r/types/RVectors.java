package org.omancode.r.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.omancode.r.RInterfaceException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPVector;
import org.rosuda.REngine.RList;

/**
 * Static utility class for working with lists and collections of
 * {@link RVector}s.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public final class RVectors {

	private RVectors() {
		// no instantiation
	}

	/**
	 * Create a list of {@link RVector}s from arrays of names and types. Types
	 * that cannot be handled will be silently ignored.
	 * 
	 * Each {@link RVector} will be empty and initialised to {@code numElements}
	 * .
	 * 
	 * @param names
	 *            array of names of the vectors to create
	 * @param types
	 *            array of types of the vectors to create
	 * @param numElements
	 *            initial size of each vector
	 * @return list of empty {@link RVector}s
	 */
	public static List<RVector> createVectorList(String[] names,
			Class<?>[] types, int numElements) {

		// create a List of RVectors that hold an unknown type
		ArrayList<RVector> vectors = new ArrayList<RVector>();

		// create each RVector
		for (int i = 0; i < names.length; i++) {
			Class<?> klass = types[i];

			RVector vector = RVector.create(names[i], klass, numElements);
			if (vector != null) {
				// only add classes we can handle, ignore the rest
				vectors.add(vector);
			}
		}
		return vectors;
	}

	/**
	 * Create a list of {@link RVector}s from list of names and types. Types
	 * that cannot be handled will be silently ignored.
	 * 
	 * Each {@link RVector} will be empty and initialised to {@code numElements}
	 * .
	 * 
	 * @param names
	 *            list of names of the vectors to create
	 * @param types
	 *            list of types of the vectors to create
	 * @param numElements
	 *            initial size of each vector
	 * @return list of empty {@link RVector}s
	 */
	public static List<RVector> createVectorList(List<String> names,
			List<Class<?>> types, int numElements) {

		int numVectors = names.size();
		return createVectorList(names.toArray(new String[numVectors]),
				types.toArray(new Class<?>[numVectors]), numElements);
	}

	/**
	 * Create a list of {@link RVector}s from an {@link RList}.
	 * 
	 * @param rlist
	 *            rlist
	 * @return list of {@link RVector}s.
	 * @throws UnsupportedTypeException
	 *             if {@code rlist} contains a type than cannot be converted to
	 *             an {@link RVector}.
	 * @throws REXPMismatchException
	 *             if problem determining {@code dimnames} attribute, or reading
	 *             a {@link REXP} in the {@link RList}.
	 */
	public static List<RVector> createVectorList(RList rlist)
			throws UnsupportedTypeException, REXPMismatchException {
		ArrayList<RVector> rVectors = new ArrayList<RVector>(rlist.size());

		int index = 0;
		for (Object element : rlist) {

			if (element instanceof REXPVector) {
				REXPVector rexp = (REXPVector) element;

				rVectors.add(new RVector((String) rlist.names.get(index++),
						rexp));
			} else {
				throw new UnsupportedTypeException("rlist contains "
						+ element.getClass().getCanonicalName());
			}

		}

		return rVectors;
	}

	/**
	 * Create an {@link RList} from a collection of {@link RVector}s.
	 * 
	 * @param vectors
	 *            vector collection
	 * @return rlist rlist
	 * @throws RInterfaceException
	 *             if problem
	 */
	public static RList toRList(Collection<RVector> vectors)
			throws RInterfaceException {
		// create an rlist of REXPVectors from each RVector
		RList rlist = new RList(vectors.size(), true);
		for (RVector vector : vectors) {
			try {
				rlist.put(vector.getName(), vector.getREXPVector());
			} catch (UnsupportedTypeException e) {
				throw new RInterfaceException("Cannot get R vector ["
						+ vector.getName() + "]. " + e.getMessage(), e);
			}
		}
		return rlist;
	}

}
