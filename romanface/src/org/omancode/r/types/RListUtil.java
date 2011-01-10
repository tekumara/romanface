package org.omancode.r.types;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.casper.data.model.CDataCacheContainer;
import net.casper.data.model.CDataGridException;
import net.casper.data.model.CDataRowSet;
import net.casper.data.model.CRowMetaData;
import net.casper.io.beans.CMarkedUpRow;
import net.casper.io.beans.CMarkedUpRowBean;
import net.casper.io.beans.util.BeanPropertyInspector;

import org.apache.commons.beanutils.PropertyUtils;
import org.omancode.r.RInterfaceException;
import org.rosuda.REngine.RList;

/**
 * Static utility class for converting Java collections and casper datasets to
 * {@link RList}s.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public final class RListUtil {

	private RListUtil() {
		// no instantiation
	}

	/**
	 * Get bean property. Return reflection errors as
	 * {@link RInterfaceException}s.
	 * 
	 * @param bean
	 *            Bean whose property is to be extracted
	 * @param name
	 *            Possibly indexed and/or nested name of the property to be
	 *            extracted
	 * @return the property value
	 * @throws RInterfaceException
	 *             if reflection problem getting property
	 */
	private static Object getProperty(Object bean, String name)
			throws RInterfaceException {
		Object prop;
		try {
			prop = PropertyUtils.getProperty(bean, name);
		} catch (IllegalAccessException e) {
			throw new RInterfaceException("Oh no, " + "couldn't get property ["
					+ name + "]", e);
		} catch (InvocationTargetException e) {
			throw new RInterfaceException("Oh no, " + "couldn't get property ["
					+ name + "]", e);
		} catch (NoSuchMethodException e) {
			throw new RInterfaceException("Oh no, " + "couldn't get property ["
					+ name + "]", e);
		}
		return prop;

	}

	/**
	 * Create an {@link RList} from a {@link CDataCacheContainer}.
	 * 
	 * NB: doesn't automatically create factors like read.table does.
	 * 
	 * @param container
	 *            casper container
	 * @return rlist rlist
	 * @throws RInterfaceException
	 *             if problem
	 */
	public static RList toRList(CDataCacheContainer container)
			throws RInterfaceException {

		if (container.size() == 0) {
			throw new RInterfaceException(
					"Cannot create RList from empty casper container \""
							+ container.getCacheName() + "\"");
		}

		// create an RVector for each column
		List<RVector> vectors = getNewRVectors(container.getMetaDefinition(),
				container.size());

		if (vectors.isEmpty()) {
			throw new RInterfaceException(
					"Container does not contain any columns "
							+ "that can be converted to a RList");
		}

		// fill the RVectors' values row by row
		// from the columns
		try {
			CDataRowSet cdrs = container.getAll();
			while (cdrs.next()) {
				for (RVector vector : vectors) {
					String propName = vector.getName();
					Object prop = cdrs.getObject(propName);
					vector.addValue(prop);
				}
			}
		} catch (CDataGridException e) {
			throw new RInterfaceException(e);
		}

		return toRList(vectors);
	}

	/**
	 * Create an {@link RList} from the given Collection. Introspection is used
	 * to determine the bean properties (i.e.: getter methods) that are exposed,
	 * and each one becomes a column in the dataframe. Columns are only created
	 * for primitive properties and arrays of primitive properties; object
	 * properties are ignored without warning.
	 * 
	 * If one of the properties is a {@link CMarkedUpRow} then this will be
	 * extracted and included in its entirety.
	 * 
	 * NB: doesn't automatically create factors like read.table does.
	 * 
	 * @param col
	 *            the Java collection to convert.
	 * @param stopClass
	 *            Columns are created for all getter methods that are defined by
	 *            {@code stopClass}'s subclasses. {@code stopClass}'s getter
	 *            methods and superclass getter methods are not converted to
	 *            columns in the dataframe.
	 * @return rlist rlist
	 * @throws RInterfaceException
	 *             if Collection cannot be read, or dataframe cannot be created.
	 */
	public static RList toRList(Collection<?> col, Class<?> stopClass)
			throws RInterfaceException {

		if (col.isEmpty()) {
			throw new RInterfaceException(
					"Cannot create RList for empty collection");
		}

		Object bean = col.iterator().next();

		BeanPropertyInspector props;
		try {
			props = new BeanPropertyInspector(bean, stopClass);
		} catch (IntrospectionException e) {
			throw new RInterfaceException(e);
		}

		int numElements = col.size();

		// create a List of RVectors that hold an unknown type
		ArrayList<RVector> vectors = new ArrayList<RVector>();

		// create an RVector for each property
		// only properties of primitive types are included
		for (BeanPropertyInspector.Property prop : props) {
			Class<?> klass = prop.getPropertyType();

			RVector vector = RVector.create(prop.getName(), klass, numElements);
			if (vector != null) {
				// only add classes we can handle, ignore the rest
				vectors.add(vector);
			}
		}

		// fill the RVectors' values row by row
		// from the bean's property values
		for (Object element : col) {
			for (RVector vector : vectors) {
				String propName = vector.getName();
				Object prop = getProperty(element, propName);
				vector.addValue(prop);
			}
		}

		String markedUpRowGetterName = props.getGetMarkedUpRowMethodName();
		if (markedUpRowGetterName != null) {
			List<RVector> rowProps = extractMarkedRow(col,
					markedUpRowGetterName);
			vectors.addAll(rowProps);
		}

		if (vectors.isEmpty()) {
			throw new RInterfaceException(
					"Collection does not contain any properties "
							+ "that can be converted to a dataframe");
		}

		return toRList(vectors);

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
	 * @throws RInterfaceException
	 *             if problem reading collection.
	 */
	private static List<RVector> extractMarkedRow(Collection<?> col,
			String markedUpRowGetterName) throws RInterfaceException {

		if (col.isEmpty()) {
			throw new RInterfaceException("Empty collection of "
					+ CMarkedUpRowBean.class);
		}

		Object cMarkedUpRowBean = col.iterator().next();
		CMarkedUpRow firstRow = (CMarkedUpRow) getProperty(cMarkedUpRowBean,
				markedUpRowGetterName);

		// get list of vectors, one for each column in the row
		// the vectors will be empty
		List<RVector> vectors = getNewRVectors(firstRow.getMetaDefinition(),
				col.size());

		if (vectors.isEmpty()) {
			throw new RInterfaceException(
					"Container does not contain any columns "
							+ "that can be converted to a RList");
		}

		// fill the RVectors' values row by row
		// from the bean's property values
		for (Object element : col) {
			for (RVector vector : vectors) {
				String propName = vector.getName();
				CMarkedUpRow row = (CMarkedUpRow) getProperty(element,
						markedUpRowGetterName);
				Object prop;
				try {
					prop = row.getObject(propName);
				} catch (CDataGridException e) {
					throw new RInterfaceException(e);
				}
				vector.addValue(prop);
			}
		}

		return vectors;

	}

	/**
	 * Generate a list of {@link RVector}s, one for each column in the meta
	 * data. Each {@link RVector} will be empty and initialised to the specified
	 * size.
	 * 
	 * @param meta
	 *            meta data
	 * @param numElements
	 *            initial size of each vector
	 * @return list of empty {@link RVector}s
	 * @throws RInterfaceException
	 *             if problem
	 */
	private static List<RVector> getNewRVectors(CRowMetaData meta,
			int numElements) throws RInterfaceException {
		String[] columnNames = meta.getColumnNames();
		Class<?>[] columnTypes = meta.getColumnTypes();

		// create a List of RVectors that hold an unknown type
		ArrayList<RVector> vectors = new ArrayList<RVector>();

		// create an RVector for each column
		for (int i = 0; i < columnNames.length; i++) {
			Class<?> klass = columnTypes[i];

			RVector vector = RVector.create(columnNames[i], klass, numElements);
			if (vector != null) {
				// only add classes we can handle, ignore the rest
				vectors.add(vector);
			}
		}
		return vectors;
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
	private static RList toRList(Collection<RVector> vectors)
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
