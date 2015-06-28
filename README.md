A high level R interface for Java that extends [org.rosuda.REngine.REngine](http://www.rforge.net/org/docs/org/rosuda/REngine/REngine.html) of the [Rserve project](http://www.rforge.net/Rserve/).

Includes

  * Single instance static interface to R (because R is single threaded) that includes:
    * Load and initialise functions.
    * Multiple methods for evaluating R expressions including with/without output of returned expressions or printed output, and with/without exception handling.
    * Evaluation of expressions from files.
    * Assignment of R objects.
    * Message output to the R console.
  * Java object representations of R matrices, dataframes, and vectors.
    * Conversion of Java bean collections and [Casper datasets](http://code.google.com/p/casperdatasets/) to R dataframes
    * Conversion of R dataframes, matrices and vectors to [Casper datasets](http://code.google.com/p/casperdatasets/)
  * GUI elements including:
    * A Swing console with a command history
    * A JTree representing objects in the R environment
    * A interface for UIs that gathers input from the user and executes an R command.

<p align='center'><img src='http://romanface.googlecode.com/svn/wiki/screenshots/RSwingConsoleExample.png' /></p>

<p align='center'>RSwingConsoleExample (see <a href='Usage.md'>Usage</a> for how to run)</p>
