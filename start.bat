@echo off

javac -cp ".;mysql-connector-j-8.3.0.jar" *.java

java -cp ".;mysql-connector-j-8.3.0.jar" WebServer

pause