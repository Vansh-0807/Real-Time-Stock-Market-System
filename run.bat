@echo off
setlocal

set "MYSQL_JAR=mysql-connector-j-8.3.0.jar"
set "MYSQL_URL=https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/%MYSQL_JAR%"

set "SLF4J_API_JAR=slf4j-api-1.7.36.jar"
set "SLF4J_API_URL=https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/%SLF4J_API_JAR%"

set "SLF4J_NOP_JAR=slf4j-nop-1.7.36.jar"
set "SLF4J_NOP_URL=https://repo1.maven.org/maven2/org/slf4j/slf4j-nop/1.7.36/%SLF4J_NOP_JAR%"

:: Check and download MySQL JDBC driver
if not exist "%MYSQL_JAR%" (
    echo [INFO] Downloading MySQL JDBC driver...
    curl -L -o "%MYSQL_JAR%" "%MYSQL_URL%"
    if errorlevel 1 (
        echo [ERROR] Failed to download MySQL JDBC driver.
        exit /b 1
    )
    echo [INFO] MySQL driver downloaded.
) else (
    echo [INFO] MySQL JDBC driver found.
)

:: Check and download SLF4J API
if not exist "%SLF4J_API_JAR%" (
    echo [INFO] Downloading SLF4J API...
    curl -L -o "%SLF4J_API_JAR%" "%SLF4J_API_URL%"
    if errorlevel 1 (
        echo [ERROR] Failed to download SLF4J API.
        exit /b 1
    )
    echo [INFO] SLF4J API downloaded.
) else (
    echo [INFO] SLF4J API found.
)

:: Check and download SLF4J NOP (No-operation logger implementation)
if not exist "%SLF4J_NOP_JAR%" (
    echo [INFO] Downloading SLF4J NOP...
    curl -L -o "%SLF4J_NOP_JAR%" "%SLF4J_NOP_URL%"
    if errorlevel 1 (
        echo [ERROR] Failed to download SLF4J NOP.
        exit /b 1
    )
    echo [INFO] SLF4J NOP downloaded.
) else (
    echo [INFO] SLF4J NOP found.
)

set "CP=.;%MYSQL_JAR%;%SLF4J_API_JAR%;%SLF4J_NOP_JAR%"

:: Compile the Java files
echo [INFO] Compiling Java files...
javac -cp "%CP%" *.java
if errorlevel 1 (
    echo [ERROR] Compilation failed.
    exit /b 1
)

:: Run the application
echo [INFO] Starting Application...
java -cp "%CP%" WebServer

endlocal
