# JAX-WS SOAP Web Service Demo

This project demonstrates how SOAP web services work using Java and JAX-WS (Jakarta XML Web Services).
It uses **Apache CXF** as the runtime so WS-Security encryption can be processed
before request dispatch.

## What is SOAP?

**SOAP (Simple Object Access Protocol)** is a messaging protocol that allows programs running on different operating systems to communicate using HTTP and XML.

### Key Concepts

```
┌─────────────────┐                          ┌─────────────────┐
│     CLIENT      │                          │     SERVER      │
│                 │     SOAP Request         │                 │
│  calculator.    │  ─────────────────────►  │  Receives XML   │
│    add(5, 3)    │     (XML via HTTP)       │  Calls add(5,3) │
│                 │                          │  Returns 8      │
│  Gets result: 8 │  ◄─────────────────────  │                 │
│                 │     SOAP Response        │                 │
└─────────────────┘     (XML via HTTP)       └─────────────────┘
```

### SOAP Message Structure

```xml
<?xml version="1.0" ?>
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
  <S:Header>
    <!-- Optional metadata (authentication, etc.) -->
  </S:Header>
  <S:Body>
    <!-- The actual request/response data -->
    <add xmlns="http://service.example.com/">
      <a>5</a>
      <b>3</b>
    </add>
  </S:Body>
</S:Envelope>
```

### WSDL (Web Service Description Language)

WSDL is an XML document that describes a web service:

- **What operations** are available (add, subtract, multiply, divide)
- **What data types** are used (integers, doubles)
- **Where** the service is located (URL)
- **How** to call it (SOAP binding)

Think of WSDL as an API documentation in machine-readable format.

## Project Structure

```
JAX-WS/
├── pom.xml                           # Maven build configuration
├── README.md                         # This file
└── src/main/java/com/example/
    ├── service/
    │   ├── CalculatorService.java     # Service Interface (SEI)
    │   └── CalculatorServiceImpl.java # Service Implementation
    ├── security/
    │   ├── SecurityConstants.java     # Shared credentials
    │   ├── ClientPasswordCallbackHandler.java # Supplies passwords for WSS4J
    │   └── ServerPasswordCallbackHandler.java # Supplies passwords for WSS4J
    ├── server/
    │   └── SoapServer.java            # Publishes the web service
    └── client/
        └── SoapClient.java            # Consumes the web service
```

## How to Run

### Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

### Step 1: Build the Project

```bash
# On Windows (PowerShell)
.\mvnw.ps1 clean compile

# On Windows (CMD) / Linux / Mac (if Maven is installed)
mvn clean compile
```

### Step 1.5: Generate Keystores (required for WS-Security)

Keystores are not committed to the repo. Generate them once after cloning:

```bash
..\generate-keystores.ps1
```

### Step 2: Start the Server

Open a terminal and run:

```bash
# On Windows (PowerShell)
.\mvnw.ps1 exec:java -D"exec.mainClass=com.example.server.SoapServer"

# On Windows (CMD) / Linux / Mac (if Maven is installed)
mvn exec:java -Dexec.mainClass="com.example.server.SoapServer"
```

You should see:

```
========================================
   SOAP Web Service Server Starting
========================================

[OK] Service published successfully!

Service URL: http://localhost:8080/calculator
WSDL URL:    http://localhost:8080/calculator?wsdl

Server is running... Press Ctrl+C to stop.
```

### Step 3: View the WSDL (Optional)

Open a browser and go to:

```
http://localhost:8080/calculator?wsdl
```

This shows the XML description of the service.

### Step 4: Run the Client

Open **another terminal** and run:

```bash
# On Windows (PowerShell)
.\mvnw.ps1 exec:java -D"exec.mainClass=com.example.client.SoapClient"

# On Windows (CMD) / Linux / Mac (if Maven is installed)
mvn exec:java -Dexec.mainClass="com.example.client.SoapClient"
```

You should see the client making requests and receiving responses.

## Understanding the Code

### 1. Service Interface (`CalculatorService.java`)

```java
@WebService  // Marks this as a web service
public interface CalculatorService {

    @WebMethod  // Marks this as a web service operation
    int add(@WebParam(name = "a") int a,
            @WebParam(name = "b") int b);
}
```

### 2. Service Implementation (`CalculatorServiceImpl.java`)

```java
@WebService(endpointInterface = "com.example.service.CalculatorService")
public class CalculatorServiceImpl implements CalculatorService {

    @Override
    public int add(int a, int b) {
        return a + b;  // Actual business logic
    }
}
```

