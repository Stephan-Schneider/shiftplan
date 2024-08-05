module shiftplan {
    requires org.apache.logging.log4j;
    requires freemarker;
    requires freemarker.java8;
    requires org.jsoup;
    requires openhtmltopdf.pdfbox;
    requires org.jdom2;
    requires org.apache.commons.cli;
    requires commons.email;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires undertow.core;
    requires jdk.unsupported;
    requires xnio.api;
    requires java.prefs; // Erforderlich, um undertow zum Laufen zu bringen
    exports shiftplan.users to freemarker, com.fasterxml.jackson.databind;
    exports shiftplan.calendar to freemarker;
    opens shiftplan.calendar to com.fasterxml.jackson.databind;
    opens shiftplan.data.json to com.fasterxml.jackson.databind;
}