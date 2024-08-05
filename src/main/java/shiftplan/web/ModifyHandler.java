package shiftplan.web;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shiftplan.ShiftPlanRunner;
import shiftplan.calendar.SwapParams;

import java.util.Map;

public class ModifyHandler implements HttpHandler {

    private final Logger logger = LogManager.getLogger(ModifyHandler.class);
    private final ConfigBundle config = ConfigBundle.INSTANCE;

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String errorMessage;
        StringBuilder builder = new StringBuilder();

        PathTemplateMatch pathMatch = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        Map<String, String> params = pathMatch.getParameters();

        String mode = params.getOrDefault("mode", "SWAP");
        builder.append(mode).append(",");
        builder.append(params.getOrDefault("swapHo", "false")).append(",");
        builder.append(params.get("emp1ID")).append(",");
        builder.append(params.get("cwIndex1")).append(",");
        builder.append(params.get("emp2ID")).append(",");
        if (mode.equalsIgnoreCase("SWAP")) {
            builder.append(params.get("cwIndex2"));
        }

        String paramString = builder.toString();
        logger.info("Der Schichtplan wird mit folgenden Parametern geändert: {}", paramString);

        try {
            ShiftPlanRunner runner = new ShiftPlanRunner();
            SwapParams swapParams = runner.getOperationalParams(paramString);
            runner.modifyShiftPlan(config.getShiftPlanCopyXMLFile(), config.getShiftPlanCopySchemaDir(), swapParams);

            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
            exchange.getResponseSender().send("Schichtplan geändert");
        } catch (Exception e) {
            logger.fatal("Problem bei Änderung des Schichtplans", e);
            errorMessage = e.getMessage();
            exchange.setStatusCode(500);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
            exchange.getResponseSender().send(errorMessage);
        }
    }
}
