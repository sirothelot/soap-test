@echo off
setlocal

set MVNW_REPOURL=https://repo.maven.apache.org/maven2
set WRAPPER_VERSION=3.2.0
set MAVEN_VERSION=3.9.6

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "WRAPPER_DIR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper"
set "WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar"
set "WRAPPER_PROPS=%WRAPPER_DIR%\maven-wrapper.properties"

if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"

if not exist "%WRAPPER_PROPS%" (
    echo distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip > "%WRAPPER_PROPS%"
)

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper...
    powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%MVNW_REPOURL%/org/apache/maven/wrapper/maven-wrapper/%WRAPPER_VERSION%/maven-wrapper-%WRAPPER_VERSION%.jar' -OutFile '%WRAPPER_JAR%'}"
)

set JAVA_CMD=java
if defined JAVA_HOME set JAVA_CMD=%JAVA_HOME%\bin\java.exe

"%JAVA_CMD%" -jar "%WRAPPER_JAR%" %*

endlocal
