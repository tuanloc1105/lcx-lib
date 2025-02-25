package vn.com.lcx.vertx.base.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.Getter;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import vn.com.lcx.common.config.BuildGson;
import vn.com.lcx.common.constant.CommonConstant;
import vn.com.lcx.common.utils.DateTimeUtils;
import vn.com.lcx.common.utils.LogUtils;
import vn.com.lcx.common.utils.MyStringUtils;
import vn.com.lcx.vertx.base.constant.VertxBaseConstant;
import vn.com.lcx.vertx.base.enums.ErrorCodeEnums;
import vn.com.lcx.vertx.base.exception.InternalServiceException;
import vn.com.lcx.vertx.base.http.request.BaseRequest;
import vn.com.lcx.vertx.base.http.response.CommonResponse;
import vn.com.lcx.vertx.base.validate.AutoValidation;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BaseController {

    public final static TypeToken<Void> VOID = new TypeToken<Void>() {
    };
    @Getter
    protected final Vertx vertx;
    protected Gson gson;
    @SuppressWarnings("FieldMayBeFinal")
    private Logger requestLogger;
    @SuppressWarnings("FieldMayBeFinal")
    private Logger responseLogger;
    @SuppressWarnings("FieldMayBeFinal")
    private Logger exceptionLogger;

    public BaseController(Vertx vertx) {
        this.vertx = vertx;
        val dateFormatType = System.getenv("DATE_FORMAT_TYPE");
        if ("VN".equals(dateFormatType)) {
            this.gson = BuildGson.getVietnameseDateFormatGson();
        } else {
            this.gson = BuildGson.getGson();
        }
        this.requestLogger = LoggerFactory.getLogger("request");
        this.responseLogger = LoggerFactory.getLogger("response");
        this.exceptionLogger = LoggerFactory.getLogger("exception");
    }

    protected String getRequestQueryParam(RoutingContext context, String paramName) {
        val paramValue = context.queryParam(paramName);
        if (CollectionUtils.isEmpty(paramValue)) {
            throw new InternalServiceException(
                    ErrorCodeEnums.INVALID_REQUEST,
                    String.format("Query parameter `%s` can not be empty", paramName)
            );
        }
        if (StringUtils.isBlank(paramValue.get(0))) {
            throw new InternalServiceException(
                    ErrorCodeEnums.INVALID_REQUEST,
                    String.format("Query parameter `%s` can not be empty", paramName)
            );
        }
        return paramValue.get(0);
    }

    protected <T> T getRequestQueryParam(RoutingContext context, String paramName, Function<String, T> function) {
        val paramValue = context.queryParam(paramName);
        if (CollectionUtils.isEmpty(paramValue)) {
            throw new InternalServiceException(
                    ErrorCodeEnums.INVALID_REQUEST,
                    String.format("Query parameter `%s` can not be empty", paramName)
            );
        }
        if (StringUtils.isBlank(paramValue.get(0))) {
            throw new InternalServiceException(
                    ErrorCodeEnums.INVALID_REQUEST,
                    String.format("Query parameter `%s` can not be empty", paramName)
            );
        }
        return function.apply(paramValue.get(0));
    }

    protected List<String> getRequestQueryParamInList(RoutingContext context, String paramName) {
        final List<String> paramValue = context.queryParam(paramName).isEmpty() ?
                new ArrayList<>() :
                Arrays.stream(context.queryParam(paramName).get(0).split(",")).map(String::trim).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(paramValue)) {
            throw new InternalServiceException(
                    ErrorCodeEnums.INVALID_REQUEST,
                    String.format("%s can not be empty", paramValue)
            );
        }
        if (paramValue.stream().anyMatch(StringUtils::isBlank)) {
            throw new InternalServiceException(
                    ErrorCodeEnums.INVALID_REQUEST,
                    String.format("%s's elements can not be empty", paramValue)
            );
        }
        return paramValue;
    }

    protected <T> List<T> getRequestQueryParamInList(RoutingContext context, String paramName, Function<String, T> function) {
        final List<String> paramValue = context.queryParam(paramName).isEmpty() ?
                new ArrayList<>() :
                Arrays.stream(context.queryParam(paramName).get(0).split(",")).map(String::trim).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(paramValue)) {
            throw new InternalServiceException(
                    ErrorCodeEnums.INVALID_REQUEST,
                    String.format("%s can not be empty", paramValue)
            );
        }
        if (paramValue.stream().anyMatch(StringUtils::isBlank)) {
            throw new InternalServiceException(
                    ErrorCodeEnums.INVALID_REQUEST,
                    String.format("%s's elements can not be empty", paramValue)
            );
        }
        return paramValue.stream().map(function).collect(Collectors.toList());
    }

    protected String getNoneRequiringRequestQueryParam(RoutingContext context, String paramName) {
        val paramValue = context.queryParam(paramName);
        if (CollectionUtils.isEmpty(paramValue)) {
            return CommonConstant.EMPTY_STRING;
        }
        return paramValue.get(0);
    }

    protected <T> T getNoneRequiringRequestQueryParam(RoutingContext context, String paramName, Function<String, T> function) {
        val paramValue = context.queryParam(paramName);
        if (CollectionUtils.isEmpty(paramValue)) {
            return null;
        }
        return function.apply(paramValue.get(0));
    }

    protected List<String> getNoneRequiringRequestQueryParamInList(RoutingContext context, String paramName) {
        val paramValue = context.queryParam(paramName).isEmpty() ?
                new ArrayList<String>() :
                Arrays.stream(context.queryParam(paramName).get(0).split(",")).map(String::trim).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(paramValue)) {
            new ArrayList<>();
        }
        return paramValue;
    }

    protected <T> List<T> getNoneRequiringRequestQueryParamInList(RoutingContext context, String paramName, Function<String, T> function) {
        val paramValue = context.queryParam(paramName).isEmpty() ?
                new ArrayList<String>() :
                Arrays.stream(context.queryParam(paramName).get(0).split(",")).map(String::trim).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(paramValue)) {
            new ArrayList<>();
        }
        return paramValue.stream().map(function).collect(Collectors.toList());
    }

    protected <T extends BaseRequest> T getRequestBodyFromContext(RoutingContext context, Gson gson, Class<T> clz) {
        context.request().uri();
        val requestBody = context.body().asString(CommonConstant.UTF_8_STANDARD_CHARSET);
        val request = gson.fromJson(requestBody, clz);
        request.validate();
        return request;
    }

    protected <T extends CommonResponse, B> void executeThreadBlock(RoutingContext context, RequestHandler<T, B> requestHandler, TypeToken<B> requestBodyClass) {
        val startingTime = (double) System.currentTimeMillis();
        val trace = (String) context.get(CommonConstant.TRACE_ID_MDC_KEY_NAME);

        final Future<@Nullable CommonResponse> blockingFutureTask = vertx.executeBlocking(() -> {
            MDC.put(CommonConstant.TRACE_ID_MDC_KEY_NAME, trace);
            final MultiMap requestHeader = context.request().headers();

            val headerLogMsg = new ArrayList<String>();

            for (Map.Entry<String, String> requestQueryParam : requestHeader) {
                headerLogMsg.add(
                        String.format(
                                "        - Name: %s\n          Value: %s",
                                requestQueryParam.getKey(),
                                requestQueryParam.getValue()
                        )
                );
            }

            val requestBody = MyStringUtils.minifyJsonString(context.body().asString(CommonConstant.UTF_8_STANDARD_CHARSET));

            BaseController.this.requestLogger.info(
                    "[{}] Request:\n    - URL: {}\n    - Header:\n{}\n    - Payload:\n        {}",
                    trace,
                    context.request().uri(),
                    String.join("\n", headerLogMsg),
                    requestBody
            );

            final T response;
            if (VOID.equals(requestBodyClass)) {
                response = requestHandler.handle(context, null);
            } else {
                if (StringUtils.isBlank(requestBody)) {
                    throw new InternalServiceException(ErrorCodeEnums.INVALID_REQUEST, "Empty request body");
                }
                LogUtils.writeLog(LogUtils.Level.INFO, requestBody);
                B requestObject = gson.fromJson(requestBody, requestBodyClass.getType());
                val errorFields = AutoValidation.validate(requestObject);
                if (!errorFields.isEmpty()) {
                    throw new InternalServiceException(ErrorCodeEnums.INVALID_REQUEST, errorFields.toString());
                }
                response = requestHandler.handle(context, requestObject);
            }

            response.setTrace(trace);
            response.setErrorCode(ErrorCodeEnums.SUCCESS.getCode());
            response.setErrorDescription(ErrorCodeEnums.SUCCESS.getMessage());
            response.setHttpCode(200);

            MDC.remove(CommonConstant.TRACE_ID_MDC_KEY_NAME);

            return response;
        }, false);
        blockingFutureTask.onSuccess(response -> {
            MDC.put(CommonConstant.TRACE_ID_MDC_KEY_NAME, trace);
            val endingTime = (double) System.currentTimeMillis();
            val duration = (endingTime - startingTime) / 1000D;
            String responseBody = gson.toJson(response);

            context.response()
                    .putHeader(VertxBaseConstant.CONTENT_TYPE_HEADER_NAME, VertxBaseConstant.CONTENT_TYPE_APPLICATION_JSON)
                    .putHeader(
                            VertxBaseConstant.PROCESSED_TIME_HEADER_NAME,
                            DateTimeUtils.generateCurrentTimeDefault()
                                    .format(
                                            DateTimeFormatter.ofPattern(CommonConstant.DEFAULT_LOCAL_DATE_TIME_STRING_PATTERN)
                                    )
                    )
                    .putHeader(VertxBaseConstant.TRACE_HEADER_NAME, trace)
                    .end(responseBody);
            BaseController.this.responseLogger.info(
                    "[{}] Response ({} second(s)):\n    - Payload:\n        {}",
                    trace,
                    duration,
                    responseBody
            );
            MDC.remove(CommonConstant.TRACE_ID_MDC_KEY_NAME);
        });
        blockingFutureTask.onFailure(e -> {
            MDC.put(CommonConstant.TRACE_ID_MDC_KEY_NAME, trace);
            val endingTime = (double) System.currentTimeMillis();
            val duration = (endingTime - startingTime) / 1000D;
            BaseController.this.exceptionLogger.error("[{}] - {}", trace, e.getMessage(), e);
            CommonResponse response;
            int httpCode = 500;
            if (e instanceof InternalServiceException) {
                InternalServiceException internalServiceException = (InternalServiceException) e;
                httpCode = internalServiceException.getHttpCode();
                response = CommonResponse.builder()
                        .trace(trace)
                        .errorCode(internalServiceException.getCode())
                        .errorDescription(internalServiceException.getMessage())
                        .httpCode(httpCode)
                        .build();
            } else {
                response = CommonResponse.builder()
                        .trace(trace)
                        .errorCode(-1)
                        .errorDescription(e.getMessage())
                        .httpCode(httpCode)
                        .build();
            }
            String responseBody = this.gson.toJson(response);
            context.response().setStatusCode(httpCode)
                    .putHeader(VertxBaseConstant.CONTENT_TYPE_HEADER_NAME, VertxBaseConstant.CONTENT_TYPE_APPLICATION_JSON)
                    .putHeader(
                            VertxBaseConstant.PROCESSED_TIME_HEADER_NAME,
                            DateTimeUtils.generateCurrentTimeDefault()
                                    .format(
                                            DateTimeFormatter.ofPattern(CommonConstant.DEFAULT_LOCAL_DATE_TIME_STRING_PATTERN)
                                    )
                    )
                    .putHeader(VertxBaseConstant.TRACE_HEADER_NAME, trace)
                    .end(responseBody);
            BaseController.this.responseLogger.warn(
                    "[{}] Response ({} second(s)):\n    - Payload:\n        {}",
                    trace,
                    duration,
                    responseBody
            );
            MDC.remove(CommonConstant.TRACE_ID_MDC_KEY_NAME);
        });
    }

    protected <T extends CommonResponse, B> void execute(RoutingContext context, RequestHandler<T, B> requestHandler, TypeToken<B> requestBodyClass) {
        val startingTime = (double) System.currentTimeMillis();
        val trace = (String) context.get(CommonConstant.TRACE_ID_MDC_KEY_NAME);
        MDC.put(CommonConstant.TRACE_ID_MDC_KEY_NAME, trace);
        String responseBody = CommonConstant.EMPTY_STRING;
        int httpStatusCode = 200;
        try {
            final MultiMap requestHeader = context.request().headers();

            val headerLogMsg = new ArrayList<String>();

            for (Map.Entry<String, String> requestQueryParam : requestHeader) {
                headerLogMsg.add(
                        String.format(
                                "        - Name: %s\n          Value: %s",
                                requestQueryParam.getKey(),
                                requestQueryParam.getValue()
                        )
                );
            }

            val requestBody = MyStringUtils.minifyJsonString(context.body().asString(CommonConstant.UTF_8_STANDARD_CHARSET));

            LogUtils.writeLog(
                    LogUtils.Level.INFO,
                    "Request:\n    - URL: {}\n    - Header:\n{}\n    - Payload:\n        {}",
                    context.request().uri(),
                    String.join("\n", headerLogMsg),
                    requestBody
            );

            final T response;
            if (VOID.equals(requestBodyClass)) {
                response = requestHandler.handle(context, null);
            } else {
                if (StringUtils.isBlank(requestBody)) {
                    throw new InternalServiceException(ErrorCodeEnums.INVALID_REQUEST, "Empty request body");
                }
                response = requestHandler.handle(context, gson.fromJson(requestBody, requestBodyClass.getType()));
            }

            response.setTrace(trace);
            response.setErrorCode(ErrorCodeEnums.SUCCESS.getCode());
            response.setErrorDescription(ErrorCodeEnums.SUCCESS.getMessage());
            response.setHttpCode(200);

            responseBody = gson.toJson(response);

            context.response()
                    .putHeader(VertxBaseConstant.CONTENT_TYPE_HEADER_NAME, VertxBaseConstant.CONTENT_TYPE_APPLICATION_JSON)
                    .putHeader(
                            VertxBaseConstant.PROCESSED_TIME_HEADER_NAME,
                            DateTimeUtils.generateCurrentTimeDefault()
                                    .format(
                                            DateTimeFormatter.ofPattern(CommonConstant.DEFAULT_LOCAL_DATE_TIME_STRING_PATTERN)
                                    )
                    )
                    .putHeader(VertxBaseConstant.TRACE_HEADER_NAME, trace)
                    .end(responseBody);
        } catch (Exception e) {
            LogUtils.writeLog(e.getMessage(), e);
            CommonResponse response;
            int httpCode = 500;
            if (e instanceof InternalServiceException) {
                InternalServiceException internalServiceException = (InternalServiceException) e;
                httpCode = internalServiceException.getHttpCode();
                response = CommonResponse.builder()
                        .trace(trace)
                        .errorCode(internalServiceException.getCode())
                        .errorDescription(internalServiceException.getMessage())
                        .httpCode(httpCode)
                        .build();
            } else {
                response = CommonResponse.builder()
                        .trace(trace)
                        .errorCode(-1)
                        .errorDescription(e.getMessage())
                        .httpCode(httpCode)
                        .build();
            }
            responseBody = this.gson.toJson(response);
            context.response().setStatusCode(httpCode)
                    .putHeader(VertxBaseConstant.CONTENT_TYPE_HEADER_NAME, VertxBaseConstant.CONTENT_TYPE_APPLICATION_JSON)
                    .putHeader(
                            VertxBaseConstant.PROCESSED_TIME_HEADER_NAME,
                            DateTimeUtils.generateCurrentTimeDefault()
                                    .format(
                                            DateTimeFormatter.ofPattern(CommonConstant.DEFAULT_LOCAL_DATE_TIME_STRING_PATTERN)
                                    )
                    )
                    .putHeader(VertxBaseConstant.TRACE_HEADER_NAME, trace)
                    .end(responseBody);
            httpStatusCode = httpCode;
            // LogUtils.writeLog(LogUtils.Level.WARN, responseBody);
        } finally {
            val endingTime = (double) System.currentTimeMillis();
            val duration = (endingTime - startingTime) / 1000D;
            if (httpStatusCode == 200) {
                LogUtils.writeLog(
                        LogUtils.Level.INFO,
                        "Response ({} second(s)):\n    - Payload:\n        {}",
                        duration,
                        responseBody
                );
            } else {
                LogUtils.writeLog(
                        LogUtils.Level.WARN,
                        "Response ({} second(s)):\n    - Payload:\n        {}",
                        duration,
                        responseBody
                );
            }
        }
        MDC.remove(CommonConstant.TRACE_ID_MDC_KEY_NAME);
    }

    protected <T> T getUser(RoutingContext context, TypeToken<T> typeToken) {
        final JsonObject jsonObject = context.user().get("accessToken");
        return this.gson.fromJson(
                jsonObject.encode(),
                typeToken.getType()
        );
    }

    protected interface RequestHandler<T extends CommonResponse, B> {
        T handle(RoutingContext context, B input);
    }

}
