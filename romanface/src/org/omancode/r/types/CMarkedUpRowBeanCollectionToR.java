package org.omancode.r.types;

import java.beans.IntrospectionException;
import java.util.Collection;
import java.util.List;

import net.casper.data.model.CDataGridException;
import net.casper.data.model.CMarkedUpRow;
import net.casper.data.model.CRowMetaData;
import net.casper.ext.swing.CDataRuntimeException;

import org.omancode.r.RInterfaceException;
import org.omancode.util.beans.BeanUtil;
import org.rosuda.REngine.RList;

/**
 * Convert a {@link Collection} that contains a bean with a property returning a
 * {@link CMarkedUpRow} to a list of {@link RVector}s or an {@link RList}.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public class CMarkedUpRowBeanCollectionToR extends CollectionToR {

	public CMarkedUpRowBeanCollectionToR(Collection<?> col, Class<?> stopClass)
			throws IntrospectionException, RInterfaceException {
		super(col, stopClass);

		// get the name of the first property that returns a
		// CMarkedUpRow
		String markedUpRowGetterName = props.getNamesAssignableFrom(
				CMarkedUpRow.class).get(0);

		// extract the vectors from the CMarkedUpRow
		if (markedUpRowGetterName != null) {
			List<RVector> rowProps = extractMarkedRow(col,
					markedUpRowGetterName);
			vectors.addAll(rowProps);
		}

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
	private static List<RVector> extractMarkedRow(Collection<?> col,
			String markedUpRowGetterName) throws IntrospectionException {

		Object cMarkedUpRowBean = col.iterator().next();
		CMarkedUpRow firstRow = (CMarkedUpRow) BeanUtil.getProperty(
				cMarkedUpRowBean, markedUpRowGetterName);

		// get list of vectors, one for each column in the row
		// the vectors will be empty
		CRowMetaData meta = firstRow.getMetaDefinition();
		List<RVector> vectors = RVectors.createVectorList(
				meta.getColumnNames(), meta.getColumnTypes(), col.size());

		// fill the RVectors' values row by row
		// from the bean's property values
		for (Object element : col) {
			for (RVector vector : vectors) {
				String propName = vector.getName();
				CMarkedUpRow row = (CMarkedUpRow) BeanUtil.getProperty(element,
						markedUpRowGetterName);
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
