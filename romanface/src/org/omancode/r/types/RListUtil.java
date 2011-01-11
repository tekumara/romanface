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

import org.omancode.r.RInterfaceException;
import org.omancode.util.beans.BeanPropertyInspector;
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
	public static RList toRList(CDataCacheContainer container)
			throws RInterfaceException {

		if (container.size() == 0) {
			throw new RInterfaceException(
					"Cannot create RList from empty casper container \""
							+ container.getCacheName() + "\"");
		}

		// create an RVector for each column
		CRowMetaData meta = container.getMetaDefinition();
		List<RVector> vectors = RVectors.createVectorList(
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

		return RVectors.toRList(vectors);
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
		
		try {
			return new CMarkedUpRowBeanCollectionToR(col, stopClass).createRList();
		} catch (IntrospectionException e) {
			throw new RInterfaceException(e);
		}
	}
}
