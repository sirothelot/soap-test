# Spring-WS SOAP Web Service Demo

This project demonstrates the **same Calculator SOAP web service** as the JAX-WS version,
but implemented using **Spring Web Services (Spring-WS)** with Spring Boot.

Compare this side-by-side with the `../JAX-WS/` project to see the differences.

## Key Differences from JAX-WS

| Aspect            | JAX-WS                                   | Spring-WS                                    |
| ----------------- | ---------------------------------------- | -------------------------------------------- |
| **Approach**      | Code-first (Java -> WSDL)                | Contract-first (XSD -> WSDL -> Java)         |
| **Schema**        | Auto-generated from annotations          | You write `calculator.xsd`                   |
| **JAXB classes**  | Not needed (uses Java types)             | Generated from XSD by `jaxb2-maven-plugin`   |
| **Server class**  | `SoapServer.java` + `Endpoint.publish()` | `SoapApplication.java` (Spring Boot)         |
| **Service class** | `@WebService` interface + impl           | `@Endpoint` class with `@PayloadRoot`        |
| **Config**        | Annotations on interface                 | `WebServiceConfig.java` (`@Configuration`)   |
| **Client**        | `Service.getPort()` proxy                | `WebServiceTemplate.marshalSendAndReceive()` |
| **WSDL URL**      | `?wsdl` suffix                           | `.wsdl` suffix                               |
| **Web server**    | JDK built-in HTTP server                 | Embedded Tomcat (Spring Boot)                |
| **Packaging**     | JAR/WAR                                  | Executable JAR (Spring Boot)                 |

## File-by-File Comparison

```
JAX-WS                                    Spring-WS
------                                    ---------
CalculatorService.java     (interface)    calculator.xsd          (schema)
                                          com.example.service.gen (generated JAXB classes)
CalculatorServiceImpl.java (@WebService)  CalculatorEndpoint.java (@Endpoint)
SoapServer.java            (Endpoint)     SoapApplication.java    (Spring Boot)
                                          WebServiceConfig.java   (@Configuration)
SoapClient.java            (proxy)        SoapClient.java         (WebServiceTemplate)

Security:
SecurityConstants.java     (shared)       SecurityConstants.java  (shared)
ClientPasswordCallbackHandler.java        PasswordCallbackInterceptor.java
ServerPasswordCallbackHandler.java        Wss4jSecurityInterceptor (built-in)
```

## Project Structure

```
Spring-WS/
├── pom.xml                                # Maven + Spring Boot + JAXB plugin
├── mvnw.ps1                               # Maven wrapper
├── README.md                              # This file
└── src/main/
    ├── java/com/example/
    │   ├── SoapApplication.java           # Spring Boot entry point
    │   ├── config/
    │   │   └── WebServiceConfig.java      # WSDL + servlet + security config
    │   ├── security/
    │   │   └── SecurityConstants.java     # Shared credentials
    │   ├── service/
    │   │   ├── CalculatorEndpoint.java    # @Endpoint (handles SOAP requests)
    │   │   └── DivisionByZeroException.java # SOAP Fault mapping example
    │   └── client/
    │       ├── SoapClient.java            # WebServiceTemplate client
    │       └── PasswordCallbackInterceptor.java # Password callback bridge
    └── resources/
        ├── application.properties          # Spring Boot config
        └── calculator.xsd                  # XSD schema (contract-first!)
```

## How to Run

### Step 1: Build the Project

```bash
.\mvnw.ps1 clean compile
```

This also generates JAXB classes from `calculator.xsd` into `target/generated-sources/jaxb/`.

### Step 1.5: Generate Keystores (required for WS-Security)

Keystores are not committed to the repo. Generate them once after cloning:

```bash
..\generate-keystores.ps1
```

### Step 2: Start the Server

```bash
.\mvnw.ps1 spring-boot:run
```

You should see:

```
========================================
   Spring-WS SOAP Server Starting
========================================

[OK] Service published successfully!

Service URL: http://localhost:8080/ws
WSDL URL:    http://localhost:8080/ws/calculator.wsdl

Server is running... Press Ctrl+C to stop.
```

### Step 3: View the WSDL (Optional)

Open a browser and go to:

```
http://localhost:8080/ws/calculator.wsdl
```

### Step 4: Run the Client

Open **another terminal** and run:

```bash
.\mvnw.ps1 exec:java -D"exec.mainClass=com.example.client.SoapClient"
```

## Understanding the Key Differences

### 1. Contract-First vs Code-First

**JAX-WS (Code-First):**

```java
// You write this Java interface:
@WebService
public interface CalculatorService {
    @WebMethod int add(@WebParam(name="a") int a, @WebParam(name="b") int b);
}
// WSDL is auto-generated from the annotations
```

**Spring-WS (Contract-First):**

```xml
<!-- You write this XSD schema: -->
<xs:element name="addRequest">
  <xs:complexType>
    <xs:sequence>
      <xs:element name="a" type="xs:int"/>
      <xs:element name="b" type="xs:int"/>
    </xs:sequence>
  </xs:complexType>
</xs:element>
<!-- JAXB classes are generated from this, WSDL is generated from this -->
```

### 2. Server-Side Handling

**JAX-WS:**

```java
@WebService
public class CalculatorServiceImpl implements CalculatorService {
    public int add(int a, int b) { return a + b; }
}
```

**Spring-WS:**

```java
@Endpoint
public class CalculatorEndpoint {
    @PayloadRoot(namespace = NS, localPart = "addRequest")
    @ResponsePayload
    public AddResponse add(@RequestPayload AddRequest request) {
        AddResponse resp = new AddResponse();
        resp.setSum(request.getA() + request.getB());
        return resp;
    }
}
```

