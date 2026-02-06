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
    │   │   └── WebServiceConfig.java      # WSDL + servlet config
    │   ├── service/
    │   │   └── CalculatorEndpoint.java    # @Endpoint (handles SOAP requests)
    │   └── client/
    │       └── SoapClient.java            # WebServiceTemplate client
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
