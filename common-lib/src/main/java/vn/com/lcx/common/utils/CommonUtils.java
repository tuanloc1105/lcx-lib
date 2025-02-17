package vn.com.lcx.common.utils;

import lombok.val;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.stream.Collectors;

public final class CommonUtils {

    private CommonUtils() {
    }

    public static void gc() {
        System.gc();
    }

    public static String generateRandom12DigitNumber() {
        Random random = new Random();

        // Generate the first 6 random digits
        int firstSixDigits = 100000 + random.nextInt(900000); // Ensures a 6-digit number

        // Get the current Unix timestamp in milliseconds and extract the last 6 digits
        long timestamp = System.currentTimeMillis();
        int lastSixDigits = (int) (timestamp % 1000000); // Extracts the last 6 digits

        // Combine the two parts and return as a 12-digit string
        return String.format("%06d%06d", firstSixDigits, lastSixDigits);
    }

    public static void bannerLogging(final String bannerResourcePath) {
        ClassLoader classLoader = CommonUtils.class.getClassLoader();
        try (
                InputStream input = classLoader.getResourceAsStream(bannerResourcePath)
        ) {
            if (input != null) {
                val text = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining(System.lineSeparator()));
                LoggerFactory.getLogger("BANNER").info("{}{}", System.lineSeparator(), text);
            }
        } catch (IOException ex) {
            LogUtils.writeLog(ex.getMessage(), ex);
        }
    }
}
