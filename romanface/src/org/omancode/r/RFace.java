package org.omancode.r;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.omancode.math.NamedNumber;
import org.omancode.r.types.REXPAttr;
import org.omancode.r.types.REXPUtil;
import org.omancode.r.types.RMatrix;
import org.omancode.util.ArrayUtil;
import org.omancode.util.StringUtil;
import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REngine;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.JRI.JRIEngine;

/**
 * General purpose interface to the R high level engine. Provides
 * <ul>
 * <li>Single instance static interface to R (because R is single threaded).
 * <li>Load and initialise functions.
 * <li>Multiple methods for evaluating R expressions including with/without
 * output of returned expressions or printed output, and with/without exception
 * handling.
 * <li>Evaluation of expressions from files.
 * <li>Assignment of R objects.
 * <li>Message output to the R console.
 * </ul>
 * 
 * Uses the RServe {@link REngine} interface, which prevents re-entrance and has
 * Java side objects that represent R data structures, rather than the lower
 * level JRI {@link org.rosuda.JRI.Rengine} interface which doesn't have these
 * features.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public final class RFace {

	/**
	 * Support file containing R functions to load into R environment on
	 * startup.
	 */
	private static final String SUPPORT_FILE = "RFace.r";

	/**
	 * Newline.
	 */
	private static final String NEWLINE = System
			.getProperty("line.separator");

	/**
	 * Private constructor prevents instantiation from other classes.
	 */
	private RFace() {
	}

	/**
	 * R REPL (read-evaluate-parse loop) handler.
	 */
	private static RMainLoopCallbacks rloopHandler = null;

	/**
	 * SingletonHolder is loaded, and the static initializer executed, on the
	 * first execution of Singleton.getInstance() or the first access to
	 * SingletonHolder.INSTANCE, not before.
	 */
	private static final class SingletonHolder {

		/**
		 * Singleton instance, with static initializer.
		 */
		private static final RFace INSTANCE = initRInterfaceHL();

		/**
		 * Initialize RInterfaceHL singleton instance using rLoopHandler from
		 * outer class.
		 * 
		 * @return RInterfaceHL instance
		 */
		private static RFace initRInterfaceHL() {
			try {
				return new RFace(rloopHandler); // NOPMD
			} catch (RFaceException e) {
				// a static initializer cannot throw exceptions
				// but it can throw an ExceptionInInitializerError
				throw new ExceptionInInitializerError(e);
			}
		}

		/**
		 * Prevent instantiation.
		 */
		private SingletonHolder() {
		}

		/**
		 * Get singleton RInterfaceHL.
		 * 
		 * @return RInterfaceHL singleton.
		 */
		public static RFace getInstance() {
			return SingletonHolder.INSTANCE;
		}

	}

	/**
	 * Return the singleton instance of RInterfaceHL. Only the first call to
	 * this will establish the rloopHandler.
	 * 
	 * @param rloopHandler
	 *            R REPL handler supplied by client.
	 * @return RInterfaceHL singleton instance
	 * @throws RFaceException
	 *             if REngine cannot be created
	 */
	public static RFace getInstance(RMainLoopCallbacks rloopHandler)
			throws RFaceException {
		RFace.rloopHandler = rloopHandler;

		try {
			return SingletonHolder.getInstance();
		} catch (ExceptionInInitializerError e) {

			// re-throw exception that occurred in the initializer
			// so our caller can deal with it
			Throwable eInInit = e.getCause();
			throw new RFaceException(eInInit.getMessage(), eInInit); // NOPMD
		}
	}

	/**
	 * org.rosuda.REngine.REngine high level R interface.
	 */
	private REngine rosudaEngine = null;

	private boolean supportFunctionsLoaded = false;

	/**
	 * Construct new RInterfaceHL. Only ever gets called once by
	 * {@link SingletonHolder.initRInterfaceHL}.
	 * 
	 * @param rloopHandler
	 *            R REPL handler supplied by client.
	 * @throws RFaceException
	 *             if R cannot be loaded.
	 */
	private RFace(RMainLoopCallbacks rloopHandler) throws RFaceException {

		// tell Rengine code not to die if it can't
		// load the JRI native DLLs. This allows
		// us to catch the UnsatisfiedLinkError
		// ourselves
		System.setProperty("jri.ignore.ule", "yes");

		try {

			// ends up loading jri.dll via a System.loadLibrary("jri") call
			// in org.rosuda.JRI.Rengine
			// which looks in java.library.path for jri.dll
			rosudaEngine =
					new JRIEngine(new String[] { "--no-save" }, rloopHandler);

			// NB: we don't load the support functions here
			// because the console may not yet be established
			// and displayed and we will miss out on any
			// warnings if the support function load fails.

		} catch (REngineException e) {

			// output diagnosis information
			System.err.format("%s=%s%n", "java.library.path",
					System.getProperty("java.library.path"));
			System.err.format("%s=%s%n", "Path", System.getenv().get("Path"));
			System.err.format("%s=%s%n", "R_HOME",
					System.getenv().get("R_HOME"));

			throw new RFaceException(e.getMessage(), e);
		}
	}

	/**
	 * Load support functions from support file. Provides support functions for
	 * {@link #parseEvalPrint(String)} and
	 * {@link org.omancode.r.ui.RObjectTreeBuilder}.
	 * 
	 * @throws IOException
	 *             if problem loading file
	 */
	public void loadRSupportFunctions() throws IOException {
		// NB: we access SUPPORT_FILE as a resource because it
		// is packaged in a .jar. This means however that we need
		// to read it as a stream and can't use loadFile()

		InputStream ins = getClass().getResourceAsStream(SUPPORT_FILE);
		if (ins == null) {
			throw new IOException("Resource " + SUPPORT_FILE + " not found.");
		}

		parseEvalTry(RUtil.readRStream(ins));
	}

	/**
	 * Evaluate an expression in R in the global environment. Does not
	 * explicitly call "parse" so any syntax errors will result in the unhelpful
	 * "parse error" exception message. Any evaluation errors will have the
	 * error messages printed to the console and return the exception message
	 * "error during evaluation". Unlike, the REPL and
	 * {@link #parseEvalPrint(String)}, does not print the result of the
	 * expression (ie: does just the E part of the REPL). Any prints within the
	 * expression or functions called by the expression will print. Warnings
	 * produced by the expression will not be printed (instead they will appear
	 * next time a command is entered directly into the REPL and executed).
	 * 
	 * @param expr
	 *            expression to evaluate.
	 * @return REXP result of the evaluation. Not printed to the console.
	 * @throws RFaceException
	 *             if problem during parse or evaluation. Parse errors will
	 *             simply return the message "parse error". Any evaluation
	 *             errors with have the error messages printed to the console
	 *             and return the exception message "error during evaluation".
	 */
	public REXP eval(String expr) throws RFaceException {
		if (!initialized()) {
			throw new IllegalStateException(
					"REngine has not been initialized.");
		}

		try {
			return rosudaEngine.parseAndEval(expr);
		} catch (REngineException e) {
			throw new RFaceException(expr + ": " + e.getMessage(), e);
		} catch (REXPMismatchException e) {
			throw new RFaceException(expr + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Evaluate an expression and test that it returns a {@link REXPString}.
	 * 
	 * @param expr
	 *            expression to evaluate.
	 * @return String array or {@code null}.
	 * @throws RFaceException
	 *             if problem during parse or evaluation, or expression does not
	 *             return a {@link REXPString}.
	 */
	public String[] evalReturnStrings(String expr) throws RFaceException {
		try {
			REXP rexp = eval(expr);

			if (rexp == null || rexp instanceof REXPNull) {
				return null;
			}

			// r command must return a REXPString
			if (!(rexp instanceof REXPString)) {
				throw new RFaceException(expr + " returned "
						+ rexp.getClass().getCanonicalName()
						+ " instead of REXPString");
			}

			return rexp.asStrings();
		} catch (REXPMismatchException e) {
			throw new RFaceException(e.getMessage(), e);
		}
	}

	public Map<String, String> evalReturnNamedStringsSorted(String expr)
			throws RFaceException {
		Map<String, String> namedStrings = new TreeMap<String, String>();

		return evalReturnNamedStrings(expr, namedStrings);

	}

	/**
	 * Evaluate an expression, testing that it returns a {@link REXPString}, and
	 * return a map with keys equal to the names of the character vector and
	 * values equal to the vector values. If the expression returns a
	 * {@link REXPNull} then an empty map is returned.
	 * 
	 * @param expr
	 *            expression to evaluate.
	 * @return map of named strings
	 * @throws RFaceException
	 *             if problem during parse or evaluation, or expression does not
	 *             return a {@link REXPString}.
	 */
	public Map<String, String> evalReturnNamedStrings(String expr)
			throws RFaceException {
		Map<String, String> namedStrings =
				new LinkedHashMap<String, String>();

		return evalReturnNamedStrings(expr, namedStrings);

	}

	/**
	 * Evaluate an expression, testing that it returns a {@link REXPString}, and
	 * return a map with keys equal to the names of the character vector and
	 * values equal to the vector values. If the expression returns a
	 * {@link REXPNull} then an empty map is returned.
	 * 
	 * @param expr
	 *            expression to evaluate.
	 * @param map
	 *            to fill with strings
	 * @return map of named strings
	 * @throws RFaceException
	 *             if problem during parse or evaluation, or expression does not
	 *             return a {@link REXPString}.
	 */
	public Map<String, String> evalReturnNamedStrings(String expr,
			Map<String, String> map) throws RFaceException {
		try {
			REXP rexp = eval(expr);

			// r command must return REXPNull or a REXPString
			if (rexp instanceof REXPNull) {
				return Collections.emptyMap();
			} else if (!(rexp instanceof REXPString)) {
				throw new RFaceException(expr + " returned "
						+ rexp.getClass().getCanonicalName()
						+ " instead of REXPString");
			}

			String[] values = rexp.asStrings();
			String[] names = REXPAttr.getNamesAttribute(rexp);

			for (int i = 0; i < values.length; i++) {
				map.put(names[i], values[i]);
			}

			return map;

		} catch (REXPMismatchException e) {
			throw new RFaceException(e.getMessage(), e);
		}
	}

	/**
	 * Evaluate a String expression in R in the global environment. Returns all
	 * console output produced by this evaluation. Does not return the
	 * {@link REXP} produced by the evaluation. This is needed for functions
	 * like {@code str} which print their output and don't return anything.
	 * 
	 * @param expr
	 *            expression to evaluate.
	 * @return console output from the evaluation.
	 * @throws RFaceException
	 *             if problem during parse or evaluation. Parse errors will
	 *             simply return the message "parse error".
	 */
	public String evalCaptureOutput(String expr) throws RFaceException {
		return evalReturnString("capture.output(" + expr + ")");
	}

	/**
	 * Evaluate an expression and test that it returns a {@link REXPString}.
	 * Return the character vector as one String with newlines between elements.
	 * 
	 * @param expr
	 *            expression to evaluate.
	 * @return String with newlines between elements, or {@code null} if expr
	 *         returns {@code null}.
	 * @throws RFaceException
	 *             if problem during parse or evaluation, or expression does not
	 *             return a {@link REXPString}.
	 */
	public String evalReturnString(String expr) throws RFaceException {
		String[] strs = evalReturnStrings(expr);

		if (strs == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder(1024);

		for (int i = 0; i < strs.length - 1; i++) {
			sb.append(strs[i]).append(NEWLINE);
		}

		sb.append(strs[strs.length - 1]);

		return sb.toString();

	}

	/**
	 * {@link #parseEvalTry(String)} that returns an {@link RList} or exception.
	 * 
	 * @param expr
	 *            expression to try and parse and eval
	 * @return rlist, or {@code null} if expr returns REXPNull.
	 * @throws RFaceException
	 *             if {@code expr} does not return a list
	 */
	public RList parseEvalTryAsRList(String expr) throws RFaceException {
		REXP rexp = parseEvalTry(expr);

		if (rexp instanceof REXPNull) {
			return null;

		}
		if (!rexp.isList()) {
			throw new RFaceException(expr + " returned "
					+ rexp.getClass().getCanonicalName()
					+ " instead of a list");
		}

		try {
			return rexp.asList();
		} catch (REXPMismatchException e) {
			throw new RFaceException(e);
		}

	}

	/**
	 * Wraps a parse and try around an eval. The parse will generate syntax
	 * error messages, and the try will catch parse and evaluation errors and
	 * return them in the exception as opposed to printing it on the console.
	 * 
	 * @param expr
	 *            expression to try and parse and eval
	 * @return REXP result of the evaluation.
	 * @throws RFaceException
	 *             if there is a parse or evaluation error the error message is
	 *             returned in the exception. Nothing printed to the console.
	 */
	public REXP parseEvalTry(String expr) throws RFaceException {
		return parseEvalTry(expr, false);
	}

	/**
	 * Wraps a parse and try around an eval. The parse will generate syntax
	 * error messages, and the try will catch parse and evaluation errors and
	 * return them in the exception.
	 * 
	 * @param expr
	 *            expression to try and parse and eval
	 * @param silent
	 *            if {@code true} will not print anything to the console,
	 *            otherwise error and warning messages will be displayed on the
	 *            console
	 * @return REXP result of the evaluation.
	 * @throws RFaceException
	 *             if there is a parse or evaluation error the error message is
	 *             returned in the exception. Nothing printed to the console.
	 */
	public REXP parseEvalTry(String expr, boolean silent)
			throws RFaceException {
		if (!initialized()) {
			throw new IllegalStateException(
					"REngine has not been initialized.");
		}

		try {

			/**
			 * Place the expression in a character vector, syntax errors and
			 * all. If we tried to execute the expression directly we might run
			 * into syntax errors that wouldn't be trapped by try.
			 */
			rosudaEngine.assign(".expression.", expr);

			/**
			 * parse: converts a file, or character vector, into an expression
			 * object but doesn't evaluate it. If there is a problem parsing,
			 * because of syntax error, it will print a detailed message. This
			 * message can be captured by "try".
			 * 
			 * eval: evaluates an expression object
			 * 
			 * try: returns a "try-error" object with the contents of the error
			 * text, or if no error returns the evaluated expression's return
			 * object (if it has one).
			 * 
			 */
			String exec =
					"try(eval(parse(text=.expression.)), silent="
							+ RUtil.rBoolean(silent) + ")";

			REXP rexp = rosudaEngine.parseAndEval(exec);

			if (rexp == null) {
				// evaluated OK and returned nothing
				return null;
			} else if (rexp.inherits("try-error")) {
				// evaluated with error and returned "try-error" object which
				// contains error message

				// inject expr into error message
				String errMsg =
						rexp.asString().replace("eval(expr, envir, enclos)",
								"\"" + expr + "\"");
				throw new RFaceException(errMsg);
			} else {
				// evaluated OK and returned object
				return rexp;
			}

		} catch (REngineException e) {
			// catch any errors generated by parseAndEval and display msg
			throw new RFaceException(exprErrMsg(expr)
					+ System.getProperty(NEWLINE) + e.getMessage(), e);
		} catch (REXPMismatchException e) {
			// catch any errors generated by parseAndEval and display msg
			throw new RFaceException(exprErrMsg(expr)
					+ System.getProperty(NEWLINE) + e.getMessage(), e);
		}
	}

	/**
	 * Return the first 256 characters of expr and the last error message
	 * reported by R.
	 * 
	 * @param expr
	 *            expression
	 * @return expr as error message
	 */
	private String exprErrMsg(String expr) {
		int len = Math.min(expr.length(), 256);
		return "\"" + expr.substring(0, len) + (len > 256 ? " ... " : "")
				+ "\": " + getErrMessage();
	}

	/**
	 * Get the last error message generated in R.
	 * 
	 * @return last error message
	 */
	public String getErrMessage() {
		try {
			return evalReturnString("geterrmessage()");
		} catch (RFaceException e) {
			return "geterrmessage failed!";
		}
	}

	/**
	 * Evaluate {@code expr} returning a {@link RMatrix} or any errors as an
	 * {@link RFaceException}.
	 * 
	 * @param expr
	 *            expression
	 * @return {@link RMatrix}
	 * @throws RFaceException
	 *             if problem evaluating {@code expr}, including if {@code expr}
	 *             does not return an expression that can be represented as a
	 *             {@link RMatrix}.
	 */
	public RMatrix parseEvalTryReturnRMatrix(String expr)
			throws RFaceException {
		REXP rexp = parseEvalTry(expr);
		return new RMatrix(expr, rexp);
	}

	/**
	 * Evaluates {@code expr} and returns an array of {@link NamedNumber}.
	 * 
	 * @param expr
	 *            expression
	 * @return named number array
	 * @throws RFaceException
	 *             if problem evaluating {@code expr}, including if {@code expr}
	 *             does not return a {@link REXPDouble} or {@link REXPInteger}.
	 */
	public NamedNumber[] parseEvalTryReturnNamedNumber(String expr)
			throws RFaceException {
		REXP rexp = parseEvalTry(expr);

		// r command must return a REXPDouble or REXPInteger
		if (!(rexp instanceof REXPDouble || rexp instanceof REXPInteger)) {
			throw new RFaceException(expr + " returned "
					+ rexp.getClass().getCanonicalName());
		}

		try {

			if (rexp.length() == 0) {
				throw new RFaceException(expr + " returned no values.");
			}

			// get names. these are the factors.
			String[] valueNames = REXPAttr.getNamesAttribute(rexp);

			if (valueNames == null) {
				throw new RFaceException("Result of " + expr
						+ " does not supply names attribute.");
			}

			// get values
			double[] values = rexp.asDoubles();

			// create named nums
			NamedNumber[] nums = new NamedNumber[values.length];
			for (int i = 0; i < values.length; i++) {
				nums[i] = new NamedNumber(valueNames[i], values[i]);
			}

			return nums;

		} catch (REXPMismatchException e) {
			throw new RFaceException(e.getMessage(), e);
		}

	}

	/**
	 * Wraps a parse around an eval and prints (shows) result. Returns the
	 * expression result AND prints it to the console if it is visible, ie: the
	 * REP parts of the REPL. Errors & warnings are output to the console, ie:
	 * doesn't produce Java exceptions for expression errors. Uses R global
	 * environment.
	 * 
	 * Since the complete expression is parsed before any of it is run, syntax
	 * errors result in none of the code being run. If an error occurs in
	 * running a syntactically correct expression, execution will stop and the
	 * rest of the expression will not be executed. Anything assigned into the
	 * workspace by code that has been run will be kept (just as from the
	 * command line).
	 * 
	 * traceback() does not work on expressions evaluted by
	 * {@link #parseEvalPrint(String)}.
	 * 
	 * @param expr
	 *            expression to parse, eval and show
	 * @return REXP result of the evaluation. Also printed to the console if
	 *         visible. Returns {@code null} if there was an exception generated
	 *         whilst evaluating the expression.
	 */
	public REXP parseEvalPrint(String expr) {
		if (!initialized()) {
			throw new IllegalStateException(
					"REngine has not been initialized.");
		}

		try {
			if (!supportFunctionsLoaded) {
				loadRSupportFunctions();
			}
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}

		try {

			/**
			 * Place the expression in a character vector, syntax errors and
			 * all. If we tried to execute the expression directly we might run
			 * into syntax errors in the executing statement. This also avoids
			 * the problem of having to search for and find quotes in string and
			 * replace them.
			 */
			rosudaEngine.assign(".expression.", expr);

			String exec = "pep(.expression.)";
			// String exec = expr;

			REXP result = rosudaEngine.parseAndEval(exec);

			return result;

		} catch (REngineException e) {
			// swallow! error message will be printed to the console
			return null;
		} catch (REXPMismatchException e) {
			// swallow! error message will be printed to the console
			return null;
		}
	}

	/**
	 * Parse and evaluate a text file in R. Calls {@link #parseEvalTry(File)}.
	 * Throws exception if problem reading the file.
	 * 
	 * @param file
	 *            text file to evaluate in R
	 * @return REXP result of the evaluation.
	 * @throws IOException
	 *             if file cannot be read or problem during evaluation. See
	 *             {@link #parseEvalTry(String)}.
	 */
	public REXP parseEvalTry(File file) throws IOException {
		try {
			return parseEvalTry(RUtil.readRFile(file));
		} catch (RFaceException e) {
			throw new RFaceException(file.getCanonicalPath() + " "
					+ e.getMessage(), e);
		}
	}

	/**
	 * Check if R engine has been loaded/initialized.
	 * 
	 * @return true if R engine has been loaded/initialized.
	 */
	public boolean initialized() {
		return rosudaEngine != null;
	}

	/**
	 * Print a message out to the R console.
	 * 
	 * @param msg
	 *            message to print.
	 * @throws RFaceException
	 *             if problem during evaluation.
	 */
	public void printToConsole(String msg) throws RFaceException {
		rloopHandler.rWriteConsole(null, msg, 0);
		// parseAndEval("cat('" + msg + "')");
	}

	/**
	 * Print a message out to the R console with a line feed.
	 * 
	 * @param msg
	 *            message to print.
	 * @throws RFaceException
	 *             if problem during evaluation.
	 */
	public void printlnToConsole(String msg) throws RFaceException {
		printToConsole(msg + "\n");
	}

	/**
	 * Create an R expression in R as an object so it can be referenced (in the
	 * global environment). Uses the rosuda engine assign function.
	 * 
	 * NB: this assign does not use dispatch assignment methods, so it cannot be
	 * used to set elements of vectors, lists, names, attributes, etc. For these
	 * cases use {@link #assign(String, String)} instead.
	 * 
	 * @param name
	 *            symbol name
	 * @param rexp
	 *            r expression
	 * @throws RFaceException
	 *             if problem assigning
	 */
	public void assign(String name, REXP rexp) throws RFaceException {
		try {
			rosudaEngine.assign(name, rexp);
		} catch (REngineException e) {
			throw new RFaceException(e);
		} catch (REXPMismatchException e) {
			throw new RFaceException(e);
		}
	}

	/**
	 * Assign a source expression to a variable using the assignment operator
	 * <-. Can be used to set elements of vectors, lists, names, attributes,
	 * etc.
	 * 
	 * 
	 * @param x
	 *            destination variable name
	 * @param value
	 *            source expression
	 * @throws RFaceException
	 *             if problem assigning
	 */
	public void assign(String x, String value) throws RFaceException {
		parseEvalTry(x + " <- " + value);
	}

	/**
	 * Create an {@link RList} in R as a dataframe.
	 * 
	 * @param name
	 *            the name of the dataframe to create in R.
	 * @param rlist
	 *            the rlist
	 * @throws RFaceException
	 *             if problem assigning list
	 */
	public void assignDataFrame(String name, RList rlist)
			throws RFaceException {
		try {
			// turn the rlist into a dataframe
			REXP dataframe = REXP.createDataFrame(rlist);

			// assign the dataframe to a named R object
			assign(name, dataframe);
		} catch (REXPMismatchException e) {
			throw new RFaceException(e);
		}
	}

	/**
	 * Create an {@link RList} in R as a matrix. All elements of {@code rlist}
	 * must be of the same type and length.
	 * 
	 * @param name
	 *            the name of the dataframe to create in R.
	 * @param rlist
	 *            the rlist
	 * @throws RFaceException
	 *             if problem assigning list
	 */
	public void assignMatrix(String name, RList rlist) throws RFaceException {
		try {
			// TODO: this is a quick nasty way, fix this up by
			// creating directly as REXPVector with appropriate
			// dims and names, doing checks on length and type.

			// turn the rlist into a dataframe
			REXP dataframe = REXP.createDataFrame(rlist);

			// assign the list to a named R object
			assign(name, dataframe);

			// dfm <- as.matrix(df)
			// rownames(dfm) <- .catadj[[1]]

			assign(name, StringUtil.functionCall(".assignMatrix", name));

		} catch (REXPMismatchException e) {
			throw new RFaceException(e);
		}
	}

	/**
	 * Create a hash in R from a {@link Map}. Requires the {@code hash} R
	 * package to have been loaded.
	 * 
	 * @param name
	 *            name of hash
	 * @param map
	 *            map to write out as hash
	 * @throws RFaceException
	 *             if problem creating hash
	 */
	public void assignHash(String name, Map<String, ?> map)
			throws RFaceException {

		// create new hash
		parseEvalTry(name + " <- hash()");

		for (Map.Entry<String, ?> entry : map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			// convert value to REXP and store in hash
			if (!"".equals(key)) {
				REXP rexp = REXPUtil.toREXP(value);
				assign(".hashValue", rexp);
				parseEvalTry(name + "[[\"" + key + "\"]] <- .hashValue");
			}
		}
	}

	/**
	 * Get R_DEFAULT_PACKAGES.
	 * 
	 * @return default packages
	 * @throws RFaceException
	 *             if problem during command evaluation.
	 */
	public String getCurrentPackages() throws RFaceException {

		REXP availablePkgs;
		try {
			availablePkgs = eval(".packages(TRUE)");
			if (availablePkgs.isNull() || availablePkgs.asStrings() == null) {
				return "";
			} else {
				return ArrayUtil.toString(availablePkgs.asStrings());
			}
		} catch (REXPMismatchException e) {
			throw new RFaceException(e);
		}
	}

	/**
	 * Loads a package. If it isn't installed, it is loaded from CRAN.
	 * 
	 * @param pack
	 *            package name
	 * @throws RFaceException
	 *             if problem loading package.
	 */
	public void loadPackage(String pack) throws RFaceException {
		String packages = getCurrentPackages();
		if (!packages.contains(pack)) {
			printlnToConsole("Package " + pack
					+ " not found. Attempting to download...");
			parseEvalPrint("install.packages('" + pack + "');library(" + pack
					+ ")");
		} else {
			printlnToConsole("Loading package: " + pack);
			parseEvalPrint("library(" + pack + ")");
		}
	}

	/**
	 * Evaluate the contents of a file in R using the {@code source()} function.
	 * 
	 * @param file
	 *            file containing R commands
	 * @throws IOException
	 *             if problem getting file path, or problem evaluating file
	 */
	public void loadFile(File file) throws IOException {
		String filename = sanitiseFilePath(file.getCanonicalPath());

		String expr = "source(\"" + filename + "\", chdir=TRUE)";
		parseEvalTry(expr);
	}

	/**
	 * Creates an new named environment and attaches it, then evaluates the
	 * contents of a file in R in that environment using the
	 * {@code sys.source()} function.
	 * 
	 * @param file
	 *            file containing R commands
	 * @param environmentName
	 *            name of the environment in which to evaluate the source file,
	 *            or {@code null} to evaluate the source file in the global
	 *            environment
	 * @throws IOException
	 *             if problem getting file path, or problem evaluating file
	 */
	public void loadFileIntoEnv(File file, String environmentName)
			throws IOException {
		String filename = sanitiseFilePath(file.getCanonicalPath());

		parseEvalTry(".env <- attach(what = NULL, name='"
				+ ((environmentName == null) ? ".GlobalEnv" : environmentName)
				+ "')");

		try {
			parseEvalTry("sys.source('" + filename
					+ "', chdir=TRUE, keep.source=TRUE, .env)");
		} catch (RFaceException e) {
			throw new RFaceException(file.getCanonicalPath() + " "
					+ e.getMessage(), e);
		}

		parseEvalTry("rm(.env)");

	}

	/**
	 * Sanitise a file path for use in R by replacing "\" with "\\".
	 * 
	 * @param filePath
	 *            file path
	 * @return sanitised file path
	 */
	private String sanitiseFilePath(String filePath) {
		return filePath.replace(String.valueOf(File.separatorChar),
				String.valueOf(File.separatorChar) + File.separatorChar);
	}

	/**
	 * Get the working directory.
	 * 
	 * @return working directory
	 * @throws RFaceException
	 *             if problem getting directory
	 */
	public String getWd() throws RFaceException {
		return evalReturnString("getwd()");
	}

	/**
	 * Set the working directory. NB: this sets the working directory in not
	 * just R, but the java application environment.
	 * 
	 * @param dir
	 *            working directory
	 * @throws RFaceException
	 *             if problem setting directory
	 */
	public void setWd(String dir) throws RFaceException {
		parseEvalTry("setwd(\"" + sanitiseFilePath(dir) + "\")");
	}
}
