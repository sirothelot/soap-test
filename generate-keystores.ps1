# ==========================================================================
#  KEYSTORE GENERATION SCRIPT
# ==========================================================================
#
#  This script creates the cryptographic keystores needed for
#  WS-Security message signing and encryption.
#
#  WHAT ARE KEYSTORES?
#  ===================
#  A keystore is a file that holds cryptographic keys and certificates.
#  Think of it like a secure wallet:
#    - KEYSTORE  = holds YOUR private key + certificate (your ID card)
#    - TRUSTSTORE = holds OTHER people's certificates (IDs you trust)
#
#  WHY DO WE NEED THEM?
#  ====================
#  Digital signatures and encryption use PUBLIC KEY CRYPTOGRAPHY:
#
#    SIGNING:    Sender signs with THEIR private key
#                Receiver verifies with sender's PUBLIC key (from truststore)
#
#    ENCRYPTION: Sender encrypts with RECEIVER's public key (from truststore)
#                Receiver decrypts with THEIR private key (from keystore)
#
#  This script creates 4 files:
#
#    client-keystore.p12   = client's private key + certificate
#    server-keystore.p12   = server's private key + certificate
#    client-truststore.p12 = server's certificate (client trusts server)
#    server-truststore.p12 = client's certificate (server trusts client)
#
#  The flow:
#
#    Client sending a request:
#    1. Signs with client's private key     (from client-keystore.p12)
#    2. Encrypts with server's public key   (from client-truststore.p12)
#
#    Server receiving a request:
#    1. Decrypts with server's private key  (from server-keystore.p12)
#    2. Verifies signature with client's public key (from server-truststore.p12)
#
# ==========================================================================

$ErrorActionPreference = "Stop"

# Passwords (in production, use strong passwords from a vault)
$CLIENT_STORE_PASS = "clientpass"
$SERVER_STORE_PASS = "serverpass"
$CLIENT_KEY_PASS   = "clientpass"
$SERVER_KEY_PASS   = "serverpass"

# Aliases (names for the keys inside the keystores)
$CLIENT_ALIAS = "client"
$SERVER_ALIAS = "server"

# Create keystores in both project directories
$directories = @(
    "JAX-WS/src/main/resources/keystores",
    "Spring-WS/src/main/resources/keystores"
)

foreach ($dir in $directories) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Generating keystores in $dir"
    Write-Host "========================================" -ForegroundColor Cyan

    # Create directory
    New-Item -ItemType Directory -Force -Path $dir | Out-Null

    # Clean up any existing files
    Remove-Item -Path "$dir/*.p12" -ErrorAction SilentlyContinue
    Remove-Item -Path "$dir/*.cer" -ErrorAction SilentlyContinue

    # -----------------------------------------------------------------------
    # Step 1: Generate CLIENT key pair (private key + self-signed certificate)
    # -----------------------------------------------------------------------
    Write-Host ""
    Write-Host "Step 1: Generating client key pair..." -ForegroundColor Yellow
    & keytool -genkeypair `
        -alias $CLIENT_ALIAS `
        -keyalg RSA `
        -keysize 2048 `
        -validity 365 `
        -keystore "$dir/client-keystore.p12" `
        -storepass $CLIENT_STORE_PASS `
        -keypass $CLIENT_KEY_PASS `
        -dname "CN=Client, O=Demo, L=Helsinki, C=FI" `
        -storetype PKCS12 `
        2>&1 | Out-Null
    Write-Host "  [OK] client-keystore.p12 created (client's private key + certificate)"

    # -----------------------------------------------------------------------
    # Step 2: Generate SERVER key pair (private key + self-signed certificate)
    # -----------------------------------------------------------------------
    Write-Host ""
    Write-Host "Step 2: Generating server key pair..." -ForegroundColor Yellow
    & keytool -genkeypair `
        -alias $SERVER_ALIAS `
        -keyalg RSA `
        -keysize 2048 `
        -validity 365 `
        -keystore "$dir/server-keystore.p12" `
        -storepass $SERVER_STORE_PASS `
        -keypass $SERVER_KEY_PASS `
        -dname "CN=Server, O=Demo, L=Helsinki, C=FI" `
        -storetype PKCS12 `
        2>&1 | Out-Null
    Write-Host "  [OK] server-keystore.p12 created (server's private key + certificate)"

    # -----------------------------------------------------------------------
    # Step 3: Export CLIENT certificate (public key only)
    # -----------------------------------------------------------------------
    Write-Host ""
    Write-Host "Step 3: Exporting client certificate..." -ForegroundColor Yellow
    & keytool -exportcert `
        -alias $CLIENT_ALIAS `
        -keystore "$dir/client-keystore.p12" `
        -storepass $CLIENT_STORE_PASS `
        -file "$dir/client.cer" `
        2>&1 | Out-Null
    Write-Host "  [OK] client.cer exported"

    # -----------------------------------------------------------------------
    # Step 4: Export SERVER certificate (public key only)
    # -----------------------------------------------------------------------
    Write-Host ""
    Write-Host "Step 4: Exporting server certificate..." -ForegroundColor Yellow
    & keytool -exportcert `
        -alias $SERVER_ALIAS `
        -keystore "$dir/server-keystore.p12" `
        -storepass $SERVER_STORE_PASS `
        -file "$dir/server.cer" `
        2>&1 | Out-Null
    Write-Host "  [OK] server.cer exported"

    # -----------------------------------------------------------------------
    # Step 5: Import SERVER cert into CLIENT truststore
    #         (so client can verify server's signatures and encrypt for server)
    # -----------------------------------------------------------------------
    Write-Host ""
    Write-Host "Step 5: Creating client truststore (with server's cert)..." -ForegroundColor Yellow
    & keytool -importcert `
        -alias $SERVER_ALIAS `
        -keystore "$dir/client-truststore.p12" `
        -storepass $CLIENT_STORE_PASS `
        -file "$dir/server.cer" `
        -noprompt `
        -storetype PKCS12 `
        2>&1 | Out-Null
    Write-Host "  [OK] client-truststore.p12 created (trusts server)"

    # -----------------------------------------------------------------------
    # Step 6: Import CLIENT cert into SERVER truststore
    #         (so server can verify client's signatures and encrypt for client)
    # -----------------------------------------------------------------------
    Write-Host ""
    Write-Host "Step 6: Creating server truststore (with client's cert)..." -ForegroundColor Yellow
    & keytool -importcert `
        -alias $CLIENT_ALIAS `
        -keystore "$dir/server-truststore.p12" `
        -storepass $SERVER_STORE_PASS `
        -file "$dir/client.cer" `
        -noprompt `
        -storetype PKCS12 `
        2>&1 | Out-Null
    Write-Host "  [OK] server-truststore.p12 created (trusts client)"

    # Clean up temporary certificate files
    Remove-Item -Path "$dir/client.cer"
    Remove-Item -Path "$dir/server.cer"

    Write-Host ""
    Write-Host "  Summary of $dir :" -ForegroundColor Green
    Write-Host "    client-keystore.p12    - client's private key (signs outgoing messages)"
    Write-Host "    client-truststore.p12  - server's public cert (encrypts FOR server)"
    Write-Host "    server-keystore.p12    - server's private key (decrypts incoming messages)"
    Write-Host "    server-truststore.p12  - client's public cert (verifies client signatures)"
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  All keystores generated successfully!"
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "These keystores enable:"
Write-Host "  1. DIGITAL SIGNATURES - prove messages weren't tampered with"
Write-Host "  2. ENCRYPTION - prevent anyone from reading message contents"
Write-Host "  3. AUTHENTICATION - verify who sent the message"
Write-Host ""
