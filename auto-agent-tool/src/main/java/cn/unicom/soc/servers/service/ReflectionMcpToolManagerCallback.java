package cn.unicom.soc.servers.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于反射的 MCP 工具管理回调实现
 * 通过反射调用 MCP 工具管理服务实现类的具体方法
 */
public class ReflectionMcpToolManagerCallback implements ToolCallback {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionMcpToolManagerCallback.class);
    private final ToolDefinition definition;
    private final Object managerService;  // 使用通用对象类型以支持多种服务类型
    private final ObjectMapper objectMapper;

    public ReflectionMcpToolManagerCallback(String toolName, String description, String inputSchema, Object managerService) {
        this.managerService = managerService;
        this.objectMapper = new ObjectMapper();

        this.definition = ToolDefinition.builder()
                .name(toolName)
                .description(description)
                .inputSchema(inputSchema)
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return definition;
    }

    @Override
    public String call(String toolInput) {
        try {
            // 解析传入的 JSON 参数
            Map<String, Object> params = objectMapper.readValue(toolInput, Map.class);

            // 获取工具名称
            String toolName = definition.name();

            // 通过反射调用相应的方法
            Method method = findMethod(toolName);
            if (method != null) {
                // 调用方法，传入参数
                return (String) method.invoke(managerService, params);
            } else {
                // 记录详细诊断信息
                logAvailableMethods(toolName);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", "Unknown tool: " + toolName);
                return objectMapper.writeValueAsString(errorResult);
            }
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Error processing tool call: " + e.getMessage());

            try {
                return objectMapper.writeValueAsString(errorResult);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        }
    }

    /**
     * 根据工具名称找到对应的方法
     */
    private Method findMethod(String toolName) {
        try {
            // 将工具名称转换为方法名称
            String methodName = convertToolNameToMethodName(toolName);

            // 根据实际对象类型查找对应的方法，接受Map<String, Object>参数
            Class<?>[] paramTypes = {Map.class};
            Class<?> serviceClass = managerService.getClass();

            // 1. 优先使用 getMethod 查找（包含继承的 public 方法）
            try {
                Method method = serviceClass.getMethod(methodName, paramTypes);
                logger.debug("Found method via getMethod: {}.{}({})", serviceClass.getName(), methodName, Map.class.getName());
                return method;
            } catch (NoSuchMethodException ignored) {
            }

            // 2. 降级使用 getDeclaredMethod 查找当前类声明的方法
            try {
                Method method = serviceClass.getDeclaredMethod(methodName, paramTypes);
                if (!method.canAccess(managerService)) {
                    method.setAccessible(true);
                }
                logger.debug("Found method via getDeclaredMethod: {}.{}({})", serviceClass.getName(), methodName, Map.class.getName());
                return method;
            } catch (NoSuchMethodException ignored) {
            }

            // 3. 尝试在所有声明的方法中按名称模糊匹配（忽略参数类型）
            for (Method m : serviceClass.getDeclaredMethods()) {
                if (m.getName().equals(methodName)) {
                    if (!m.canAccess(managerService)) {
                        m.setAccessible(true);
                    }
                    logger.warn("Found method by name only (parameter type mismatch expected): {}.{} -> actual params: {}",
                            serviceClass.getName(), methodName, java.util.Arrays.toString(m.getParameterTypes()));
                    return m;
                }
            }

            return null;
        } catch (Exception e) {
            logger.error("Error finding method for tool: {}", toolName, e);
            return null;
        }
    }

    /**
     * 记录可用的方法列表用于诊断
     */
    private void logAvailableMethods(String toolName) {
        Class<?> serviceClass = managerService.getClass();
        StringBuilder sb = new StringBuilder();
        sb.append("[ReflectionMcpToolManagerCallback] Unknown tool: ").append(toolName).append("\n");
        sb.append("  managerService class: ").append(serviceClass.getName()).append("\n");
        sb.append("  converted methodName: ").append(convertToolNameToMethodName(toolName)).append("\n");
        sb.append("  available public methods (accepting Map):\n");
        for (Method m : serviceClass.getMethods()) {
            if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == Map.class) {
                sb.append("    - ").append(m.getName()).append("(Map)\n");
            }
        }
        sb.append("  all declared methods:\n");
        for (Method m : serviceClass.getDeclaredMethods()) {
            sb.append("    - ").append(m.getName()).append("(");
            for (int i = 0; i < m.getParameterTypes().length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(m.getParameterTypes()[i].getSimpleName());
            }
            sb.append(")\n");
        }
        logger.error(sb.toString());
    }

    /**
     * 将工具名称转换为方法名称
     * 例如: "addOrUpdateHttpTool" -> "addOrUpdateHttpTool"
     * 或者 "delete-http-tool" -> "deleteHttpTool"
     */
    private String convertToolNameToMethodName(String toolName) {
        // 如果工具名称已经是驼峰命名法，直接返回
        if (toolName.matches("[a-z][a-zA-Z0-9]*")) {
            return toolName;
        }

        // 如果是连字符命名法，转换为驼峰命名法
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : toolName.toCharArray()) {
            if (c == '-') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
        }

        // 确保第一个字母是小写的
        if (result.length() > 0) {
            result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
        }

        return result.toString();
    }
}