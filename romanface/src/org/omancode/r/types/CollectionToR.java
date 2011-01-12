package org.omancode.r.types;

import java.beans.IntrospectionException;
import java.util.Collection;
import java.util.List;

import org.omancode.r.RInterfaceException;
import org.omancode.util.beans.BeanPropertyInspector;
import org.omancode.util.beans.BeanUtil;
import org.rosuda.REngine.RList;

/**
 * Convert a {@link Collection} to a list of {@link RVector}s or an
 * {@link RList}.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public class CollectionToR {

	/**
	 * {@link BeanPropertyInspector} for the collection.
	 */
	protected final BeanPropertyInspector props;
	
	/**
	 * List of {@link RVector}s generated for the collection.
	 */
	protected final List<RVector> vectors;

	/**
	 * Create an {@link CollectionToR} from the given {@link Collection}.
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
	public CollectionToR(Collection<?> col, Class<?> stopClass)
			throws IntrospectionException {

		if (col.isEmpty()) {
			throw new IllegalArgumentException(
					"Cannot create empty collection in R");
		}

		// get bean properties
		Object bean = col.iterator().next();
		props = new BeanPropertyInspector(bean, stopClass);

		// create a List of RVectors from the properties
		vectors = RVectors.createVectorList(props.getNames(), props.getTypes(),
				col.size());

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
	 * Get the {@link BeanPropertyInspector}.
	 * 
	 * @return bean props
	 */
	public BeanPropertyInspector getProps() {
		return props;
	}

	/**
	 * Get the vectors.
	 * 
	 * @return list of vectors.
	 */
	public List<RVector> getVectors() {
		return vectors;
	}

	/**
	 * Create an {@link RList} of the vectors.
	 * 
	 * @return rlist rlist
	 * @throws RInterfaceException
	 *             if cannot convert vectors to an {@link RList}.
	 * @throws IntrospectionException
	 *             if vector list is empty because the colection contains no
	 *             convertable properties
	 */
	public RList createRList() throws RInterfaceException,
			IntrospectionException {

		if (vectors.isEmpty()) {
			throw new IntrospectionException(
					"Collection does not contain any properties "
							+ "that can be converted to R");
		}

		return RVectors.toRList(vectors);
	}

}
