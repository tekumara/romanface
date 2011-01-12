package org.omancode.r.types;

import java.beans.IntrospectionException;
import java.util.Collection;
import java.util.List;

import net.casper.data.model.CDataGridException;
import net.casper.data.model.CMarkedUpRow;
import net.casper.data.model.CRowMetaData;
import net.casper.ext.CMarkedUpRowBean;
import net.casper.ext.swing.CDataRuntimeException;

import org.rosuda.REngine.RList;

/**
 * Convert a {@link Collection} of {@link CMarkedUpRowBean}s to a list of
 * {@link RVector}s or an {@link RList}. Converts all properties of the beans
 * just like {@link CollectionToR} does, but also adds the columns in the
 * {@link CMarkedUpRow}.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public class CMarkedUpRowBeanCollectionToR extends CollectionToR {

	/**
	 * Create an {@link CMarkedUpRowBeanCollectionToR} from the given
	 * {@link Collection}.
	 * 
	 * Introspection is used to determine the bean properties (i.e.: getter
	 * methods) that are exposed, and each one becomes a vector. Vectors are
	 * only created for primitive properties and arrays of primitive properties;
	 * object properties are ignored without warning.
	 * 
	 * Vectors are also created for each column in the {@link CMarkedUpRow}
	 * exposed by the beans.
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
	public CMarkedUpRowBeanCollectionToR(
			Collection<? extends CMarkedUpRowBean> col, Class<?> stopClass)
			throws IntrospectionException {
		super(col, stopClass);

		// extract the vectors from the CMarkedUpRow
		List<RVector> rowProps = extractMarkedRow(col);
		vectors.addAll(rowProps);

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
		List<RVector> vectors = RVectors.createVectorList(
				meta.getColumnNames(), meta.getColumnTypes(), col.size());

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
