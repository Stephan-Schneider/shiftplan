package shiftplan.web;

public enum ConfigBundle {

    INSTANCE;

    private String jsonFile;
    private String webResourcesBasePath;
    private String shiftPlanCopyXMLFile;
    private String shiftPlanCopySchemaDir;
    private String templateDir;
    private String smtpConfigPath;
    private String generatedDataDir;

    private void build(ConfigBuilder builder) {
        jsonFile = builder.jsonFile;
        webResourcesBasePath = builder.webResourcesBasePath;
        shiftPlanCopyXMLFile = builder.shiftPlanCopyXMLFile;
        shiftPlanCopySchemaDir = builder.shiftPlanCopySchemaDir;
        templateDir = builder.templateDir;
        smtpConfigPath = builder.smtpConfigPath;
        generatedDataDir = builder.generatedDataDir;
    }

    public static ConfigBundle getInstance() {
        return INSTANCE;
    }

    public String getJsonFile() {
        return jsonFile;
    }

    public String getWebResourcesBasePath() {
        return webResourcesBasePath;
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

    public String getGeneratedDataDir() {
        return generatedDataDir;
    }


    public static class ConfigBuilder {

        private String jsonFile;
        private String webResourcesBasePath;
        private String shiftPlanCopyXMLFile;
        private String shiftPlanCopySchemaDir;
        private String templateDir;
        private String smtpConfigPath;
        private String generatedDataDir;

        public ConfigBuilder(String jsonFile) {
            this.jsonFile = jsonFile;
        }

        public ConfigBuilder(String shiftPlanCopyXMLFile, String shiftPlanCopySchemaDir) {
            this.shiftPlanCopyXMLFile = shiftPlanCopyXMLFile;
            this.shiftPlanCopySchemaDir = shiftPlanCopySchemaDir;
        }

        public ConfigBuilder(String jsonFile, String shiftPlanCopyXMLFile, String shiftPlanCopySchemaDir) {
            this.jsonFile = jsonFile;
            this.shiftPlanCopyXMLFile = shiftPlanCopyXMLFile;
            this.shiftPlanCopySchemaDir = shiftPlanCopySchemaDir;
        }

         public ConfigBuilder jsonFile(String jsonFile) {
            this.jsonFile = jsonFile;
            return this;
         }

         public ConfigBuilder webResourcesBasePath(String webResourcesBasePath) {
            this.webResourcesBasePath = webResourcesBasePath;
            return this;
         }

        public ConfigBuilder shiftPlanCopyXMLFile(String shiftPlanCopyXMLFile) {
            this.shiftPlanCopyXMLFile = shiftPlanCopyXMLFile;
            return this;
        }

        public ConfigBuilder shiftPlanCopySchemaDir(String shiftPlanCopySchemaDir) {
            this.shiftPlanCopySchemaDir = shiftPlanCopySchemaDir;
            return this;
        }

        public ConfigBuilder templateDir(String templateDir) {
            this.templateDir = templateDir;
            return this;
        }

        public ConfigBuilder smtpConfigPath(String smtpConfigPath) {
            this.smtpConfigPath = smtpConfigPath;
            return this;
        }

        public ConfigBuilder generatedDataDir(String generatedDataDir) {
            this.generatedDataDir = generatedDataDir;
            return this;
        }

        public void build() {
            ConfigBundle.INSTANCE.build(this);
        }
    }
}
