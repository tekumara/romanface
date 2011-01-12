package org.omancode.r.types;

import java.util.List;

import net.casper.data.model.CDataCacheContainer;
import net.casper.data.model.CDataGridException;
import net.casper.data.model.CDataRowSet;
import net.casper.data.model.CRowMetaData;

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

}