### 3. Publishing the Service (`SoapServer.java`)

```java
// Create the service implementation
CalculatorServiceImpl service = new CalculatorServiceImpl();

// Publish at a URL - JAX-WS handles everything else!
Endpoint.publish("http://localhost:8080/calculator", service);
```

### 4. Consuming the Service (`SoapClient.java`)

```java
// Point to the WSDL
URL wsdlUrl = new URL("http://localhost:8080/calculator?wsdl");

// Create a service from the WSDL
Service service = Service.create(wsdlUrl, serviceName);

// Get a proxy that implements the interface
CalculatorService calc = service.getPort(CalculatorService.class);

// Call methods like normal Java!
int result = calc.add(5, 3);  // Returns 8
```

## What Happens Behind the Scenes?

When you call `calc.add(5, 3)`:

1. **JAX-WS creates a SOAP request:**

   ```xml
   <Envelope>
     <Body>
       <add>
         <a>5</a>
         <b>3</b>
       </add>
     </Body>
   </Envelope>
   ```

2. **Sends it via HTTP POST** to `http://localhost:8080/calculator`

3. **Server receives and parses** the XML

4. **Server calls** `add(5, 3)` on the implementation

5. **Server creates a SOAP response:**

   ```xml
   <Envelope>
     <Body>
       <addResponse>
         <sum>8</sum>
       </addResponse>
     </Body>
   </Envelope>
   ```

6. **Client receives and parses** the response

7. **Returns `8`** from the `add()` method call

All this XML handling is automatic!

## WS-Security (UsernameToken + Signature + Encryption)

This project includes a working WS-Security example that adds authentication, message integrity, and confidentiality to every SOAP message.

### What is WS-Security?

WS-Security is a standard for adding security **inside** the SOAP message itself (message-level security), as opposed to relying only on HTTPS (transport-level security).

```xml
<S:Envelope>
  <S:Header>
    <wsse:Security>
      <wsse:UsernameToken>...</wsse:UsernameToken>
      <ds:Signature>...</ds:Signature>
      <xenc:EncryptedData>...</xenc:EncryptedData>
    </wsse:Security>
  </S:Header>
  <S:Body>...</S:Body>
</S:Envelope>
```

### How It Works in JAX-WS (CXF + WSS4J)

This project uses **Apache CXF** as the JAX-WS runtime. CXF wires **WSS4J interceptors**
into the request/response pipeline, so security happens **before** the message is dispatched.

```
Client                          Server
  │                               │
  │  WSS4JOutInterceptor           │  WSS4JInInterceptor
  │  adds UsernameToken            │  decrypts + verifies
  │  signs + encrypts        ───►  │  validates credentials
  │                               │  then dispatches
```

| File                                 | Role                                                            |
| ------------------------------------ | --------------------------------------------------------------- |
| `SecurityConstants.java`             | Shared credentials + keystore settings                          |
| `ClientPasswordCallbackHandler.java` | Supplies passwords to WSS4J for UsernameToken + signing         |
| `ServerPasswordCallbackHandler.java` | Supplies passwords to WSS4J for UsernameToken + decryption      |
| `SoapClient.java`                    | Configures WSS4JOutInterceptor (UsernameToken + Sign + Encrypt) |
| `SoapServer.java`                    | Configures WSS4JInInterceptor (Decrypt + Verify + Validate)     |

### Key Point

CXF avoids the classic JAX-WS + encryption dispatch issue by decrypting **before** routing.
Spring-WS solves the same problem with a custom endpoint mapping.

## Raw SOAP Example (unencrypted)

With security disabled, an `add(5,3)` request looks like this:

```xml
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
  <S:Header/>
  <S:Body>
    <add xmlns="http://service.example.com/">
      <a>5</a>
      <b>3</b>
    </add>
  </S:Body>
</S:Envelope>
```

In this demo, WSS4J adds a `<wsse:Security>` header and encrypts the body.

## SOAP Fault Example (divide by zero)

If you call `divide(10, 0)`, the server returns a SOAP Fault:

```xml
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
  <S:Body>
    <S:Fault>
      <faultcode>S:Client</faultcode>
      <faultstring>Cannot divide by zero!</faultstring>
      <detail>
        <error xmlns="http://service.example.com/">DIVIDE_BY_ZERO</error>
      </detail>
    </S:Fault>
  </S:Body>
</S:Envelope>
```

## Validation + Logging + Timeouts

