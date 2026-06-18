package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.common.annotation.Tool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import cn.unicom.soc.servers.service.ChunkQueryTool;
import cn.unicom.soc.servers.common.annotation.ToolInput;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义方法工具回调提供者
 * 通过反射扫描对象中带有@Tool注解的方法，并将其注册为工具
 */
public class CustomMethodToolCallbackProvider implements ToolCallbackProvider {
    
    private final List<ToolCallback> toolCallbacks;
    
    /**
     * 构造函数，接收一个或多个带有@Tool注解的对象实例
     * @param objects 带有@Tool注解方法的对象实例
     */
    public CustomMethodToolCallbackProvider(Object... objects) {
        this.toolCallbacks = new ArrayList<>();
        
        // 遍历所有传入的对象
        for (Object obj : objects) {
            registerTools(obj);
        }
    }
    
    /**
     * 注册对象中所有带有@Tool注解的方法为工具
     * @param obj 带有@Tool注解方法的对象实例
     */
    private void registerTools(Object obj) {
        Class<?> clazz = obj.getClass();
        
        // 获取类及其父类中的所有方法
        Method[] methods = clazz.getDeclaredMethods();
        
        for (Method method : methods) {
            // 检查方法是否有@Tool注解
            if (method.isAnnotationPresent(Tool.class)) {
                Tool toolAnnotation = method.getAnnotation(Tool.class);
                
                // 获取工具名称，如果注解中未指定，则使用方法名
                String toolName = toolAnnotation.name().isEmpty() ? method.getName() : toolAnnotation.name();
                String description = toolAnnotation.description();
                String schema = toolAnnotation.schema();
                
                // 如果schema为空，生成一个默认的schema
                if (schema == null || schema.isEmpty()) {
                    schema = generateDefaultSchema(method);
                }
                
                // 根据方法所属对象类型创建相应的回调实例
                ToolCallback callback = createCallbackForObjectType(obj, toolName, description, schema);
                
                if (callback != null) {
                    toolCallbacks.add(callback);
                }
            }
        }
    }
    
    /**
     * 为不同类型的对象创建相应的回调实例
     * @param obj 对象实例
     * @param toolName 工具名称
     * @param description 工具描述
     * @param schema 工具schema
     * @return ToolCallback实例
     */
    private ToolCallback createCallbackForObjectType(Object obj, String toolName, String description, String schema) {
        // 支持所有使用 @Tool 注解且方法参数为 Map<String, Object> 的服务类
        // 通过 ReflectionMcpToolManagerCallback 进行通用反射调用
        if (obj instanceof HttpApiManagerService || obj instanceof ChunkQueryTool) {
            return new ReflectionMcpToolManagerCallback(toolName, description, schema, obj);
        }
        
        // 对于其他类型的对象，可以根据需要扩展更多类型
        // 目前返回null，表示不支持该类型的对象
        System.err.println("Unsupported object type for Tool annotation: " + obj.getClass().getName());
        return null;
    }
    
    /**
     * 为方法生成默认的schema
     * @param method 方法
     * @return 默认schema JSON字符串
     */
    private String generateDefaultSchema(Method method) {
        // 获取方法参数信息并生成schema
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            // 如果方法没有参数，返回空对象schema
            return "{\"type\": \"object\", \"properties\": {}, \"required\": []}";
        }
        
        StringBuilder schemaBuilder = new StringBuilder();
        schemaBuilder.append("{\"type\": \"object\", \"properties\": {");
        
        boolean hasParams = false;
        for (int i = 0; i < parameters.length; i++) {
            if (hasParams) {
                schemaBuilder.append(",");
            }
            
            java.lang.reflect.Parameter param = parameters[i];
            String paramName = param.getName();
            String paramTypeName = param.getType().getSimpleName();
            
            // 尝试获取参数上的@ToolInput注解
            ToolInput toolInput = param.getAnnotation(ToolInput.class);
            
            // 开始构建参数定义
            schemaBuilder.append("\"").append(paramName).append("\": {\"type\": \"");
            
            // 根据参数类型确定schema类型
            if (param.getType().equals(String.class)) {
                schemaBuilder.append("string");
            } else if (param.getType().equals(int.class) || param.getType().equals(Integer.class)) {
                schemaBuilder.append("integer");
            } else if (param.getType().equals(double.class) || param.getType().equals(Double.class)) {
                schemaBuilder.append("number");
            } else if (param.getType().equals(boolean.class) || param.getType().equals(Boolean.class)) {
                schemaBuilder.append("boolean");
            } else {
                schemaBuilder.append("string"); // 默认为string
            }
            
            // 处理描述，避免产生多余的逗号
            if (toolInput != null && !toolInput.description().isEmpty()) {
                schemaBuilder.append("\", \"description\": \"").append(toolInput.description()).append("\"}");
            } else {
                schemaBuilder.append("\"}");
            }
            
            hasParams = true;
        }
        
        schemaBuilder.append("}, \"required\": [");
        // 添加所有参数到required数组
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                schemaBuilder.append(",");
            }
            schemaBuilder.append("\"" + parameters[i].getName() + "\"");
        }
        schemaBuilder.append("]}");
        
        return schemaBuilder.toString();
    }
    
    @Override
    public ToolCallback[] getToolCallbacks() {
        return toolCallbacks.toArray(new ToolCallback[0]);
    }
    
    /**
     * 添加工具到提供者
     * @param callbacks 要添加的工具回调
     */
    public void addTools(ToolCallback... callbacks) {
        for (ToolCallback callback : callbacks) {
            // 使用拦截器包装工具回调，确保所有工具调用结果都经过分块检查
            // 注意：此处需要在外部进行包装，因为无法访问AutoChunkInterceptor实例
            this.toolCallbacks.add(callback);
        }
    }
    
    /**
     * 获取工具回调的数量
     * @return 工具回调的数量
     */
    public int size() {
        return toolCallbacks.size();
    }
}