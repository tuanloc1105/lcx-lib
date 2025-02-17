package vn.com.lcx.common.utils;

import org.apache.commons.lang3.StringUtils;
import vn.com.lcx.common.constant.Constant;

public final class PdfGenerator {

    private static final String CONVERT_HTML_TO_PDF_SCRIPT_NAME = FileUtils.pathJoining(
            Constant.ROOT_DIRECTORY_PROJECT_PATH,
            "additional-source",
            "python",
            "convert_html_to_pdf"
    );

    private PdfGenerator() {
    }

    public static void generatePdfFromHtmlString(final String htmlSource, final String outputPath) {
        if (StringUtils.isBlank(htmlSource) || StringUtils.isBlank(outputPath)) {
            throw new NullPointerException("htmlSource and outputPath cannot be empty");
        }
        ShellCommandRunningUtils.runWithProcessBuilder(
                String.format(
                        Constant.CALLING_PYTHON_CODE_COMMAND_LINE,
                        CONVERT_HTML_TO_PDF_SCRIPT_NAME,
                        String.format(
                                "\"%s\" \"%s\"",
                                htmlSource,
                                outputPath
                        )
                ),
                Constant.ROOT_DIRECTORY_PROJECT_PATH
        );
    }
}
