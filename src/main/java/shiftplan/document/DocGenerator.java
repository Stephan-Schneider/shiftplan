package shiftplan.document;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public class DocGenerator {

    public Document getRawHTML(String in) {
        if (in == null || in.isBlank()) {
            throw new IllegalArgumentException("Leerer Input-String !");
        }

        Document document = Jsoup.parse(in, "");
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        return document;
    }

    public void createPDF(Document document, Path outputFile) throws IOException {
        try (OutputStream os = new FileOutputStream(outputFile.toFile())) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withUri(String.valueOf(outputFile.toUri()));
            builder.toStream(os);
            builder.withW3cDocument(new W3CDom().fromJsoup(document), "/");
            builder.run();
        }
    }
}
