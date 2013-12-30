# Atmosphere 2
`wes-atmosphere2` module integrates wes application with the [Atmosphere 2](https://github.com/atmosphere/atmosphere/) which makes the application run on most servlet containers that support the Servlet Specification 2.3. That being said, this module requires Servlet 3.1 containers.

## Install
With Atmosphere, you can write a web application, a war project in Maven. Add the following dependency to your pom.xml or include it on your classpath.

```xml
<dependency>
    <groupId>io.github.flowersinthesand</groupId>
    <artifactId>wes-atmosphere2</artifactId>
    <version>${portal.version}</version>
</dependency>
```

To install a wes in Servlet environment, prepare `ServletContext`.
```java
@WebListener
public class Initializer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        // ServletContext
        ServletContext context = event.getServletContext());
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {}
}

```

Create a servlet of `AtmosphereServlet` and register it to `ServletContext` with some options.

```java
AtmosphereServlet servlet = context.createServlet(AtmosphereServlet.class);
ServletRegistration.Dynamic reg = context.addServlet("wes", servlet);
reg.setLoadOnStartup(0);
reg.setAsyncSupported(true);
// Path
reg.addMapping("/portal");
```

Define a `AtmosphereHandler` and add it to `AtmosphereFramework` mapping to the root path, `/`. In the handler, wrap a given resource with `AtmosphereServerWebSocket` if transport is WebSocket and request method is GET or with `AtmosphereServerHttpExchange` if transport is not WebSocket.

```java
AtmosphereFramework framework = servlet.framework();
framework.addAtmosphereHandler("/", new AtmosphereHandlerAdapter() {
    @Override
    public void onRequest(AtmosphereResource resource) throws IOException {
        if (resource.transport() == TRANSPORT.WEBSOCKET) {
            if (resource.getRequest().getMethod().equals("GET")) {
                // ServerWebSocket
                new AtmosphereServerWebSocket(resource);
            }
        } else {
            // ServerHttpExchange
            new AtmosphereServerHttpExchange(resource);
        }
    }
});
```

There are so many ways to bootstrap Atmosphere, however, if you don't know Atmosphere well, just follow the above way. 