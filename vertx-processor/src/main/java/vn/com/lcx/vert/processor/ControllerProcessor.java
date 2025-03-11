package vn.com.lcx.vert.processor;

import org.apache.commons.lang3.StringUtils;
import vn.com.lcx.common.constant.CommonConstant;
import vn.com.lcx.common.utils.ExceptionUtils;
import vn.com.lcx.vertx.base.annotation.process.APIKey;
import vn.com.lcx.vertx.base.annotation.process.Auth;
import vn.com.lcx.vertx.base.annotation.process.Controller;
import vn.com.lcx.vertx.base.annotation.process.Delete;
import vn.com.lcx.vertx.base.annotation.process.Get;
import vn.com.lcx.vertx.base.annotation.process.Post;
import vn.com.lcx.vertx.base.annotation.process.Put;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("vn.com.lcx.vertx.base.annotation.process.Controller")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ControllerProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Map<TypeElement, List<ExecutableElement>> classMap = new HashMap<>();

        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(vn.com.lcx.vertx.base.annotation.process.Controller.class)) {
            if (annotatedElement instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) annotatedElement;
                try {
                    // Get all methods
                    List<ExecutableElement> allMethodsOfClass = this.processingEnv.getElementUtils().getAllMembers(typeElement).stream()
                            .filter(e -> {
                                boolean elementIsAMethod = e.getKind() == ElementKind.METHOD;
                                boolean isNotStaticAndFinal = !(e.getModifiers().contains(Modifier.FINAL) || e.getModifiers().contains(Modifier.STATIC));
                                boolean notHashCodeMethod = !"hashCode".equalsIgnoreCase(e.getSimpleName().toString());
                                boolean notEqualsMethod = !"equals".equalsIgnoreCase(e.getSimpleName().toString());
                                boolean notToStringMethod = !"toString".equalsIgnoreCase(e.getSimpleName().toString());
                                boolean annotatedWithGetOrPostOrPutOrDelete =
                                        e.getAnnotation(Get.class) != null ||
                                                e.getAnnotation(Post.class) != null ||
                                                e.getAnnotation(Put.class) != null ||
                                                e.getAnnotation(Delete.class) != null;
                                return elementIsAMethod &&
                                        isNotStaticAndFinal &&
                                        notHashCodeMethod &&
                                        notEqualsMethod &&
                                        notToStringMethod &&
                                        annotatedWithGetOrPostOrPutOrDelete;
                            })
                            .map(member -> (ExecutableElement) member).collect(Collectors.toList());
                    classMap.put(typeElement, allMethodsOfClass);


                } catch (Exception e) {
                    this.processingEnv.
                            getMessager().
                            printMessage(
                                    Diagnostic.Kind.ERROR,
                                    ExceptionUtils.getStackTrace(e)
                            );
                }
            }
        }

        boolean applicationHaveAuthentication = false;
        boolean applicationHaveAPIKeyAuthentication = false;

        if (!classMap.isEmpty()) {
            int count = 1;

            List<String> classProperties = new ArrayList<>();
            List<String> routerConfigures = new ArrayList<>();
            List<String> constructorParameters = new ArrayList<>();
            List<String> constructorBody = new ArrayList<>();

            for (Map.Entry<TypeElement, List<ExecutableElement>> currentClass : classMap.entrySet()) {
                System.out.printf("Configuring route for controller : %s\n", currentClass.getKey().getQualifiedName());
                constructorParameters.add(
                        String.format(
                                "%s controller%d",
                                currentClass.getKey().getQualifiedName() + CommonConstant.EMPTY_STRING,
                                count
                        )
                );
                constructorBody.add(
                        String.format(
                                "this.controller%1$d = controller%1$d;",
                                count
                        )
                );
                classProperties.add(
                        String.format(
                                "private final %s controller%d",
                                currentClass.getKey().getQualifiedName() + CommonConstant.EMPTY_STRING,
                                count
                        )
                );
                for (ExecutableElement e : currentClass.getValue()) {

                    boolean isAuthMethod = false;
                    boolean isAPIKeyMethod = false;

                    if (e.getAnnotation(Auth.class) != null) {
                        applicationHaveAuthentication = true;
                        isAuthMethod = true;
                    }

                    if (e.getAnnotation(APIKey.class) != null) {
                        applicationHaveAPIKeyAuthentication = true;
                        isAPIKeyMethod = true;
                    }

                    String basePath;

                    if (StringUtils.isNotBlank(currentClass.getKey().getAnnotation(Controller.class).path())) {
                        String controllerPath = currentClass.getKey().getAnnotation(Controller.class).path();
                        if (controllerPath.startsWith("/")) {
                            basePath = controllerPath;
                        } else {
                            basePath = "/" + controllerPath;
                        }
                    } else {
                        basePath = CommonConstant.EMPTY_STRING;
                    }

                    String routerConfigureCode = "";

                    if (e.getAnnotation(Get.class) != null) {
                        String apiPath;
                        if (StringUtils.isNotBlank(e.getAnnotation(Get.class).path())) {
                            String methodPath = e.getAnnotation(Get.class).path();
                            if (methodPath.startsWith("/")) {
                                apiPath = methodPath;
                            } else {
                                apiPath = "/" + methodPath;
                            }
                        } else {
                            apiPath = CommonConstant.EMPTY_STRING;
                        }
                        routerConfigureCode = String.format(
                                "router.get(\"%s\").handler(this::createUUIDHandler)",
                                basePath + apiPath
                        );
                    }
                    if (e.getAnnotation(Post.class) != null) {
                        String apiPath;
                        if (StringUtils.isNotBlank(e.getAnnotation(Post.class).path())) {
                            String methodPath = e.getAnnotation(Post.class).path();
                            if (methodPath.startsWith("/")) {
                                apiPath = methodPath;
                            } else {
                                apiPath = "/" + methodPath;
                            }
                        } else {
                            apiPath = CommonConstant.EMPTY_STRING;
                        }
                        routerConfigureCode = String.format(
                                "router.post(\"%s\").handler(this::createUUIDHandler)",
                                basePath + apiPath
                        );
                    }
                    if (e.getAnnotation(Put.class) != null) {
                        String apiPath;
                        if (StringUtils.isNotBlank(e.getAnnotation(Put.class).path())) {
                            String methodPath = e.getAnnotation(Put.class).path();
                            if (methodPath.startsWith("/")) {
                                apiPath = methodPath;
                            } else {
                                apiPath = "/" + methodPath;
                            }
                        } else {
                            apiPath = CommonConstant.EMPTY_STRING;
                        }
                        routerConfigureCode = String.format(
                                "router.put(\"%s\").handler(this::createUUIDHandler)",
                                basePath + apiPath
                        );
                    }
                    if (e.getAnnotation(Delete.class) != null) {
                        String apiPath;
                        if (StringUtils.isNotBlank(e.getAnnotation(Delete.class).path())) {
                            String methodPath = e.getAnnotation(Delete.class).path();
                            if (methodPath.startsWith("/")) {
                                apiPath = methodPath;
                            } else {
                                apiPath = "/" + methodPath;
                            }
                        } else {
                            apiPath = CommonConstant.EMPTY_STRING;
                        }
                        routerConfigureCode = String.format(
                                "router.delete(\"%s\").handler(this::createUUIDHandler)",
                                basePath + apiPath
                        );
                    }
                    if (StringUtils.isNotBlank(routerConfigureCode)) {
                        if (isAuthMethod) {
                            routerConfigureCode += ".handler(this::authenticate)";
                        }
                        if (isAPIKeyMethod) {
                            routerConfigureCode += ".handler(this::validateApiKey)";
                        }
                        routerConfigureCode += String.format(
                                ".handler(this.controller%d::%s);",
                                count,
                                e.getSimpleName() + CommonConstant.EMPTY_STRING
                        );
                        routerConfigures.add(routerConfigureCode);
                    }
                }
                ++count;
            }
            if (applicationHaveAuthentication) {
                constructorParameters.add("JWTAuth jwtAuth");
                constructorBody.add("this.jwtAuth = jwtAuth;");
            }
            final String constructor = String.format(
                    "\n    public ApplicationVerticle(%s) {\n%s\n    }\n",
                    String.join(", ", constructorParameters),
                    constructorBody.stream().collect(Collectors.joining("\n       ", "       ", CommonConstant.EMPTY_STRING))
            );
            String codeToWrite = String.format(
                    "" +
                            "package vn.com.lcx.vertx.verticle;\n" +
                            "\n" +
                            "import io.vertx.core.Promise;\n" +
                            "import io.vertx.ext.auth.authentication.TokenCredentials;\n" +
                            "import io.vertx.ext.auth.jwt.JWTAuth;\n" +
                            "import io.vertx.ext.web.RoutingContext;\n" +
                            "import io.vertx.ext.web.handler.BodyHandler;\n" +
                            "import vn.com.lcx.common.annotation.Verticle;\n" +
                            "import vn.com.lcx.common.constant.CommonConstant;\n" +
                            "import vn.com.lcx.common.utils.LogUtils;\n" +
                            "import vn.com.lcx.common.utils.MyStringUtils;\n" +
                            "import vn.com.lcx.vertx.base.config.HttpOption;\n" +
                            "import vn.com.lcx.vertx.base.custom.MyRouter;\n" +
                            "import vn.com.lcx.vertx.base.verticle.VertxBaseVerticle;\n" +
                            "\n" +
                            "@Verticle\n" +
                            "public class ApplicationVerticle extends VertxBaseVerticle {\n" +
                            "\n" +
                            "    %s\n" +
                            "%s" +
                            "%s\n" +
                            "    @Override\n" +
                            "    public void start(Promise<Void> startPromise) {\n" +
                            "        try {\n" +
                            "            io.vertx.ext.web.Router router = MyRouter.router(super.vertx);\n" +
                            "\n" +
                            "            // Enable parsing of request bodies\n" +
                            "            router.route().handler(BodyHandler.create());\n" +
                            "\n" +
                            "            router.get(\"/health\").handler(routingContext -> routingContext.response().end(\"OK\"));\n" +
                            "            router.get(\"/starting_probe\").handler(routingContext -> {\n" +
                            "                routingContext.response().end(\"OK\");\n" +
                            "            });\n" +
                            "            %s\n" +
                            "            final String portString = CommonConstant.applicationConfig.getProperty(\"server.port\");\n" +
                            "            int port;\n" +
                            "            if (MyStringUtils.isNotBlank(portString) && MyStringUtils.isNumeric(portString)) {\n" +
                            "                port = Integer.parseInt(portString);\n" +
                            "            } else {\n" +
                            "                port = 8080;\n" +
                            "            }\n" +
                            "\n" +
                            "            final boolean enableHttp2 = Boolean.parseBoolean(CommonConstant.applicationConfig.getProperty(\"server.enable-http-2\") + CommonConstant.EMPTY_STRING);\n" +
                            "\n" +
                            "            if (enableHttp2) {\n" +
                            "                super.vertx.createHttpServer(HttpOption.configureHttp2H2C(port))\n" +
                            "                        .requestHandler(router)\n" +
                            "                        .listen()\n" +
                            "                        .onSuccess(server -> {\n" +
                            "                            LogUtils.writeLog(LogUtils.Level.INFO, \"HTTP2 server started on port \" + port);\n" +
                            "                            startPromise.complete();\n" +
                            "                        })\n" +
                            "                        .onFailure(startPromise::fail);\n" +
                            "            } else {\n" +
                            "                super.vertx.createHttpServer()\n" +
                            "                        .requestHandler(router)\n" +
                            "                        .listen(port)\n" +
                            "                        .onSuccess(server -> {\n" +
                            "                            LogUtils.writeLog(LogUtils.Level.INFO, \"HTTP server started on port \" + port);\n" +
                            "                            startPromise.complete();\n" +
                            "                        })\n" +
                            "                        .onFailure(startPromise::fail);\n" +
                            "            }\n" +
                            "        } catch (Throwable e) {\n" +
                            "            LogUtils.writeLog(e.getMessage(), e);\n" +
                            "        }\n" +
                            "    }\n" +
                            "\n" +
                            "%s" +
                            "%s" +
                            "\n" +
                            "}\n",
                    classProperties.stream().collect(Collectors.joining(";\n    ", CommonConstant.EMPTY_STRING, ";")),
                    applicationHaveAuthentication ? "    private final JWTAuth jwtAuth;\n" : CommonConstant.EMPTY_STRING,
                    constructor,
                    routerConfigures.stream().collect(Collectors.joining("\n            ", CommonConstant.EMPTY_STRING, "\n")),
                    applicationHaveAuthentication ? "    private void authenticate(RoutingContext ctx) {\n" +
                            "        String authHeader = ctx.request().getHeader(\"Authorization\");\n" +
                            "        if (authHeader == null || !authHeader.startsWith(\"Bearer \")) {\n" +
                            "            ctx.response().setStatusCode(401).end(\"Missing or invalid Authorization header\");\n" +
                            "            return;\n" +
                            "        }\n" +
                            "        String token = authHeader.substring(7);  // Remove \"Bearer \" prefix\n" +
                            "        this.jwtAuth.authenticate(new TokenCredentials(token))\n" +
                            "                .onSuccess(user -> {\n" +
                            "                    ctx.setUser(user);\n" +
                            "                    ctx.next();  // Continue to the next handler\n" +
                            "                })\n" +
                            "                .onFailure(err -> ctx.response().setStatusCode(401).end(\"Invalid token\")\n" +
                            "                );\n" +
                            "    }\n" : CommonConstant.EMPTY_STRING,
                    applicationHaveAPIKeyAuthentication ?
                                    "    public void validateApiKey(RoutingContext context) {\n" +
                                    "        String apiKey = context.request().getHeader(\"x-api-key\");\n" +
                                    "        String validApiKey = CommonConstant.applicationConfig.getProperty(\"server.api-key\");\n" +
                                    "        if (!((apiKey + CommonConstant.EMPTY_STRING).equals(validApiKey))) {\n" +
                                    "            context.response().setStatusCode(401).end(\"Invalid api key\");\n" +
                                    "        } else {\n" +
                                    "            context.next();\n" +
                                    "        }\n" +
                                    "    }\n" :
                            CommonConstant.EMPTY_STRING
            );
            try {
                JavaFileObject builderFile = this.processingEnv.getFiler().createSourceFile("vn.com.lcx.vertx.verticle.ApplicationVerticle");
                try (Writer writer = builderFile.openWriter()) {
                    writer.write(codeToWrite);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

}
