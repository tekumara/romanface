@echo off
@REM runs romanface.jar with path to R and leaves console window open so 
@REM any console output can be viewed

PATH=%PATH%;C:\Program Files\R\R-2.11.1\bin
java -Djava.library.path="C:\Program Files\R\R-2.11.1\library\rJava\jri" -jar romanface*.jar 

pause