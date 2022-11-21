module shiftplan {
    requires org.apache.logging.log4j;
    requires freemarker;
    requires freemarker.java8;
    exports shiftplan.users to freemarker;
    exports shiftplan.calendar to freemarker;
}