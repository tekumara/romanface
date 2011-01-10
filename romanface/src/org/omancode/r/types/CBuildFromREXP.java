package org.omancode.r.types;

import java.io.IOException;
import java.util.Map;

import net.casper.data.model.CBuilder;

import org.apache.commons.lang.NotImplementedException;
import org.omancode.r.RInterfaceException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPVector;

/**
 * Builds a Casper dataset from a {@link REXP}.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public class CBuildFromREXP implements CBuilder {

	private final CBuilder builder;
	
	/**
	 * Builds a Casper dataset from a {@link REXP}.
	 * 
	 * @param rexp
	 *            rexp
	 * @param name
	 *            name of dataset
	 * @throws RInterfaceException
	 *             if dataset cannot be built from this REXP
	 */
	public CBuildFromREXP(REXP rexp, String name)
			throws RInterfaceException {
	
		try {
			if (RMatrix.isMatrix(rexp)) {
				builder = new RMatrix(name, rexp);
			} else if (RDataFrame.isDataFrame(rexp)) {
				builder = new RDataFrame(name, rexp);
			} else if (rexp instanceof REXPVector) {
				builder = new RVector(name, (REXPVector) rexp);
			} else {
				throw new NotImplementedException(" REXP of class "
						+ REXPAttr.getClassAttribute(rexp)
						+ ".\nConversion of this class to "
						+ "dataset not yet implemented.");
			}

		} catch (UnsupportedTypeException e) {
			throw new RInterfaceException(e.getMessage(), e);
		} catch (REXPMismatchException e) {
			throw new RInterfaceException(e.getMessage(), e);
		}
	
	}

	
	@Override
	public String getName() {
		return builder.getName();
	}

	@Override
	public String[] getColumnNames() {
		return builder.getColumnNames();
	}

	@Override
	public Class[] getColumnTypes() {
		return builder.getColumnTypes();
	}

	@Override
	public String[] getPrimaryKeyColumns() {
		return builder.getPrimaryKeyColumns();
	}

	@Override
	public Map getConcreteMap() {
		return builder.getConcreteMap();
	}

	@Override
	public void open() throws IOException {
		builder.open();
	}

	@Override
	public Object[] readRow() throws IOException {
		return builder.readRow();
	}

	@Override
	public void close() {
		builder.close();
	}

}
