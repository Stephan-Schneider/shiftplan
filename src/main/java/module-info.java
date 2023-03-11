module shiftplan {
    requires org.apache.logging.log4j;
    requires freemarker;
    requires freemarker.java8;
    requires org.jsoup;
    requires openhtmltopdf.pdfbox;
    requires org.jdom2;
    requires commons.cli;
    exports shiftplan.users to freemarker;
    exports shiftplan.calendar to freemarker;
}