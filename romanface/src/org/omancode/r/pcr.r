library(svMisc)

pcr <- function (stringExpr) {
	#pcr - parse, capture all, return 
	#based on svMisc::captureAll
	#takes a character string and parses and then evaluates it, 
	#reproducing the evaluation console output the same way as it 
	#would be done in a R console, and returns the value of the
	#last executed statement
		
	#override the warning function in TempEnv()
	assign("warning", .warningOverride, envir = TempEnv())
	
	on.exit({
				if (exists("warning", envir = TempEnv())) rm("warning", 
							envir = TempEnv())
			})
	
	#main evaluation loop
	#evaluate each statement in the expression individually one at a time
	tmp <- NULL
	parsedExpr <- svMisc::Parse(stringExpr)
	for (i in 1:length(parsedExpr)) {
		tmp <- .evalVis(parsedExpr[[i]])
		
		#check for error
		if (inherits(tmp, "try-error")) {
			
			#set last.warning to the "last.warning" attribute of tmp
			last.warning <- attr(tmp, "last.warning")
			
			if (!is.null(last.warning)) {
				cat(svMisc:::.gettext("In addition: "))
				.WarningMessage(last.warning)
			}
			break
		}
		else {
			if (tmp$visible) 
				print(tmp$value)
			last.warning <- attr(tmp, "last.warning")
			if (!is.null(last.warning)) 
				.WarningMessage(last.warning)
		}
	}
	
	if (!inherits(tmp, "try-error")) {
		return(tmp$value)
	}
}


.warningOverride <- function(..., call. = TRUE, immediate. = FALSE, 
		domain = NULL) {
	#used when warning() is called in stringExpr during evaluation by cap()
	args <- list(...)
	if (length(args) == 1 && inherits(args[[1]], "condition")) {
		base::warning(..., call. = call., immediate. = immediate., 
				domain = domain)
	}
	else {
		oldwarn <- getOption("warn")
		if (immediate. && oldwarn < 1) {
			options(warn = 1)
			on.exit(options(warn = oldwarn))
		}
		.Internal(warning(as.logical(call.), as.logical(immediate.), 
						.makeMessage(..., domain = domain)))
	}
}

.WarningMessage <- function(last.warning) {
	#.WarningMessage
	assign("last.warning", last.warning, envir = baseenv())
	n.warn <- length(last.warning)
	if (n.warn < 11) {
		print.warnings(warnings(" ", sep = ""))
	}
	else if (n.warn >= 50) {
		cat(svMisc:::.gettext("There were 50 or more warnings (use warnings() to see the first 50)\n"))
	}
	else {
		cat(svMisc:::.gettextf("There were %d warnings (use warnings() to see them)\n", 
						n.warn))
	}
	return(invisible(n.warn))
}

.evalVis <- function(Expr) {
	#.evalVis
	owarns <- getOption("warning.expression")
	options(warning.expression = expression())
	on.exit({
				nwarns <- getOption("warning.expression")
				if (!is.null(nwarns) && length(as.character(nwarns)) == 
						0) options(warning.expression = owarns)
			})
	
	res <- try(withCallingHandlers(.Internal(eval.with.vis(Expr, 
									.GlobalEnv, baseenv())), 
					warning = function(e) { .evalVisHandleWarning(e) }, 
					interrupt = function(i) cat(svMisc:::.gettext("<INTERRUPTED!>\n")), 
					error = function(e) { .evalVisHandleError(e) }, 
					message = function(e) { .evalVisHandleMessage(e) }
					), silent = TRUE)
	
	if (exists("warns", envir = TempEnv())) {
		warns <- get("warns", envir = TempEnv())
		last.warning <- lapply(warns, "[[", "call")
		names(last.warning) <- sapply(warns, "[[", "message")
		attr(res, "last.warning") <- last.warning
		rm("warns", envir = TempEnv())
	}
	return(res)
}

.evalVisHandleWarning <- function(e) {
	#.evalVisHandleWarning
	msg <- conditionMessage(e)
	call <- conditionCall(e)
	wl <- getOption("warning.length")
	if (is.null(wl)) 
		wl <- 1000
	if (nchar(msg) > wl) 
		msg <- paste(substr(msg, 1, wl), svMisc:::.gettext("[... truncated]"))
	Warn <- getOption("warn")
	if (!is.null(call) && identical(call[[1]], quote(eval.with.vis))) 
		e$call <- NULL
	if (Warn < 0) {
		return()
	}
	else if (Warn == 0) {
		if (exists("warns", envir = TempEnv())) {
			lwarn <- get("warns", envir = TempEnv())
		}
		else lwarn <- list()
		if (length(lwarn) >= 50) 
			return()
		assign("warns", append(lwarn, list(e)), envir = TempEnv())
		return()
	}
	else if (Warn > 1) {
		msg <- svMisc:::.gettextf("(converted from warning) %s", 
				msg)
		stop(simpleError(msg, call = call))
	}
	else {
		if (!is.null(call)) {
			dcall <- deparse(call)[1]
			prefix <- paste(svMisc:::.gettext("Warning in"), dcall, 
					": ")
			sm <- strsplit(msg, "\n")[[1]]
			if (nchar(dcall, type = "w") + nchar(sm[1], 
					type = "w") > 58) 
				prefix <- paste(prefix, "\n  ", sep = "")
		}
		else prefix <- svMisc:::.gettext("Warning : ")
		msg <- paste(prefix, msg, "\n", sep = "")
		cat(msg)
	}
}

.evalVisHandleError <- function(e) {
	#.evalVisHandleError
	call <- conditionCall(e)
	msg <- conditionMessage(e)
	if (!is.null(call) && identical(call[[1]], quote(eval.with.vis))) 
		call <- NULL
	if (!is.null(call)) {
		dcall <- deparse(call)[1]
		prefix <- paste(svMisc:::.gettext("Error in"), dcall, 
				": ")
		sm <- strsplit(msg, "\n")[[1]]
		if (nchar(dcall, type = "w") + nchar(sm[1], 
				type = "w") > 61) 
			prefix <- paste(prefix, "\n  ", sep = "")
	}
	else prefix <- svMisc:::.gettext("Error: ")
	msg <- paste(prefix, msg, "\n", sep = "")
	.Internal(seterrmessage(msg[1]))
	if (identical(getOption("show.error.messages"), 
			TRUE)) {
		cat(msg)
	}
}

.evalVisHandleMessage <- function(e) {
	#.evalVisHandleMessage
	signalCondition(e)
	conditionMessage(e)
}