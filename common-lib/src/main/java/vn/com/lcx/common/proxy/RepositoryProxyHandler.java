package vn.com.lcx.common.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RepositoryProxyHandler<T> implements InvocationHandler {
    private final Logger log = LoggerFactory.getLogger("proxy");
    private final Object target;

    public RepositoryProxyHandler(Object target) {
        this.target = target;
    }

    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> interfaceType, Object target) {
        return (T) Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[]{interfaceType},
                new RepositoryProxyHandler<T>(target)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isDefault()) {
            return invokeDefaultMethod(proxy, method, args);
        }
        String methodName = method.getName();

        // Handle Object's built-in methods separately
        switch (methodName) {
            case "toString":
                return target.getClass().getName() + "@" + Integer.toHexString(target.hashCode());
            case "hashCode":
                return System.identityHashCode(proxy);
            case "equals":
                return proxy == args[0];
        }

        this.log.info("Executing method: {}", methodName);
        try {
            return method.invoke(target, args);
        }catch (Exception e) {
            throw new RuntimeException("Cannot invoke method: " + methodName);
        }
    }

    private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
        @SuppressWarnings("JavaReflectionMemberAccess") final Constructor<MethodHandles.Lookup> constructor =
                MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
        constructor.setAccessible(true);
        return constructor.newInstance(method.getDeclaringClass(), MethodHandles.Lookup.PRIVATE)
                .unreflectSpecial(method, method.getDeclaringClass())
                .bindTo(proxy)
                .invokeWithArguments(args);
    }

}
