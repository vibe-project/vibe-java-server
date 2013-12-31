# Play 2
`wes-play2` module integrates wes application with the [Play framework](http://www.playframework.org/) 2 which is a high productivity Java and Scala web application framework. 

## Install
Add the following dependency to your `build.sbt`:
```scala
"io.github.flowersinthesand" % "wes-play2" % "${wes.version}"
```

To install a wes in Play, write a controller in Java.

```java
public class Application extends Controller {
    @BodyParser.Of(BodyParser.TolerantText.class)
    public static Result http() {
        final Request request = request();
        final Response response = response();
        return ok(new Chunks<String>(JavaResults.writeString(Codec.utf_8())) {
            @Override
            public void onReady(Chunks.Out<String> out) {
                // ServerHttpExchange
                new PlayServerHttpExchange(request, response, out);
            }
        });
    }

    public static WebSocket<String> ws() {
        final Request request = request();
        return new WebSocket<String>() {
            @Override
            public void onReady(WebSocket.In<String> in, WebSocket.Out<String> out) {
                // ServerWebSocket
                new PlayServerWebSocket(request, in, out);
            }
        };
    }
}
```

Add new routes for the controller to `routes`. In this case, uri can't be shared. 
```
GET     /portal                  controllers.Application.http()
GET     /portal/ws               controllers.Application.ws()
```

### Sharing uri
If you want to share uri for http and ws entry, write `Global.scala` and override `onRouteRequest`. It's not easy to do that in Java, if any.

Note that this is an internal API and not documented. Actually, these API have broken in minor release and even in patch release. The following code works in `2.2.0`.

```scala
import controllers.{Application => T}
 
object Global extends GlobalSettings {
  override def onRouteRequest(req: RequestHeader): Option[Handler] = {
    if (req.path == "/portal") {
      if (req.method == "GET" && req.headers.get("Upgrade").exists(_.equalsIgnoreCase("websocket"))) {
        Some(JavaWebSocket.ofString(T.ws))
      } else {
        Some(new JavaAction {
          val annotations = new JavaActionAnnotations(classOf[T], classOf[T].getMethod("http"))
          val parser = annotations.parser
          def invocation = Promise.pure(T.http)
        })
      }
    } else {
      super.onRouteRequest(req)
    }
  }
}
```

However, I'm not familiar with Scala. If you have a good idea to improve this, please let me know that.