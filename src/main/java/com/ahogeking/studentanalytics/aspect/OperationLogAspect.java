package com.ahogeking.studentanalytics.aspect;

import com.ahogeking.studentanalytics.annotation.LogOperation;
import com.ahogeking.studentanalytics.common.constant.OperationResult;
import com.ahogeking.studentanalytics.common.constant.OperationTargetType;
import com.ahogeking.studentanalytics.context.SysUserContext;
import com.ahogeking.studentanalytics.entity.OperationLog;
import com.ahogeking.studentanalytics.entity.SysUser;
import com.ahogeking.studentanalytics.service.OperationLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class OperationLogAspect {
    private static final String MASKED_VALUE = "***";

    private final OperationLogService operationLogService;

    private final ObjectMapper objectMapper;

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(logOperation)")
    public Object around(ProceedingJoinPoint joinPoint, LogOperation logOperation) throws Throwable {

        try {
            Object result = joinPoint.proceed();
            saveOperationLog(
                    joinPoint,
                    logOperation,
                    OperationResult.SUCCESS
            );
            return result;
        } catch (Throwable ex) {
            saveOperationLog(
                    joinPoint,
                    logOperation,
                    OperationResult.FAIL
            );
            throw ex;
        }
    }

    private void saveOperationLog(
            ProceedingJoinPoint joinPoint,
            LogOperation annotation,
            String operationResult
    ) {
        try {
            OperationLog log = buildOperationLog(joinPoint, annotation, operationResult);
            operationLogService.saveOperationLog(log);
        } catch (Exception e) {
            log.warn("saveOperationLog error:", e);
        }
    }

    private OperationLog buildOperationLog(
            ProceedingJoinPoint joinPoint,
            LogOperation annotation,
            String operationResult
    ) {
        OperationLog log = new OperationLog();

        log.setModuleName(annotation.module());
        log.setOperationType(annotation.type());
        log.setOperationResult(operationResult);
        log.setTargetType(emptyToNull(annotation.targetType()));
        log.setTargetId(parseExpression(annotation.targetId(), joinPoint));
        log.setBusinessKey(parseExpression(annotation.businessKey(), joinPoint));

        fillCurrentUser(log);
        fillTargetFallback(log);
        fillHttpRequest(log, annotation.recordRequest(), joinPoint);

        return log;
    }

    private void fillCurrentUser(OperationLog log) {
        SysUser currentUser = SysUserContext.get();
        if (currentUser == null) {
            return;
        }
        log.setUserId(currentUser.getId());
        log.setUsername(currentUser.getUsername());
        log.setRealName(currentUser.getRealName());
        log.setUserRole(currentUser.getRole());
    }

    private void fillTargetFallback(OperationLog log) {
        if (!OperationTargetType.USER.equals(log.getTargetType()) || log.getTargetId() != null) {
            return;
        }
        Integer currentUserId = log.getUserId();
        if (currentUserId != null) {
            log.setTargetId(String.valueOf(currentUserId));
        }
    }

    private void fillHttpRequest(OperationLog log, boolean recordRequest, ProceedingJoinPoint joinPoint) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            log.setRequestParams("{}");
            log.setRequestBody("{}");
            return;
        }
        log.setRequestMethod(request.getMethod());
        log.setRequestUri(request.getRequestURI());
        log.setIpAddress(getClientIp(request));
        if (recordRequest) {
            log.setRequestParams(buildRequestParamsJson(request));
            log.setRequestBody(buildRequestBodyJson(joinPoint));
        } else {
            log.setRequestParams("{}");
            log.setRequestBody("{}");
        }
    }

    private HttpServletRequest getCurrentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        return servletRequestAttributes.getRequest();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String parseExpression(String expression, ProceedingJoinPoint joinPoint) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
            if (parameterNames == null) {
                parameterNames = signature.getParameterNames();
            }
            Object[] args = joinPoint.getArgs();
            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; parameterNames != null && i < parameterNames.length && i < args.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
            Object value = expressionParser.parseExpression(expression).getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            log.warn("parse operation log expression error: {}", expression, e);
            return null;
        }
    }

    private String buildRequestParamsJson(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return "{}";
        }
        return toMaskedJson(parameterMap);
    }

    private String buildRequestBodyJson(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        List<Object> loggableArgs = Arrays.stream(args)
                .filter(this::isLoggableArgument)
                .toList();
        if (loggableArgs.isEmpty()) {
            return "{}";
        }
        if (loggableArgs.size() == 1) {
            return toMaskedJson(loggableArgs.getFirst());
        }
        return toMaskedJson(loggableArgs);
    }

    private boolean isLoggableArgument(Object arg) {
        if (arg == null) {
            return false;
        }
        return !(arg instanceof HttpServletRequest)
                && !(arg instanceof HttpServletResponse)
                && !(arg instanceof MultipartFile)
                && !(arg instanceof BindingResult);
    }

    private String toMaskedJson(Object value) {
        try {
            JsonNode node = objectMapper.valueToTree(value);
            maskJsonNode(node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("serialize operation log request error", e);
            return "{}";
        }
    }

    private void maskJsonNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String fieldName = entry.getKey().toLowerCase();
                if (fieldName.contains("password")
                        || fieldName.contains("token")
                        || fieldName.contains("authorization")) {
                    objectNode.put(entry.getKey(), MASKED_VALUE);
                } else {
                    maskJsonNode(entry.getValue());
                }
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                maskJsonNode(item);
            }
        }
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
