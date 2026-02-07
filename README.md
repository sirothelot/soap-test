# SOAP Web Service Demo

Side-by-side comparison of two approaches to building the **same SOAP web service** in Java:

|               | [JAX-WS](JAX-WS/)                        | [Spring-WS](Spring-WS/)                      |
| ------------- | ---------------------------------------- | -------------------------------------------- |
| **Approach**  | Code-first (Java → WSDL)                 | Contract-first (XSD → WSDL → Java)           |
| **Framework** | Jakarta XML Web Services                 | Spring Web Services + Spring Boot            |
| **Service**   | `@WebService` interface + implementation | `@Endpoint` with `@PayloadRoot` methods      |
| **Client**    | Proxy via `Service.getPort()`            | `WebServiceTemplate.marshalSendAndReceive()` |
| **Server**    | `Endpoint.publish()` (JDK HTTP server)   | Embedded Tomcat (Spring Boot)                |
| **Security**  | CXF + WSS4J interceptors (map config)    | `Wss4jSecurityInterceptor` (bean config)     |

Both projects implement a **Calculator** service with four operations: add, subtract, multiply, and divide.

## Quick Start

Each project has its own `mvnw.ps1` Maven wrapper — no Maven installation needed.

**JAX-WS:**

```powershell
cd JAX-WS
.\mvnw.ps1 clean compile
.\mvnw.ps1 exec:java -D"exec.mainClass=com.example.server.SoapServer"    # terminal 1
.\mvnw.ps1 exec:java -D"exec.mainClass=com.example.client.SoapClient"    # terminal 2
```

**Spring-WS:**

```powershell
cd Spring-WS
.\mvnw.ps1 clean compile
.\mvnw.ps1 spring-boot:run                                                # terminal 1
.\mvnw.ps1 exec:java -D"exec.mainClass=com.example.client.SoapClient"    # terminal 2
```

See each project's README for detailed explanations and code comparisons.

## Keystores (WS-Security)

Keystores are intentionally **not** committed. Generate them after cloning:

```powershell
./generate-keystores.ps1
```

> **Production note:** This demo uses self-signed certificates for simplicity.
> In production, use CA-signed certificates from a trusted Certificate Authority
> (e.g., Let's Encrypt, DigiCert). The keystore/truststore setup is the same —
> you just replace the self-signed certs with CA-issued ones.

## What About MTOM?

**MTOM (Message Transmission Optimization Mechanism)** is a SOAP standard for
sending binary data (files, images, PDFs) as attachments without Base64-encoding
them into the XML body.

### Why It Matters

Normally, SOAP sends everything as XML text. If you need to send a 10 MB PDF,
it gets Base64-encoded (growing ~33% larger) and embedded directly in the SOAP
body. MTOM solves this by sending binary data as a **separate MIME part** alongside
the SOAP envelope, similar to email attachments:

```
MIME Boundary
├── Part 1: SOAP Envelope (XML with <xop:Include> reference)
└── Part 2: Binary attachment (raw bytes, not Base64)
```

### How It Would Work

**JAX-WS:** Add `@MTOM` annotation to the service + use `byte[]` or `DataHandler` parameters.
**Spring-WS:** Set `mtomEnabled=true` on the `Saaj` message factory + use `DataHandler` in JAXB types.

This demo doesn't include MTOM because a calculator service has no binary data.
MTOM is relevant for document management, file upload/download, or image
processing services.

### MTOM and Encryption

MTOM attachments are sent **outside** the SOAP XML envelope as separate MIME parts.
This means WS-Security XML Encryption (used in this demo) **does not encrypt MTOM
attachments** — it only encrypts the XML body, which just contains an
`<xop:Include href="cid:..."/>` reference.

Options for securing MTOM data:

| Approach                         | Description                                                                           |
| -------------------------------- | ------------------------------------------------------------------------------------- |
| **TLS (HTTPS)**                  | Encrypt the entire HTTP connection — covers all MIME parts. Simplest and most common. |
| **Disable MTOM when encrypting** | Send everything inline as Base64 in the XML body so encryption covers it.             |
| **Attachment encryption (CXF)**  | CXF supports the OASIS WSS Attachment Profile to encrypt MIME parts individually.     |

The recommended production approach is **HTTPS + WS-Security**: TLS encrypts
everything on the wire (including attachments), while WS-Security provides
authentication, integrity, and non-repudiation at the message level.

## Code-First vs Contract-First

| Aspect         | JAX-WS (this demo) | Spring-WS (this demo) | JAX-WS (alternative)        |
| -------------- | ------------------ | --------------------- | --------------------------- |
| **Approach**   | Code-first         | Contract-first        | Contract-first              |
| **Start with** | Java interface     | XSD schema            | WSDL + XSD                  |
| **Generate**   | WSDL from code     | WSDL from XSD         | Java from WSDL (`wsimport`) |

> **Note:** JAX-WS _can_ do contract-first using the `wsimport` tool (generates
> Java classes from a WSDL). This demo uses code-first to highlight the contrast
> with Spring-WS's contract-first approach.
