#these tests are used to test how well a REPL imitator (such as svMisc::captureAll
#or evaluate::evaluate or our own pcr function)  mimics console output 

cat("1\n")
print("2")
warning("3")
print("4")
message("5")
stop("6")
stop("7", call. = FALSE)

f <- function(x) {
  print("8")
  message("9")
  warning("10")
  stop("11")
}
f()

#warning
1:2 + 1:3
#warning produced in this function but is muted by it
mutedWarning <- function() {
	old <- options(warn = -1)
    on.exit(options(old))
    !any(is.na(as.numeric(c("F","M"))))
}
mutedWarning()

#source("D:\\workspace\\org.omancode.r.test\\REPL tests.r")