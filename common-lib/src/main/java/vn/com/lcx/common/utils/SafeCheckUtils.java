package vn.com.lcx.common.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import vn.com.lcx.common.constant.Constant;

import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SafeCheckUtils {

    public static String stringSafeCheck(Supplier<String> supplier) {
        try {
            val value = supplier.get();
            return MyStringUtils.isBlank(value) ? Constant.EMPTY_STRING : value;
        } catch (Exception e) {
            return Constant.EMPTY_STRING;
        }
    }

}
