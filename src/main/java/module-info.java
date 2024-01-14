module shiftplan {
    requires org.apache.logging.log4j;
    requires freemarker;
    requires freemarker.java8;
    requires org.jsoup;
    requires openhtmltopdf.pdfbox;
    requires org.jdom2;
    requires commons.cli;
    requires commons.email;
    requires com.fasterxml.jackson.databind;
    requires undertow.core;
    requires jdk.unsupported; // Erforderlich, um undertow zum Laufen zu bringen
    exports shiftplan.users to freemarker;
    exports shiftplan.calendar to freemarker;
    opens shiftplan.calendar to com.fasterxml.jackson.databind;
}