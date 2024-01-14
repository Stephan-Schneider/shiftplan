package shiftplan.web;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.util.List;

public class ShiftplanServer {

    public static void createServer(int port) {

        // Bei Aufruf der PUT-Routings müssen alle Parameter angegeben werden, einschließlich des smtpPwd-Parameters.
        // Falls kein Emailversand durchgeführt werden soll, den Wert 'false' angegeben
        Undertow server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setHandler(Handlers.path()
                        .addPrefixPath("api/shiftplan", Handlers.routing()
                                .add(new HttpString("head"), "/stafflist", exchange -> {
                                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
                                    exchange.getResponseSender().send("");

                                })
                                .get("/stafflist", new StaffListHandler())
                                .put("/modify/{mode}/{swapHo}/{emp1ID}/{cwIndex1}/{emp2ID}/{smtpPwd}", new ModifyHandler())
                                .put("/modify/{mode}/{swapHo}/{emp1ID}/{cwIndex1}/{emp2ID}/{cwIndex2}/{smtpPwd}", new ModifyHandler())
                                .setFallbackHandler(exchange -> {
                                    exchange.setStatusCode(404);
                                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                                    exchange.getResponseSender().send("404 - Page not found");
                                })
                                .setInvalidMethodHandler(exchange -> {
                                    HttpString method = exchange.getRequestMethod();
                                    if (!List.of(new HttpString("get"), new HttpString("put"), new HttpString("head")).contains(method)) {
                                        exchange.setStatusCode(405);
                                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                                        exchange.getResponseSender().send("405 - Invalid Method");
                                    }
                                })
                        )

                )
                .build();
        server.start();
    }
}