### 3. Client-Side Calling

**JAX-WS:**

```java
// Proxy-based: looks like calling a local method
CalculatorService calc = service.getPort(CalculatorService.class);
int result = calc.add(10, 5);  // Simple!
```

**Spring-WS:**

```java
// Template-based: create request objects, send, get response objects
AddRequest req = new AddRequest();
req.setA(10); req.setB(5);
AddResponse resp = (AddResponse) template.marshalSendAndReceive(req);
int result = resp.getSum();
```

## WS-Security (UsernameToken + Signature + Encryption)

This project includes WS-Security using **Apache WSS4J** via `spring-ws-security`.
Each request is authenticated, signed, and encrypted.

### What Happens

Every SOAP message gets a `<wsse:Security>` header with a username/password,
plus signature and encryption elements:

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

### How It Works in Spring-WS

Spring-WS uses **interceptors** (configured as Spring beans) instead of hand-written handlers:

**Server side** (`WebServiceConfig.java`):

```java
@Bean
public Wss4jSecurityInterceptor securityInterceptor() {
  Wss4jSecurityInterceptor interceptor = new Wss4jSecurityInterceptor();
  interceptor.setValidationActions("UsernameToken Signature Encrypt");
  interceptor.setValidationCallbackHandler(new ServerPasswordCallbackHandler());
  interceptor.setValidationSignatureCrypto(serverCrypto());
  interceptor.setValidationDecryptionCrypto(serverCrypto());
  return interceptor;
}
```

**Client side** (`SoapClient.java`):

```java
Wss4jSecurityInterceptor interceptor = new Wss4jSecurityInterceptor();
interceptor.setSecurementActions("UsernameToken Signature Encrypt");
interceptor.setSecurementUsername("alice");
interceptor.setSecurementSignatureUser("client");
interceptor.setSecurementEncryptionUser("server");
template.setInterceptors(new ClientInterceptor[]{
  new PasswordCallbackInterceptor(),
  interceptor
});
```

`PasswordCallbackInterceptor` injects a WSS4J callback so different passwords
can be used for the UsernameToken and the signing key.

### JAX-WS vs Spring-WS Security Comparison

| Aspect            | JAX-WS                        | Spring-WS                             |
| ----------------- | ----------------------------- | ------------------------------------- |
| **Library**       | CXF + WSS4J                   | Apache WSS4J (via spring-ws-security) |
| **Server-side**   | Properties map + interceptor  | Bean configuration                    |
| **Client-side**   | Properties map + interceptor  | WebServiceTemplate interceptors       |
| **Approach**      | Pipeline interceptors         | Declarative configuration             |
| **Extensibility** | Add CXF features/interceptors | Change interceptor properties         |

## Raw SOAP Example (unencrypted)

With security disabled, an `add(5,3)` request looks like this:

```xml
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
  <S:Header/>
  <S:Body>
    <addRequest xmlns="http://service.example.com/">
      <a>5</a>
      <b>3</b>
    </addRequest>
  </S:Body>
</S:Envelope>
```

In this demo, WSS4J adds the security header and encrypts the body.

## SOAP Fault Example (divide by zero)

If you call `divide(10, 0)`, the server returns a SOAP Fault:

```xml
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
  <S:Body>
    <S:Fault>
      <faultcode>S:Client</faultcode>
      <faultstring>Cannot divide by zero!</faultstring>
    </S:Fault>
  </S:Body>
</S:Envelope>
```

## Logging + Timeouts

- **Payload logging** is enabled in
  [Spring-WS/src/main/java/com/example/config/WebServiceConfig.java](Spring-WS/src/main/java/com/example/config/WebServiceConfig.java).
- **Client timeouts** are set in
  [Spring-WS/src/main/java/com/example/client/SoapClient.java](Spring-WS/src/main/java/com/example/client/SoapClient.java).

**Note on schema validation:** `PayloadValidatingInterceptor` is not used because
it's incompatible with WS-Security encryption. After decryption, the DOM tree
contains residual security artifacts that cause validation errors. Schema
conformance is still enforced by JAXB unmarshalling and the XSD-generated WSDL.

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

**In Spring-WS**, you enable MTOM on the message factory:

```java
@Bean
public SaajSoapMessageFactory messageFactory() {
    SaajSoapMessageFactory factory = new SaajSoapMessageFactory();
    factory.setMtomEnabled(true);
    return factory;
}
```

Then use `DataHandler` fields in your JAXB-generated classes (defined in the XSD
with `xmime:expectedContentTypes`).

This demo doesn't include MTOM because a calculator service has no binary data.
MTOM is relevant for document management, file upload, or image processing services.

### MTOM and Encryption

MTOM attachments travel **outside** the SOAP XML envelope as separate MIME parts,
so WS-Security XML Encryption (used in this demo) **does not cover them** — it only
encrypts the XML body containing the `<xop:Include>` reference.

Options for securing MTOM data:

| Approach                         | Description                                                                                         |
| -------------------------------- | --------------------------------------------------------------------------------------------------- |
| **TLS (HTTPS)**                  | Encrypt the entire HTTP connection — covers all MIME parts. Simplest and most common.               |
| **Disable MTOM when encrypting** | Send everything inline as Base64 so encryption covers it.                                           |
| **Attachment encryption**        | Spring-WS defers to WSS4J, which supports the OASIS WSS Attachment Profile for per-part encryption. |

The recommended production approach is **HTTPS + WS-Security**: TLS handles
transport confidentiality (including attachments), while WS-Security provides
authentication, integrity, and non-repudiation at the message level.
