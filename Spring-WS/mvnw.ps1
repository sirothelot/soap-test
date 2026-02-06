# Maven Wrapper for PowerShell
# This script downloads and runs Maven if it's not installed

$ErrorActionPreference = "Stop"

$MAVEN_VERSION = "3.9.6"
$MAVEN_BASE_URL = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven"
$MAVEN_HOME_DIR = Join-Path $env:USERPROFILE ".m2\wrapper\dists\apache-maven-$MAVEN_VERSION"
$MAVEN_ZIP = Join-Path $MAVEN_HOME_DIR "apache-maven-$MAVEN_VERSION-bin.zip"
$MAVEN_DIR = Join-Path $MAVEN_HOME_DIR "apache-maven-$MAVEN_VERSION"
$MVN_CMD = Join-Path $MAVEN_DIR "bin\mvn.cmd"

# Check if Maven is already downloaded
if (-not (Test-Path $MVN_CMD)) {
    Write-Host "Maven not found. Downloading Maven $MAVEN_VERSION..."
    
    # Create directory
    if (-not (Test-Path $MAVEN_HOME_DIR)) {
        New-Item -ItemType Directory -Path $MAVEN_HOME_DIR -Force | Out-Null
    }
    
    # Download Maven
    $downloadUrl = "$MAVEN_BASE_URL/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"
    Write-Host "Downloading from: $downloadUrl"
    
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $downloadUrl -OutFile $MAVEN_ZIP
    
    # Extract
    Write-Host "Extracting Maven..."
    Expand-Archive -Path $MAVEN_ZIP -DestinationPath $MAVEN_HOME_DIR -Force
    
    Write-Host "Maven installed successfully!"
    Write-Host ""
}

# Run Maven with the provided arguments
& $MVN_CMD $args
