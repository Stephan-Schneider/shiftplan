package shiftplan.web;

public enum ConfigBundle {

    INSTANCE;

    private String shiftPlanCopyXMLFile;
    private String shiftPlanCopySchemaDir;
    private String templateDir;
    private String smtpConfigPath;
    private String pdfOutDir;

    private void build(ConfigBuilder builder) {
        shiftPlanCopyXMLFile = builder.shiftPlanCopyXMLFile;
        shiftPlanCopySchemaDir = builder.shiftPlanCopySchemaDir;
        templateDir = builder.templateDir;
        smtpConfigPath = builder.smtpConfigPath;
        pdfOutDir = builder.pdfOutDir;
    }

    public static ConfigBundle getInstance() {
        return INSTANCE;
    }

    public String getShiftPlanCopyXMLFile() {
        return shiftPlanCopyXMLFile;
    }

    public String getShiftPlanCopySchemaDir() {
        return shiftPlanCopySchemaDir;
    }

    public String getTemplateDir() {
        return templateDir;
    }

    public String getSmtpConfigPath() {
        return smtpConfigPath;
    }

    public String getPdfOutDir() {
        return pdfOutDir;
    }


    public static class ConfigBuilder {

        private final String shiftPlanCopyXMLFile;
        private final String shiftPlanCopySchemaDir;
        private String templateDir;
        private String smtpConfigPath;
        private String pdfOutDir;

        public ConfigBuilder(String shiftPlanCopyXMLFile, String shiftPlanCopySchemaDir) {
            this.shiftPlanCopyXMLFile = shiftPlanCopyXMLFile;
            this.shiftPlanCopySchemaDir = shiftPlanCopySchemaDir;
        }

        public ConfigBuilder templateDir(String templateDir) {
            this.templateDir = templateDir;
            return this;
        }

        public ConfigBuilder smtpConfigPath(String smtpConfigPath) {
            this.smtpConfigPath = smtpConfigPath;
            return this;
        }

        public ConfigBuilder pdfOutDir(String pdfOutDir) {
            this.pdfOutDir = pdfOutDir;
            return this;
        }

        public void build() {
            ConfigBundle.INSTANCE.build(this);
        }
    }
}
