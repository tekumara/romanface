/**
 * High level R interface for Java that extends 
 * {@link org.rosuda.REngine.REngine}.
 *
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
 * @author Oliver Mannion
 *
 */
package org.omancode.r;