package vn.com.lcx.vertx.base.context;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthContext {

    private static final ThreadLocal<Object> connetionThreadLocal = new ThreadLocal<>();

    public static void set(Object connection) {
        connetionThreadLocal.set(connection);
    }

    public static Object get() {
        return connetionThreadLocal.get();
    }

    public static <T> T get(Class<T> clz) {
        return clz.cast(connetionThreadLocal.get());
    }

    public static void clear() {
        connetionThreadLocal.remove();
    }

}
