package shiftplan.web;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.security.AuthenticatedUsers;

public class AuthorizedHandler implements HttpHandler {

    private final Logger logger = LogManager.getLogger(AuthorizedHandler.class);
    private final HttpHandler next;

    public AuthorizedHandler(final HttpHandler next) {
        this.next = next;
    }
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String userToken = exchange.getRequestHeaders().getFirst(ShiftplanServer.CUSTOM_AUTH_HEADER);
        logger.debug("User-Token: {}", userToken);
        if (!AuthenticatedUsers.getInstance().isAuthenticated(userToken)) {
            logger.info("Nicht authentifiziert");
            exchange.setStatusCode(401);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
            exchange.getResponseSender().send("Not authorized");
        } else {
            next.handleRequest(exchange);
        }
    }
}
