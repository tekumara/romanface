package org.omancode.r.ui;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.omancode.r.RFaceException;
import org.omancode.r.RFace;
import org.omancode.r.RUtil;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;

/**
 * Builds a {@link JTree} that represents the objects in an R environment.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 * 
 */
public class RObjectTreeBuilder {

	private final RFace rInterface;
	private final JTree tree = new JTree();
	private final DefaultMutableTreeNode root;
	private final DefaultTreeModel model;
	private final String include;

	/**
	 * Construct a {@link RObjectTreeBuilder} from the set of all objects
	 * present at time of construction in the R global environment.
	 * 
	 * @param rInterface
	 *            r interface
	 * @throws RFaceException
	 *             if problem getting objects
	 */
	public RObjectTreeBuilder(RFace rInterface)
			throws RFaceException {
		this(rInterface, null, "all");
	}

	/**
	 * Construct a {@link RObjectTreeBuilder} from a dataframe or the set of
	 * objects present at time of construction in the R global environment.
	 * 
	 * @param rInterface
	 *            r interface
	 * @param dataframe
	 *            name of a dataframe. If specified only this dataframe will be
	 *            present in the tree. If {@code null} then all objects in the
	 *            environment will be present in the tree.
	 * @param includeClass
	 *            specify a single class of object to display, or {@code null}
	 *            to display objects of any class.
	 * @throws RFaceException
	 *             if problem getting objects
	 */
	public RObjectTreeBuilder(RFace rInterface, String dataframe,
			String includeClass) throws RFaceException {
		this(rInterface, dataframe, new String[] { includeClass });
	}

	/**
	 * Construct a {@link RObjectTreeBuilder} from a dataframe or the set of
	 * objects present at time of construction in the R global environment.
	 * 
	 * @param rInterface
	 *            r interface
	 * @param dataframe
	 *            name of a dataframe. If specified only this dataframe will be
	 *            present in the tree. If {@code null} then all objects in the
	 *            environment will be present in the tree.
	 * @param includeClass
	 *            specify the classes of object to display, or {@code null} to
	 *            display objects of any class.
	 * @throws RFaceException
	 *             if problem getting objects
	 */
	public RObjectTreeBuilder(RFace rInterface, String dataframe,
			String[] includeClass) throws RFaceException {
		this.rInterface = rInterface;
		this.include = RUtil.toVectorExprString(includeClass);

		if (dataframe == null) {
			root = new DefaultMutableTreeNode("R");
			model = new DefaultTreeModel(root);
			addNodes(root, getObjects());
		} else {
			root =
					new RObjectNode(this, dataframe, "data.frame",
							getInfo(dataframe));
			root.setParent(null);
			model = new DefaultTreeModel(root);

		}

		tree.setModel(model);
		tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);

	}

	/**
	 * Get the JTree for display.
	 * 
	 * @return jtree
	 */
	public JTree getTree() {
		return tree;
	}

	/**
	 * Create {@link RObjectNode}s underneath parent node from array of nodes.
	 * 
	 * @param parent
	 *            parent node
	 * @param nodes
	 *            map of strings to create {@link RObjectNode}s from.
	 */
	public final void addNodes(DefaultMutableTreeNode parent,
			RObjectNode[] nodes) {

		for (RObjectNode node : nodes) {
			model.insertNodeInto(node, parent, parent.getChildCount());
		}

	}

	private RObjectNode[] getObjects() throws RFaceException {
		String expr = ".getObjects(include=" + include + ")";
		return getNodes(expr);
	}

	/**
	 * Get the parts that make up an R object, or an empty array if the R object
	 * has no parts.
	 * 
	 * @param rname
	 *            r object name
	 * @return array of nodes created from parts
	 * @throws RFaceException
	 *             if problem getting part information from R
	 */
	public final RObjectNode[] getParts(String rname)
			throws RFaceException {
		String expr = ".getParts(" + rname + ", include=" + include + ")";
		return getNodes(expr);
	}

	/**
	 * Execute an R command that returns a list containing the named {@code chr}
	 * vectors {@code names}, {@code class}, {@code info}.
	 * 
	 * @param expr
	 *            r expression
	 * @return array of nodes created from expr, or an array with nothing if
	 *         expr returns nothing.
	 * @throws RFaceException
	 *             if problem evaluating expr
	 */
	private RObjectNode[] getNodes(String expr) throws RFaceException {
		RList rlist = rInterface.parseEvalTryAsRList(expr);

		if (rlist == null) {
			return new RObjectNode[0];
		}

		String[] names = ((REXPString) rlist.get("names")).asStrings();
		String[] klass = ((REXPString) rlist.get("class")).asStrings();
		String[] info = ((REXPString) rlist.get("info")).asStrings();

		RObjectNode[] nodes = new RObjectNode[names.length];

		for (int i = 0; i < names.length; i++) {
			nodes[i] = new RObjectNode(this, names[i], klass[i], info[i]);
		}

		return nodes;
	}

	/**
	 * Get info for a particular R object. Info returned depends on the R object
	 * class and could be dimensions/length etc.
	 * 
	 * @param rname
	 *            r object name
	 * @return info
	 */
	public final String getInfo(String rname) {
		String expr = ".getInfo(" + rname + ")";

		try {
			String info = rInterface.evalReturnString(expr);

			return info;
		} catch (RFaceException e) {
			throw new RuntimeException(e);
		}
	}

}
