package org.omancode.r.types;

import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import net.casper.data.model.CDataCacheContainer;
import net.casper.data.model.CDataGridException;
import net.casper.data.model.CDataRowSet;
import net.casper.data.model.CDataRuntimeException;
import net.casper.data.model.CMarkedUpRow;
import net.casper.data.model.CMarkedUpRowBean;
import net.casper.data.model.CRowMetaData;

import org.omancode.r.RFaceException;
import org.omancode.util.beans.BeanPropertyInspector;
import org.omancode.util.beans.BeanUtil;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPVector;
import org.rosuda.REngine.RList;

/**
 * A list of {@link RVector}s. Can be created from POJO collections, an
 * {@link RList} or Casper dataset. Provides method to convert to an
 * {@link RList} for use with R.
 * 
 * Used to represent POJO collections or Casper datasets in R.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public class RVectorList implements List<RVector> {

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
	public RVectorList(Collection<?> col, Class<?> stopClass)
			throws IntrospectionException {
		this(col, new BeanPropertyInspector(col.iterator().next(), stopClass));
	}

	private RVectorList(Collection<?> col, BeanPropertyInspector props)
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
	 * @throws RFaceException
	 *             if problem
	 */
	public RVectorList(CDataCacheContainer container) throws RFaceException {
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
			throw new RFaceException(e);
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
	public RVectorList(List<String> names, List<Class<?>> types,
			int numElements) {
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
	public RVectorList(String[] names, Class<?>[] types, int numElements) {
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
	public RVectorList(RList rlist) throws UnsupportedTypeException,
			REXPMismatchException {
		vectors = new ArrayList<RVector>(rlist.size());

		int index = 0;
		for (Object element : rlist) {

			if (element instanceof REXPVector) {
				REXPVector rexp = (REXPVector) element;

				vectors.add(new RVector((String) rlist.names.get(index++),
						rexp));
			} else {
				throw new UnsupportedTypeException("rlist contains "
						+ element.getClass().getCanonicalName());
			}

		}
	}

	/**
	 * Add the columns in a {@link CMarkedUpRow} as vectors to this
	 * {@link RVectorList}. The additional columns/vectors are filled by the
	 * {@link CMarkedUpRow}s retrieved from a collection of
	 * {@link CMarkedUpRowBean}s.
	 * 
	 * If a {@link CMarkedUpRow} is not returned by the first
	 * {@link CMarkedUpRowBean} then this method will silently exit.
	 * 
	 * @param col
	 *            collection of beans that expose a {@link CMarkedUpRow}.
	 * @return this {@link RVectorList}. Will now contain the added
	 *         columns/vectors.
	 * @throws IntrospectionException
	 *             if problem reading collection.
	 */
	public RVectorList addCMarkedUpRow(
			Collection<? extends CMarkedUpRowBean> col)
			throws IntrospectionException {

		if (col.size() != vectors.get(0).size()) {
			throw new IllegalArgumentException(
					"Size of CMarkedUpRowBean collection does not match "
							+ "existing vector size");
		}

		CMarkedUpRowBean cMarkedUpRowBean = col.iterator().next();
		CMarkedUpRow firstRow = cMarkedUpRowBean.getMarkedUpRow();

		if (firstRow == null) {
			// exit silently
			return this;
		}

		// get list of vectors, one for each column in the row
		// the vectors will be empty
		CRowMetaData meta = firstRow.getMetaDefinition();
		List<RVector> rowVectors =
				createEmptyVectors(meta.getColumnNames(),
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
	 * @throws RFaceException
	 *             if vectors cannot be converted to {@link REXPVector}s.
	 */
	public RList asRList() throws RFaceException {
		// create an rlist of REXPVectors from each RVector
		RList rlist = new RList(vectors.size(), true);
		for (RVector vector : vectors) {
			try {
				rlist.put(vector.getName(), vector.getREXPVector());
			} catch (UnsupportedTypeException e) {
				throw new RFaceException("Cannot get R vector ["
						+ vector.getName() + "]. " + e.getMessage(), e);
			}
		}
		return rlist;
	}

	/**
	 * Return a new R dataframe from this list of {@link RVector}s.
	 * 
	 * @return rlist rlist
	 * @throws RFaceException
	 *             if vectors are empty or cannot be converted to
	 *             {@link REXPVector}s.
	 */
	public REXP asDataFrame() throws RFaceException {
		try {
			return REXP.createDataFrame(asRList());
		} catch (REXPMismatchException e) {
			throw new RFaceException(e.getMessage(), e);
		}
	}

	@Override
	public int size() {
		return vectors.size();
	}

	@Override
	public boolean isEmpty() {
		return vectors.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return vectors.contains(o);
	}

	@Override
	public Iterator<RVector> iterator() {
		return vectors.iterator();
	}

	@Override
	public Object[] toArray() {
		return vectors.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return vectors.toArray(a);
	}

	@Override
	public boolean add(RVector e) {
		return vectors.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return vectors.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return vectors.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends RVector> c) {
		return vectors.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends RVector> c) {
		return vectors.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return vectors.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return vectors.retainAll(c);
	}

	@Override
	public void clear() {
		vectors.clear();
	}

	@Override
	public RVector get(int index) {
		return vectors.get(index);
	}

	@Override
	public RVector set(int index, RVector element) {
		return vectors.set(index, element);
	}

	@Override
	public void add(int index, RVector element) {
		vectors.add(index, element);
	}

	@Override
	public RVector remove(int index) {
		return vectors.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return vectors.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return vectors.lastIndexOf(o);
	}

	@Override
	public ListIterator<RVector> listIterator() {
		return vectors.listIterator();
	}

	@Override
	public ListIterator<RVector> listIterator(int index) {
		return vectors.listIterator(index);
	}

	@Override
	public List<RVector> subList(int fromIndex, int toIndex) {
		return vectors.subList(fromIndex, toIndex);
	}
}