- **Schema validation** is enabled via `@SchemaValidation` on
  [JAX-WS/src/main/java/com/example/service/CalculatorServiceImpl.java](JAX-WS/src/main/java/com/example/service/CalculatorServiceImpl.java).
- **SOAP logging** is enabled with CXF logging interceptors (server + client).
- **Client timeouts** are set in
  [JAX-WS/src/main/java/com/example/client/SoapClient.java](JAX-WS/src/main/java/com/example/client/SoapClient.java).

## Configuration via Environment Variables

You can override demo credentials without changing code:

- `SOAP_USERNAME`
- `SOAP_PASSWORD`
- `SOAP_CLIENT_KEY_PASSWORD`
- `SOAP_SERVER_KEY_PASSWORD`

## Security Certificates in Production

This demo uses **self-signed certificates** for simplicity. In production:

- Use **CA-signed certificates** from a trusted Certificate Authority (e.g., Let's Encrypt, DigiCert)
- The keystore/truststore setup is the same — you just replace the self-signed certs with CA-issued ones
- Consider using a secrets manager (e.g., HashiCorp Vault, AWS Secrets Manager) instead of `.p12` files on disk

## Code-First vs Contract-First

This demo uses **code-first** (Java → WSDL) to contrast with Spring-WS's contract-first approach.
However, JAX-WS fully supports **contract-first** via `wsimport`:

```bash
# Generate Java classes from an existing WSDL
wsimport -keep -s src/main/java http://example.com/service?wsdl
```

This generates the service interface, JAXB types, and `Service` factory classes
from a WSDL — useful when you're consuming a third-party SOAP service or when
the schema is defined before the implementation.

## What About MTOM?

**MTOM (Message Transmission Optimization Mechanism)** is a SOAP standard for sending
binary data (files, images, PDFs) efficiently as MIME attachments instead of Base64-encoding
them into the XML body.

Without MTOM, a 10 MB file gets Base64-encoded (+33% size) and embedded in the SOAP body.
With MTOM, the binary data travels as a raw MIME attachment alongside the SOAP envelope:

```
MIME Boundary
├── Part 1: SOAP Envelope (XML with <xop:Include href="cid:attachment"/>)
└── Part 2: Binary data (raw bytes, no encoding overhead)
```

**In JAX-WS**, you enable MTOM with one annotation:

```java
@MTOM
@WebService
public class FileService {
    @WebMethod
    public void upload(@WebParam(name = "file") DataHandler file) { ... }
}
```

This demo doesn't include MTOM because a calculator service has no binary data.
MTOM is relevant for document management, file upload, or image processing services.

### MTOM and Encryption

MTOM attachments travel **outside** the SOAP XML envelope as separate MIME parts,
so WS-Security XML Encryption (used in this demo) **does not cover them** — it only
encrypts the XML body containing the `<xop:Include>` reference.

Options for securing MTOM data:

| Approach                         | Description                                                                                                                       |
| -------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| **TLS (HTTPS)**                  | Encrypt the entire HTTP connection — covers all MIME parts. Simplest and most common.                                             |
| **Disable MTOM when encrypting** | Send everything inline as Base64 so encryption covers it.                                                                         |
| **CXF attachment encryption**    | Configure `encryptionParts` to include `cid:Attachments` — encrypts MIME parts individually via the OASIS WSS Attachment Profile. |

The recommended production approach is **HTTPS + WS-Security**: TLS handles
transport confidentiality (including attachments), while WS-Security provides
authentication, integrity, and non-repudiation at the message level.

## SOAP vs REST

| Feature        | SOAP                        | REST                    |
| -------------- | --------------------------- | ----------------------- |
| Protocol       | Strict XML-based protocol   | Architectural style     |
| Message Format | XML only                    | JSON, XML, or others    |
| Contract       | WSDL (formal contract)      | Usually OpenAPI/Swagger |
| Transport      | HTTP, SMTP, TCP             | Usually HTTP            |
| Overhead       | Higher (XML verbose)        | Lower (JSON compact)    |
| Use Cases      | Enterprise, banking, legacy | Web APIs, mobile apps   |

## Troubleshooting

### "Connection refused" error

Make sure the server is running before starting the client.

### Port already in use

Change the port in `SoapServer.java` from 8080 to another port.

### Build errors

Run `mvn clean compile` to rebuild from scratch.

## Learn More

- [JAX-WS Tutorial](https://eclipse-ee4j.github.io/metro-jax-ws/)
- [SOAP Specification](https://www.w3.org/TR/soap/)
- [WSDL Specification](https://www.w3.org/TR/wsdl/)
