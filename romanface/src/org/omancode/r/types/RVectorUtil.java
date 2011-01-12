package org.omancode.r.types;

import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.casper.data.model.CDataGridException;
import net.casper.data.model.CMarkedUpRow;
import net.casper.data.model.CRowMetaData;
import net.casper.ext.CMarkedUpRowBean;
import net.casper.ext.swing.CDataRuntimeException;

import org.omancode.util.beans.BeanPropertyInspector;
import org.omancode.util.beans.BeanUtil;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPVector;
import org.rosuda.REngine.RList;

/**
 * Static utility class for creating lists of {@link RVector}s.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public final class RVectorUtil {

	private RVectorUtil() {
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
	 * Create an list of {@link RVector}s from the given {@link Collection}.
	 * 
	 * Introspection is used to determine the bean properties (i.e.: getter
	 * methods) that are exposed, and each one becomes a vector. Vectors are
	 * only created for primitive properties and arrays of primitive properties;
	 * object properties are ignored without warning.
	 * 
	 * NB: doesn't automatically create factors for vectors like read.table
	 * does.
	 * 
	 * @param col
	 *            the Java collection to convert.
	 * @param stopClass
	 *            Columns are created for all getter methods that are defined by
	 *            {@code stopClass}'s subclasses. {@code stopClass}'s getter
	 *            methods and superclass getter methods are not converted to
	 *            columns in the dataframe.
	 * @throws IntrospectionException
	 *             if problem reading properties of the collection
	 */
	public static List<RVector> createVectorList(Collection<?> col,
			Class<?> stopClass) throws IntrospectionException {

		// get bean properties
		Object bean = col.iterator().next();
		BeanPropertyInspector props = new BeanPropertyInspector(bean, stopClass);

		// create a List of RVectors from the properties
		List<RVector> vectors = createVectorList(props.getNames(),
				props.getTypes(), col.size());

		// fill the RVectors' values row by row
		// from the bean's property values
		for (Object element : col) {
			for (RVector vector : vectors) {
				String propName = vector.getName();
				Object prop = BeanUtil.getProperty(element, propName);
				vector.addValue(prop);
			}
		}

		return vectors;
	}

	/**
	 * Create an list of {@link RVector}s from the given {@link Collection} of
	 * {@link CMarkedUpRowBean}s.
	 * 
	 * Uses {@link RVectorUtil#createVectorList(Collection, Class)} to create a
	 * list of {@link RVector}s and then creates additional vectors for each
	 * column in the {@link CMarkedUpRow} exposed by the beans.
	 * 
	 * NB: doesn't automatically create factors for vectors like read.table
	 * does.
	 * 
	 * @param col
	 *            the Java collection to convert.
	 * @param stopClass
	 *            Columns are created for all getter methods that are defined by
	 *            {@code stopClass}'s subclasses. {@code stopClass}'s getter
	 *            methods and superclass getter methods are not converted to
	 *            columns in the dataframe.
	 * @throws IntrospectionException
	 *             if problem reading properties of the collection
	 * @return {@link RList}
	 */
	public static List<RVector> createVectorListMarkedUp(
			Collection<? extends CMarkedUpRowBean> col, Class<?> stopClass)
			throws IntrospectionException {

		List<RVector> vectors = createVectorList(col, stopClass);

		// extract the vectors from the CMarkedUpRow
		List<RVector> rowProps = extractMarkedRow(col);
		vectors.addAll(rowProps);

		return vectors;
	}

	/**
	 * Extract the values of a {@link CMarkedUpRow} that is retrieved via a
	 * getter method on a collection of beans.
	 * 
	 * @param col
	 *            collection of beans that expose a {@link CMarkedUpRow}.
	 * @param markedUpRowGetterName
	 *            getter method used on the bean to retrieve the
	 *            {@link CMarkedUpRow}.
	 * @return list of {@link RVector}s. One for each value in the
	 *         {@link CMarkedUpRow}.
	 * @throws IntrospectionException
	 *             if problem reading collection.
	 */
	private static List<RVector> extractMarkedRow(
			Collection<? extends CMarkedUpRowBean> col)
			throws IntrospectionException {

		CMarkedUpRowBean cMarkedUpRowBean = col.iterator().next();
		CMarkedUpRow firstRow = cMarkedUpRowBean.getMarkedUpRow();

		// get list of vectors, one for each column in the row
		// the vectors will be empty
		CRowMetaData meta = firstRow.getMetaDefinition();
		List<RVector> vectors = createVectorList(meta.getColumnNames(),
				meta.getColumnTypes(), col.size());

		// fill the RVectors' values row by row
		// from the bean's property values
		for (CMarkedUpRowBean element : col) {
			CMarkedUpRow row = element.getMarkedUpRow();
			for (RVector vector : vectors) {
				String propName = vector.getName();
				Object prop;
				try {
					prop = row.getObject(propName);
				} catch (CDataGridException e) {
					throw new CDataRuntimeException(e);
				}
				vector.addValue(prop);
			}
		}

		return vectors;

	}

}
