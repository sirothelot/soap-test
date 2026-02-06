# SOAP Web Service Demo

Side-by-side comparison of two approaches to building the **same SOAP web service** in Java:

|               | [JAX-WS](JAX-WS/)                        | [Spring-WS](Spring-WS/)                      |
| ------------- | ---------------------------------------- | -------------------------------------------- |
| **Approach**  | Code-first (Java → WSDL)                 | Contract-first (XSD → WSDL → Java)           |
| **Framework** | Jakarta XML Web Services                 | Spring Web Services + Spring Boot            |
| **Service**   | `@WebService` interface + implementation | `@Endpoint` with `@PayloadRoot` methods      |
| **Client**    | Proxy via `Service.getPort()`            | `WebServiceTemplate.marshalSendAndReceive()` |
| **Server**    | `Endpoint.publish()` (JDK HTTP server)   | Embedded Tomcat (Spring Boot)                |

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
