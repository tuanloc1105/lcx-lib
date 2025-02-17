package vn.com.lcx.common.utils;

import org.apache.commons.lang3.StringUtils;
import vn.com.lcx.common.constant.CommonConstant;

public final class PdfGenerator {

    private static final String CONVERT_HTML_TO_PDF_SCRIPT_NAME = FileUtils.pathJoining(
            CommonConstant.ROOT_DIRECTORY_PROJECT_PATH,
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
                        CommonConstant.CALLING_PYTHON_CODE_COMMAND_LINE,
                        CONVERT_HTML_TO_PDF_SCRIPT_NAME,
                        String.format(
                                "\"%s\" \"%s\"",
                                htmlSource,
                                outputPath
                        )
                ),
                CommonConstant.ROOT_DIRECTORY_PROJECT_PATH
        );
    }
}
