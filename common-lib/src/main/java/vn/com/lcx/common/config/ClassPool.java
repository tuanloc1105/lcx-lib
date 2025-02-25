package vn.com.lcx.common.config;

import lombok.val;
import lombok.var;
import org.slf4j.LoggerFactory;
import vn.com.lcx.common.annotation.Component;
import vn.com.lcx.common.annotation.Instance;
import vn.com.lcx.common.annotation.InstanceClass;
import vn.com.lcx.common.annotation.PostConstruct;
import vn.com.lcx.common.annotation.Repository;
import vn.com.lcx.common.annotation.Verticle;
import vn.com.lcx.common.annotation.mapper.Mapper;
import vn.com.lcx.common.annotation.mapper.MapperClass;
import vn.com.lcx.common.constant.CommonConstant;
import vn.com.lcx.common.database.DatabaseExecutor;
import vn.com.lcx.common.database.DatabaseExecutorImpl;
import vn.com.lcx.common.database.repository.LCXRepository;
import vn.com.lcx.common.proxy.RepositoryProxyHandler;
import vn.com.lcx.common.scanner.PackageScanner;
import vn.com.lcx.common.utils.PropertiesUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClassPool {

    public static final ConcurrentHashMap<String, Object> CLASS_POOL = new ConcurrentHashMap<>();

    public static void init(final List<String> packagesToScan, final List<Class<?>> verticleClass) {
        ClassLoader classLoader = ClassPool.class.getClassLoader();
        val configFile = System.getProperty("application_config.file");
        if (configFile != null) {
            CommonConstant.applicationConfig = PropertiesUtils.getProperties(configFile);
        } else {
            CommonConstant.applicationConfig = PropertiesUtils.getProperties(classLoader, "application.yaml");
        }
        try {
            final List<Class<?>> listOfClassInPackage = new ArrayList<>();
            packagesToScan.forEach(packageName -> {
                try {
                    listOfClassInPackage.addAll(PackageScanner.findClasses(packageName));
                } catch (Exception ignore) {
                }
            });

            val postHandleComponent = new ArrayList<Class<?>>();
            val handledPostHandleComponent = new ArrayList<Class<?>>();

            for (Class<?> aClass : listOfClassInPackage) {

                if (aClass.getAnnotation(Verticle.class) != null) {
                    verticleClass.add(aClass);
                    continue;
                }

                val repositoryAnnotation = aClass.getAnnotation(Repository.class);
                if (repositoryAnnotation != null) {
                    val implementClassName = aClass.getName() + "Implement";
                    val repository = (LCXRepository<?>) Class.forName(implementClassName).getDeclaredConstructor(DatabaseExecutor.class).newInstance(DatabaseExecutorImpl.getInstance());
                    val repositoryProxy = RepositoryProxyHandler.createProxy(aClass, repository);
                    CLASS_POOL.put(aClass.getName(), repositoryProxy);
                    CLASS_POOL.put(implementClassName, repository);
                    continue;
                }
                val mapperClassAnnotation = aClass.getAnnotation(MapperClass.class);
                if (mapperClassAnnotation != null && aClass.isInterface()) {
                    CLASS_POOL.put(aClass.getName(), Mapper.getInstance(aClass));
                }
                val instanceClassAnnotation = aClass.getAnnotation(InstanceClass.class);
                if (instanceClassAnnotation != null) {
                    val methodsOfInstance = Arrays.stream(aClass.getDeclaredMethods()).filter(m -> m.getAnnotation(Instance.class) != null).collect(Collectors.toList());
                    if (!methodsOfInstance.isEmpty()) {
                        val instanceClass = aClass.getDeclaredConstructor().newInstance();
                        for (Method method : methodsOfInstance) {
                            val instanceMethodResult = method.invoke(instanceClass);
                            CLASS_POOL.put(instanceMethodResult.getClass().getName(), instanceMethodResult);
                            CLASS_POOL.put(method.getName(), instanceMethodResult);
                        }
                    }
                    continue;
                }
                val componentAnnotation = aClass.getAnnotation(Component.class);
                if (componentAnnotation != null) {
                    val fieldsOfComponent = Arrays.stream(aClass.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers())).collect(Collectors.toList());
                    if (fieldsOfComponent.isEmpty()) {
                        val component = aClass.getDeclaredConstructor().newInstance();
                        CLASS_POOL.put(aClass.getName(), component);
                    } else {
                        postHandleComponent.add(aClass);
                    }
                }
            }
            var count = 0;
            val limit = 10;
            while (postHandleComponent.size() != handledPostHandleComponent.size()) {
                if (limit == count) {
                    break;
                }
                for (Class<?> aClass : postHandleComponent) {
                    if (handledPostHandleComponent.stream().anyMatch(c -> c.isAssignableFrom(aClass))) {
                        continue;
                    }
                    val fields = new ArrayList<Field>();
                    if (aClass.getSuperclass() != null) {
                        List<Field> superClassField = Arrays.asList(aClass.getSuperclass().getDeclaredFields());
                        fields.addAll(superClassField);
                    }
                    fields.addAll(Arrays.asList(aClass.getDeclaredFields()));
                    val fieldsOfComponent = fields.stream().filter(f -> !Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())).collect(Collectors.toList());

                    // final Class<?>[] asd = getConstructorWithMostParameters(aClass).getParameterTypes();
                    final Class<?>[] fieldArr = fieldsOfComponent.stream().map(Field::getType).toArray(Class[]::new);

                    final Object[] args = fieldsOfComponent.stream().map(
                            f -> {
                                Object o1 = CLASS_POOL.get(f.getName());
                                if (o1 != null) {
                                    return o1;
                                }
                                return CLASS_POOL.get(f.getType().getName());
                            }
                    ).toArray(Object[]::new);
                    if (Arrays.stream(args).noneMatch(Objects::isNull)) {
                        val instance = aClass.getDeclaredConstructor(fieldArr).newInstance(
                                fieldsOfComponent.stream()
                                        .map(
                                                f -> {
                                                    Object o1 = CLASS_POOL.get(f.getName());
                                                    if (o1 != null) {
                                                        return o1;
                                                    }
                                                    return CLASS_POOL.get(f.getType().getName());
                                                }
                                        ).toArray(Object[]::new)
                        );
                        ClassPool.CLASS_POOL.put(aClass.getName(), instance);

                        val superClass = aClass.getSuperclass();

                        if (superClass != null && superClass != Object.class) {
                            ClassPool.CLASS_POOL.put(superClass.getName(), instance);
                        }

                        val iFace = aClass.getInterfaces();

                        for (Class<?> iFaceClass : iFace) {
                            ClassPool.CLASS_POOL.put(iFaceClass.getName(), instance);
                        }

                        val postConstructMethods = Arrays.stream(aClass.getDeclaredMethods()).filter(m -> m.getAnnotation(PostConstruct.class) != null).collect(Collectors.toList());
                        val hasMoreThanOnePostConstructMethod = postConstructMethods.size() > 1;
                        if (hasMoreThanOnePostConstructMethod) {
                            throw new RuntimeException(
                                    String.format(
                                            "Cannot create instance of %s because there are more than one PostConstruct method",
                                            aClass.getName()
                                    )
                            );
                        }
                        if (!postConstructMethods.isEmpty()) {
                            val postConstructMethod = postConstructMethods.get(0);

                            if (postConstructMethod.getReturnType().equals(void.class)) {

                                if (postConstructMethod.getParameterCount() > 0) {
                                    throw new RuntimeException(
                                            String.format(
                                                    "Cannot create instance of %s. Does not accept parameters",
                                                    aClass.getName()
                                            )
                                    );
                                }

                                postConstructMethod.invoke(instance);
                            } else {
                                throw new RuntimeException(
                                        String.format(
                                                "Post construct of %s must be a void method",
                                                aClass.getName()
                                        )
                                );
                            }
                        }
                        handledPostHandleComponent.add(aClass);
                    }
                }
                ++count;
            }
            if (limit == count && postHandleComponent.size() != handledPostHandleComponent.size()) {
                throw new RuntimeException(
                        String.format(
                                "Cannot create instance of classes %s",
                                postHandleComponent.stream().map(Class::getName).collect(Collectors.joining(", ", "[", "]"))
                        )
                );
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(ClassPool.class).error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static Constructor<?> getConstructorWithMostParameters(Class<?> clazz) {
        Constructor<?> maxParamConstructor = null;
        int maxParams = -1;

        for (Constructor<?> constructor : clazz.getConstructors()) {
            int paramCount = constructor.getParameterCount();
            if (paramCount > maxParams) {
                maxParams = paramCount;
                maxParamConstructor = constructor;
            }
        }
        return maxParamConstructor;
    }

    public static <T> T getInstance(String name, Class<T> clazz) {
        return clazz.cast(CLASS_POOL.get(name));
    }

}
