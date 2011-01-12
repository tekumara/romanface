package org.omancode.r.types;

import java.beans.IntrospectionException;
import java.util.Collection;
import java.util.List;

import net.casper.data.model.CDataCacheContainer;
import net.casper.data.model.CDataGridException;
import net.casper.data.model.CDataRowSet;
import net.casper.data.model.CRowMetaData;
import net.casper.ext.CMarkedUpRowBean;

import org.omancode.r.RInterfaceException;
import org.rosuda.REngine.RList;

/**
 * Static utility class for creating {@link RList}s from Java collections and
 * casper datasets.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public final class RListUtil {

	private RListUtil() {
		// no instantiation
	}

	/**
	 * Create an {@link RList} from a {@link CDataCacheContainer}.
	 * 
	 * NB: doesn't automatically create factors like read.table does. TODO:
	 * implement as a CExporter, but any advantages?
	 * 
	 * @param container
	 *            casper container
	 * @return rlist rlist
	 * @throws RInterfaceException
	 *             if problem
	 */
	public static RList createRList(CDataCacheContainer container)
			throws RInterfaceException {

		if (container.size() == 0) {
			throw new RInterfaceException(
					"Cannot create RList from empty casper container \""
							+ container.getCacheName() + "\"");
		}

		// create an RVector for each column
		CRowMetaData meta = container.getMetaDefinition();
		List<RVector> vectors = RVectorUtil.createVectorList(
				meta.getColumnNames(), meta.getColumnTypes(), container.size());

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

		return RListUtil.createRList(vectors);
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
	public static RList createRList(Collection<RVector> vectors)
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

	/**
	 * Create an {@link RList} from the given {@link Collection} of
	 * {@link CMarkedUpRowBean}s.
	 * 
	 * Uses {@link RVectorUtil#createVectorList(Collection, Class)} to create a
	 * list of {@link RVector}s which are then converted to a {@link RList}.
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
	 * @throws RInterfaceException
	 *             if cannot convert vectors to an {@link RList}.
	 * @return {@link RList}
	 */
	public static RList createRList(Collection<?> col, Class<?> stopClass)
			throws RInterfaceException, IntrospectionException {

		List<RVector> vectors = RVectorUtil.createVectorList(col, stopClass);

		return RListUtil.createRList(vectors);
	}

	/**
	 * Create an {@link RList} from the given {@link Collection} of
	 * {@link CMarkedUpRowBean}s.
	 * 
	 * Uses {@link RVectorUtil#createVectorListMarkedUp(Collection, Class)} to
	 * create a list of {@link RVector}s which are then converted to a
	 * {@link RList}.
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
	 * @throws RInterfaceException
	 *             if cannot convert vectors to an {@link RList}.
	 * @return {@link RList}
	 */
	public static RList createRListMarkedUp(
			Collection<? extends CMarkedUpRowBean> col, Class<?> stopClass)
			throws RInterfaceException, IntrospectionException {

		List<RVector> vectors = RVectorUtil.createVectorListMarkedUp(col,
				stopClass);

		return RListUtil.createRList(vectors);
	}
}
