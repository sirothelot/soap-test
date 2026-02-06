# JAX-WS SOAP Web Service Demo

This project demonstrates how SOAP web services work using Java and JAX-WS (Jakarta XML Web Services).

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
soap-test/
├── pom.xml                           # Maven build configuration
├── README.md                         # This file
└── src/main/java/com/example/
    ├── service/
    │   ├── CalculatorService.java    # Service Interface (SEI)
    │   └── CalculatorServiceImpl.java # Service Implementation
    ├── server/
    │   └── SoapServer.java           # Publishes the web service
    └── client/
        └── SoapClient.java           # Consumes the web service
```

## How to Run

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

### Step 1: Build the Project

```bash
# On Windows (PowerShell)
.\mvnw.ps1 clean compile

# On Windows (CMD) / Linux / Mac (if Maven is installed)
mvn clean compile
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
