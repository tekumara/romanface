**I've built romanface from the sources but when I run RSwingConsoleExample the R console does not respond to my commands**

This occurs if you are using bsh-2.0b4.jar on your classpath. There is a bug in this version of BeanShell that prevents end of line detection so your commands are never submitted to R. Please:
  * build BeanShell using the bsh.xml ant script in the `buildsupport/thirdparty/beanshell` directory in SVN which patches this, or
  * use the already built `bsh-2.0b4patched.jar` included in the binaries + dependencies zip download.