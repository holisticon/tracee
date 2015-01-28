# TracEE 0.8.0

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/tracee/tracee?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.tracee/tracee-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.tracee/tracee-parent)
[![Tasks in Ready](https://badge.waffle.io/tracee/tracee.png?label=ready&title=Ready)](https://waffle.io/tracee/tracee)
[![Tasks in Progress](https://badge.waffle.io/tracee/tracee.png?label=in+progress&title=In+Progress)](https://waffle.io/tracee/tracee)
[![Build Status](https://api.travis-ci.org/tracee/tracee.svg)](https://travis-ci.org/tracee/tracee)
[![Coverage Status](https://img.shields.io/coveralls/tracee/tracee.svg)](https://coveralls.io/r/tracee/tracee)
[![Coverity Scan](https://scan.coverity.com/projects/3882/badge.svg)](https://scan.coverity.com/projects/3882)

TL;DR, watch our [5 minutes slide deck](http://www.tracee.io/docs/slides/#/)

> __TracEE makes it easy to track the invocation context throughout your distributed JavaEE-Application logs. It does not force
> you to manually pass around collaboration-ids but instead uses side-channels on the underlying protocols and therefore
> encapsulates the aspect of invocation context propagation.__

You may already aggregate all your application logs at a single place (using graylog, logstash+kibana or others) but it is still
complicated to gather relevant log entries that belong to a certain interaction with your system.

*TracEE* is an integration framework that eases this kind of interaction monitoring of JavaEE applications by passing contextual information
through your whole system and makes them visible in your logs. Therefore it contains adapters or interceptors for the most popular JavaEE technologies:

* servlet 2.5+
* jax-ws
* jax-rs2
* jms

Also supported are these common and widespread frameworks:

* Spring MVC
* Spring Web (RestClients)
* Apache HttpClient 3 / 4
* Apache CXF

This project is still in an experimental stage and the api may change during further development.

# Getting started

## The Invocation Context

So what is this all about?

An example:

- A user opens your JavaEE enterprisey hotel booking page in a browser
- Your servlet container renders a calendar and room selection
- The user selects a date and a tasty breakfast and clicks on _book_
- Your servlet starts to chat with your full-blown EE-Server
- _Awesome stuff happens_, EJBs, SOAP- and REST-Services are called, databases are read and written
- Finally the user sees a confirmation page and receives a booking confirmation via email

This is the happy path. But what if something goes wrong? Lets name it: Exception stack trace in the log files.
The stack trace often shows a very narrow scope of what went wrong, namely the current thread.

An __invocation context__ is a temporal or logical boundary in which a system invocation happens.
Since each system invocation may cause further system interactions, every subsequent system invocation logically belongs to the
same context as the invoking interaction.

An example for an invocation context in a servlet based application would be a HTTP-Request. Every system interaction
that is directly or indirectly caused by a browser asking a servlet-container lies within the context of its HTTP-Request.

Another example would be the context of a servlet session in which every request within this session belongs to
the session invocation context.

It can be a great benefit to make those invocation contexts visible in the log files. So if a failure happens, you could
lookup the invocation context in which it occurred and see pretty clearly what happened _in this context_ before the
server gave up with an error. And it does not end here.

Even without errors, the invocation context information empowers you to measure the reaction time
of your application at the level of every single service call.

So how do you implement this in an JavaEE-Application? An obvious way would be to pass an invocation context identifier
around as a parameter of every of your business interfaces (EBJ, SOAP, REST, whatsoever) and write it explicitly into each
log statement. It should also be obvious that this is a dumb idea because it pollutes all of our business interfaces with
unnecessary artificial parameters just for the benefit of making the invocation context explicit. But we can do better!

## The Propagated Invocation Context (PIC)

The [Mapped Diagnostic Context (MDC)](http://logback.qos.ch/manual/mdc.html) is a logging concept that allows printing of contextual information in log messages
without explicitly passing them them to each log statement. A MDC is bound to its executing thread (in fact they are backed by thread locals).

Invocations of JavaEE components are seldom fully processed within a single thread - they might not even be fully processed
on the same JVM. So each time an invocation escapes its original executing thread, the MDC is lost in the nested processing.
We may call these context boundaries MDC gaps. There are different kinds of those gaps:

* Remote calls
    * Remote EJB calls
    * Invoking and accepting arbitrary HTTP requests (Servlet, HttpClient)
    * Invoking and accepting remote web-service calls (JAX-WS)
    * Invoking and accepting remote rest-services calls (JAX-RS, RestClients / Spring MVC)
* Asynchronous dispatch
    * Async processing of a servlet request (Servlet3 Async)
    * Async EJB calls
    * JMS messaging

TracEE acts as a gap closer for different types of MDC gaps and boosts the concept of the MDC by enabling you to carry your contextual information through
your whole application and beyond. You may easily configure TracEE to map arbitrary third-party correlation and transaction ids to
your logging context without polluting your business logic.

## On the wire

So how does a Propagated Invocation Context look like on the wire? This naturally depends on the transport.
The most prominent transports are native java serialization formats, HTTP and SOAP.

Using Java serialization the Propagated Invocation Context is simply encoded as a Java map of strings.

On HTTP-Transports, like JAX-RS, Servlets, JSPs, or whatever, the invocation context is encoded as a custom HTTP-Header 
__TPIC__. Key and value are URL-Encoded and concatenated with `=` and `,`.
 
```
GET / HTTP/1.1
TPIC: in+Request=yes
User-Agent: Jakarta Commons-HttpClient/3.1
Host: localhost:2000
```
 
In the SOAP-world the invocation context is, regardless of the underlying transport mechanism, encoded as a special header
in the SOAP-Request-Envelope and SOAP-Response-Envelope.
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
	<soap:Header>
		<tpic xmlns="http://tracee.io/tpic/1.0">
			<entry key="traceeRequestId">ABCDEFG</entry>
		</tpic>
	</soap:Header>
...
</soap:Envelope>
```

# Integrating TracEE into your application

TracEE is highly modular. What modules actually you need for integration depends on your application, its underlying frameworks and containers.
The following table describes all available TracEE-modules and their usage scenarios.

| Module                                              | Usage |
|----------------------------------------------------:|:-----:|
| __core modules__                                    |       |
| [tracee-api](api/)                                  | API to interact with the TracEE context from within your business code. Use it to write contextual information from your application into the TracEE context.
| [tracee-core](core/)                                | Common utility classes, configuration system and transport serialization mechanisms. *You won't need this module as a direct dependency.*
| __connector / binding modules__                     | *These dependencies are needed due compile time* |
| [tracee-httpcomponents](binding/httpcomponents/)    | Adapter for `org.apache.httpcomponents:httpclient`-library (also known as HttpClient 4.x). Use it to make your JAX-RS or raw http clients propagate and receive invocation contexts.
| [tracee-httpclient](binding/httpclient/)            | Adapter for `commons-httpclient`-library (also known as HttpClient 3.x). Use it to make your JAX-RS or raw http clients propagate and receive invocation contexts.
| [tracee-jaxrs2](binding/jaxrs2)                     | Interceptors for JAX-RS2. Use it to traceefy your JAX-RS2 endpoints and clients.
| [tracee-jaxws](binding/jaxws)                       | HandlerChains for JAX-WS endpoints and clients.
| [tracee-jms](binding/jms)                           | EJB-Interceptors and MessageProducers that allow you to pass around your TracEE context with JMS.
| [tracee-servlet](binding/servlet)        		      | Listeners and filters for the servlet spec. Use it to traceefy JAX-RS, Vaadin, JSP or any other servlet based web application.
| [tracee-springmvc](binding/springmvc)               | Provides a HandlerInterceptor for Spring MVC. Use it to traceefy Spring MVC or Spring WebFlow applications.
| [tracee-springhttpclient](binding/springhttpclient) | ClientHttpRequestInterceptor for Springs `RestTemplate`. Simply add an `TraceeClientHttpRequestInterceptor` to traceefy your requests.
| [tracee-cxf](binding/cxf)                           | To transfer context informations with CXF add the `TraceeCxfFeature` to your Client oder Server.
| __backends__                                        | *These dependencies are needed due runtime.*         |
| [tracee-slf4j](backend/slf4j)                       | Backend implementation for containers using slf4j. You may use this for Logback-Backend or on top of a java util logging containers like tomcat6 together with slf4j-jcl.
| [tracee-log4j](backend/log4j)                       | Backend implementation for containers using log4j for logging.
| [tracee-log4j2](backend/log4j2)                     | Backend implementation for containers using log4j2 for logging.
| [tracee-jboss-logging](backend/jboss-logging)       | Backend implementation for containers using jboss-logging like used in JBoss EAP5/AS6.
| [threadlocal-store](backend/threadlocal-store)      | Backend implementation for containers that use no common logging framework. Use it in scenarios where you have a component that does not use a supported logging framework but that you still want to to propagate the invocation context.

Look into our [Bindings](binding/)-Page to get a more detailed binding overview.

All TracEE modules are (hopefully) OSGI compliant.

## Maven artifacts

Tracee is released to maven central via Sonatype OSS Repository Hosting. Just add a maven/gradle/sbt dependency as usual. If you are a crazy one, you could use the very latest SNAPSHOT as well by adding the sonatype snapshot repository:

```xml
<repositories>
    <repository>
        <id>sonatype-nexus-snapshots</id>
        <name>Sonatype Nexus Snapshots</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
    </repository>
</repositories>
```

# More

## Shipped context information
TracEE creates the following context identfiers on the fly if not configured otherwise:

    * it generates a pseudo-unique request id (configurable length)
    * it generates a session hash based on the servlet session id. Since the servlet session id is a secure item that should not 
    be passed around unnecessarily, we use a hash of it.

## Performance considerations

TracEE is designed with performance in mind. It does not introduce global synchronization and cleans up the MDC after
each invocation lifecycle. A real benchmark is pending...

The automatically generated context ids (like request- and session-identifiers) are configurable in length and allow you
to choose a tradeoff between the chance of _uniqueness_ in time and data overhead depending on your load scenario.

## Security considerations

Since you may pass sensitive user information within your TracEE-Context, it is important to cancel the propagation at
trust boundaries (like HTTP-Responses to users or third-party Web-Services). TracEE offers filter configuration mechanisms
that allow you to selectively decide at which point in your application you want to pass around what contextual identifiers.

## Classloader considerations

You can bundle TracEE with your application or install it as global library to your container.

You just need to understand, that the invocation context identifiers are stored in the MDC of your logging framework (and therefore also share its lifecycle). 
So when you host multiple applications within a container with classloader isolation, an inter-application invocation context 
(like remote EJBs) can only be propagated when they share the same MDC (MDCs in the end just little more than a thread-local per class-loader).
Therefore it is highly recommended to use your containers logging framework because it resides in the container classloader and can be
accessed by all your applications.

# Contributing to TracEE

We welcome any kind of suggestions and pull requests. Please notice that TracEE is an integration framework and we will not support
application specific features. We will rather try to enhance our api and empower you to tailor TracEE to your needs.

## Building and developing TracEE

TracEE is built using Maven (at least version 3.1.0).
A simple import of the pom in your IDE should get you up and running. To build TracEE on the commandline, just run `mvn clean install`

## Requirements

The likelihood of a pull request being used rises with the following properties:

- You have used a feature branch.
- You have included a test that demonstrates the functionality added or fixed.
- You adhered to the [code conventions](http://www.oracle.com/technetwork/java/javase/documentation/codeconvtoc-136057.html).

## Contributions

- (2013) Daniel Wegener (Holisticon AG)
- (2013) Tobias Gindler (Holisticon AG)
- (2014) Sven Bunge (Holisticon AG)

## Sponsoring

This project is sponsored and supported by [holisticon AG](http://www.holisticon.de/)

# License

This project is released under the revised [BSD License](LICENSE).
