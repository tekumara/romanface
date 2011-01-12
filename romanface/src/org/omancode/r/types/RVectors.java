package org.omancode.r.types;

import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.casper.data.model.CDataCacheContainer;
import net.casper.data.model.CDataGridException;
import net.casper.data.model.CDataRowSet;
import net.casper.data.model.CMarkedUpRow;
import net.casper.data.model.CRowMetaData;
import net.casper.ext.CMarkedUpRowBean;
import net.casper.ext.swing.CDataRuntimeException;

import org.omancode.r.RInterfaceException;
import org.omancode.util.beans.BeanPropertyInspector;
import org.omancode.util.beans.BeanUtil;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPVector;
import org.rosuda.REngine.RList;

/**
 * A list of {@link RVector)s. Can be created from POJO collections, an
 * {@link RList} or Casper dataset. Can return an {@link RList} representation.
 * 
 * Used to represent POJO collections or Casper datasets in R.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public class RVectors {

	private final List<RVector> vectors;

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
	public RVectors(Collection<?> col, Class<?> stopClass)
			throws IntrospectionException {
		this(col, new BeanPropertyInspector(col.iterator().next(), stopClass));
	}

	private RVectors(Collection<?> col, BeanPropertyInspector props)
			throws IntrospectionException {

		// create a empty list of RVectors from the properties
		this(props.getNames(), props.getTypes(), col.size());

		// fill the RVectors' values row by row
		// from the bean's property values
		for (Object element : col) {
			for (RVector vector : vectors) {
				String propName = vector.getName();
				Object prop = BeanUtil.getProperty(element, propName);
				vector.addValue(prop);
			}
		}
	}

	/**
	 * Create an list of {@link RVector}s from a {@link CDataCacheContainer}.
	 * 
	 * NB: doesn't automatically create factors like read.table does.
	 * 
	 * TODO: implement as a CExporter, but any advantages?
	 * 
	 * @param container
	 *            casper container
	 * @throws RInterfaceException
	 *             if problem
	 */
	public RVectors(CDataCacheContainer container) throws RInterfaceException {
		// create an RVector for each column
		this(container.getMetaDefinition().getColumnNames(), container
				.getMetaDefinition().getColumnTypes(), container.size());

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
	}

	/**
	 * Create a list of empty {@link RVector}s from list of names and types.
	 * Types that cannot be handled will be silently ignored.
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
	 */
	public RVectors(List<String> names, List<Class<?>> types, int numElements) {
		this(names.toArray(new String[names.size()]), types
				.toArray(new Class<?>[names.size()]), numElements);
	}

	/**
	 * Create a list of empty {@link RVector}s from arrays of names and types.
	 * Types that cannot be handled will be silently ignored.
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
	 */
	public RVectors(String[] names, Class<?>[] types, int numElements) {
		vectors = createEmptyVectors(names, types, numElements);
	}

	/**
	 * Create a list of empty {@link RVector}s from arrays of names and types.
	 * Types that cannot be handled will be silently ignored.
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
	 */
	private static List<RVector> createEmptyVectors(String[] names,
			Class<?>[] types, int numElements) {

		// create a List of RVectors that hold an unknown type
		List<RVector> emptyVectors = new ArrayList<RVector>();

		// create each RVector
		for (int i = 0; i < names.length; i++) {
			Class<?> klass = types[i];

			RVector vector = RVector.create(names[i], klass, numElements);
			if (vector != null) {
				// only add classes we can handle, ignore the rest
				emptyVectors.add(vector);
			}
		}

		return emptyVectors;
	}

	/**
	 * Create a list of {@link RVector}s from an {@link RList}.
	 * 
	 * @param rlist
	 *            rlist
	 * @throws UnsupportedTypeException
	 *             if {@code rlist} contains a type than cannot be converted to
	 *             an {@link RVector}.
	 * @throws REXPMismatchException
	 *             if problem determining {@code dimnames} attribute, or reading
	 *             a {@link REXP} in the {@link RList}.
	 */
	public RVectors(RList rlist) throws UnsupportedTypeException,
			REXPMismatchException {
		vectors = new ArrayList<RVector>(rlist.size());

		int index = 0;
		for (Object element : rlist) {

			if (element instanceof REXPVector) {
				REXPVector rexp = (REXPVector) element;

				vectors.add(new RVector((String) rlist.names.get(index++), rexp));
			} else {
				throw new UnsupportedTypeException("rlist contains "
						+ element.getClass().getCanonicalName());
			}

		}
	}

	/**
	 * Add the columns in a {@link CMarkedUpRow} as vectors to this
	 * {@link RVectors}. The additional columns/vectors are filled by the
	 * {@link CMarkedUpRow}s retrieved from a collection of
	 * {@link CMarkedUpRowBean}s.
	 * 
	 * @param col
	 *            collection of beans that expose a {@link CMarkedUpRow}.
	 * @return this {@link RVectors}. Will now contain the added
	 *         columns/vectors.
	 * @throws IntrospectionException
	 *             if problem reading collection.
	 */
	public RVectors addCMarkedUpRow(Collection<? extends CMarkedUpRowBean> col)
			throws IntrospectionException {

		if (col.size() != vectors.get(0).size()) {
			throw new IllegalArgumentException(
					"Size of CMarkedUpRowBean collection does not match "
							+ "existing vector size");
		}

		CMarkedUpRowBean cMarkedUpRowBean = col.iterator().next();
		CMarkedUpRow firstRow = cMarkedUpRowBean.getMarkedUpRow();

		// get list of vectors, one for each column in the row
		// the vectors will be empty
		CRowMetaData meta = firstRow.getMetaDefinition();
		List<RVector> rowVectors = createEmptyVectors(meta.getColumnNames(),
				meta.getColumnTypes(), col.size());

		// fill the RVectors' values row by row
		// from the bean's property values
		for (CMarkedUpRowBean element : col) {
			CMarkedUpRow row = element.getMarkedUpRow();
			for (RVector vector : rowVectors) {
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

		vectors.addAll(rowVectors);
		return this;
	}

	/**
	 * Return a new {@link RList} from this list of {@link RVector}s.
	 * 
	 * @return rlist rlist
	 * @throws RInterfaceException
	 *             if problem
	 */
	public RList asRList() throws RInterfaceException {
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
