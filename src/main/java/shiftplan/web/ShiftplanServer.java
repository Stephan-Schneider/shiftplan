package shiftplan.web;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.attribute.ExchangeAttributes;
import io.undertow.predicate.Predicates;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.MimeMappings;
import shiftplan.security.AuthenticatedUsers;

import static io.undertow.Handlers.resource;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class ShiftplanServer {

    static final String BASE_PATH = ConfigBundle.INSTANCE.getWebResourcesBasePath();
    static final String CUSTOM_AUTH_HEADER = "X-Auth-ID";

    public static void createServer(String host, int port) {

        final IdentityManager identityManager = new PrefsIdentityManager();

        // Bei Aufruf der Modify-Handler müssen 5 Parameter angegeben werden, wenn ein REPLACE durchgeführt wird und
        // 6 Parameter bei einem SWAP
        Undertow server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(Handlers.path()
                        .addPrefixPath("/", resource(
                                new PathResourceManager(
                                        Path.of(BASE_PATH),
                                        100,
                                        false,
                                        true
                                ))
                                .setDirectoryListingEnabled(true)
                                .setMimeMappings(MimeMappings.DEFAULT)
                                .setCachable(
                                        Predicates.not(Predicates.contains(
                                                ExchangeAttributes.relativePath(), "Schichtplan")))
                                .setCacheTime(90)
                        )
                        .addPrefixPath("api/shiftplan", Handlers.routing()
                                .add(new HttpString("head"), "/stafflist", exchange -> {
                                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
                                    exchange.getResponseSender().send("");

                                })
                                .get("/authenticate", addSecurity(exchange -> {
                                    final SecurityContext context = exchange.getSecurityContext();
                                    String msg;
                                    if (context.isAuthenticated()) {
                                        String authId = exchange.getRequestHeaders().getFirst(CUSTOM_AUTH_HEADER);
                                        AuthenticatedUsers.getInstance().addAuthenticatedUser(authId);
                                        exchange.setStatusCode(200);
                                        msg = "User authentifiziert";
                                    } else {
                                        exchange.setStatusCode(401);
                                        msg = "Zugriff verweigert";
                                    }
                                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
                                    exchange.getResponseSender().send(msg);
                                }, identityManager))
                                .get("/create", new AuthorizedHandler(new CreateHandler()))
                                .post("/create/{clear}", new AuthorizedHandler(new CreateHandler()))
                                .put("/publish/{format}", new AuthorizedHandler(new PublishHandler()))
                                .get("/stafflist", new AuthorizedHandler(new StaffListHandler()))
                                .put("/modify/{mode}/{swapHo}/{emp1ID}/{cwIndex1}/{emp2ID}", new AuthorizedHandler(new ModifyHandler()))
                                .put("/modify/{mode}/{swapHo}/{emp1ID}/{cwIndex1}/{emp2ID}/{cwIndex2}", new AuthorizedHandler(new ModifyHandler()))
                                .setFallbackHandler(exchange -> {
                                    exchange.setStatusCode(404);
                                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                                    exchange.getResponseSender().send("404 - Page not found");
                                })
                                .setInvalidMethodHandler(exchange -> {
                                    HttpString method = exchange.getRequestMethod();
                                    if (!List.of(
                                            new HttpString("get"),
                                            new HttpString("post"),
                                            new HttpString("put"),
                                            new HttpString("head")
                                            ).contains(method)) {
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

    private static HttpHandler addSecurity(final HttpHandler toWrap, final IdentityManager identityManager) {
        HttpHandler handler = toWrap;
        handler = new AuthenticationCallHandler(handler);
        handler = new AuthenticationConstraintHandler(handler);
        final List<AuthenticationMechanism> mechanisms = Collections.singletonList(
                new BasicAuthenticationMechanism("Shiftplan App"));
        handler = new AuthenticationMechanismsHandler(handler, mechanisms);
        handler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, identityManager, handler);
        return handler;
    }
}
