package shiftplan.web;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import shiftplan.calendar.ShiftPlanCopy;

import shiftplan.calendar.ShiftPlanSwapException;
import shiftplan.users.StaffList;
import shiftplan.data.ShiftPlanSerializer;

import java.util.Map;
import java.io.IOException;

public class StaffListHandler implements HttpHandler {

    private static final Logger logger = LogManager.getLogger(StaffListHandler.class);
    private final ConfigBundle config = ConfigBundle.INSTANCE;

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        try {
            String content = getStaffList();
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; charset=utf-8");
            exchange.getResponseSender().send(content);
        } catch (Exception e) {
            exchange.setStatusCode(500);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
            exchange.getResponseSender().send("500 - Internal Server Error: " + e.getMessage());
        }
    }

    public String getStaffList() {
        try {
            ShiftPlanSerializer serializer = new ShiftPlanSerializer(
                    config.getShiftPlanCopyXMLFile(), config.getShiftPlanCopySchemaDir());
            ShiftPlanCopy copy = serializer.deserializeShiftplan();

            StaffList staffList = new StaffList(copy);
            Map<String, StaffList.StaffData> employeeMap = staffList.createStaffList();
            return staffList.serializeStaffList(employeeMap);
        } catch (NullPointerException | IllegalArgumentException e) {
            logger.fatal("Ungültiger Pfadangaben zur Schichtplankopie oder Schemadatei oder copy = null", e);
            throw new ShiftPlanSwapException(e.getMessage());
        } catch (IOException | JDOMException e) {
            logger.fatal("Problem beim Auslesen der Schichtplankopie", e);
            throw new ShiftPlanSwapException(e.getMessage());
        } catch (ShiftPlanSwapException e) {
            logger.fatal("Ungültiger, veralteter Schichtplan");
            throw e;
        }
    }
}
