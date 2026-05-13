@echo off
setlocal

set "CP=.;mysql-connector-j-8.3.0.jar;slf4j-api-1.7.36.jar;slf4j-nop-1.7.36.jar"

echo [INFO] Compiling DBViewer...
javac -cp "%CP%" DBViewer.java

echo [INFO] Reading MySQL Database...
echo.
java -cp "%CP%" DBViewer

echo.
pause
endlocal
