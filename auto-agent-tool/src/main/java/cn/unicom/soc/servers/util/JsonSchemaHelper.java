package cn.unicom.soc.servers.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;

public class JsonSchemaHelper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将params字符串转换为标准JSON Schema
     *
     * @param paramsJsonString 包含参数定义的JSON字符串
     * @return 标准JSON Schema对象
     */
    public static ObjectNode convertParamsToSchema(String paramsJsonString) {
        try {
            // 创建根schema对象
            ObjectNode schema = objectMapper.createObjectNode();
            schema.put("type", "object");

            // 创建properties对象
            ObjectNode properties = objectMapper.createObjectNode();
            schema.set("properties", properties);

            // 创建required数组
            ArrayNode requiredArray = objectMapper.createArrayNode();

            // 解析params JSON字符串
            JsonNode paramsArray = parseJsonString(paramsJsonString);

            // 遍历每个参数
            if (paramsArray != null && paramsArray.isArray()) {
                Iterator<JsonNode> elements = paramsArray.elements();
                while (elements.hasNext()) {
                    JsonNode param = elements.next();

                    // 获取字段名
                    String fieldName = param.has("Field") ? param.get("Field").asText() : null;
                    if (fieldName == null || fieldName.isEmpty()) {
                        continue; // 跳过没有字段名的参数
                    }

                    // 创建属性节点
                    ObjectNode property = objectMapper.createObjectNode();

                    // 设置类型
                    String typeVerbose = param.has("TypeVerbose") ? param.get("TypeVerbose").asText() : "";
                    String methodType = param.has("MethodType") ? param.get("MethodType").asText() : "";
                    String type = mapToSchemaType(typeVerbose, methodType);
                    property.put("type", type);

                    // 设置描述
                    if (param.has("Help")) {
                        property.put("description", param.get("Help").asText());
                    }
//                    // 设置标题（显示名称）
//                    if (param.has("FieldVerbose") && !param.has("Help")) {
//                        property.put("description", param.get("FieldVerbose").asText());
//                    }

                    // 设置默认值
                    if (param.has("DefaultValue")) {
                        // 根据类型处理默认值
                        JsonNode defaultValue = param.get("DefaultValue");
                        if ("boolean".equals(type)) {
                            property.put("default", Boolean.parseBoolean(defaultValue.asText()));
                        } else if ("number".equals(type)) {
                            try {
                                property.put("default", Double.parseDouble(defaultValue.asText()));
                            } catch (NumberFormatException e) {
                                property.put("default", defaultValue.asText());
                            }
                        } else {
                            property.put("default", defaultValue.asText());
                        }
                    }

                    // 处理select类型的额外设置
                    if ("select".equals(typeVerbose) && param.has("ExtraSetting")) {
                        try {
                            String extraSettingStr = param.get("ExtraSetting").asText();
                            JsonNode extraSetting = parseJsonString(extraSettingStr);

                            if (extraSetting != null && extraSetting.has("data")) {
                                // 对于select类型，我们需要提取value字段作为枚举值
                                JsonNode dataArray = extraSetting.get("data");
                                if (dataArray.isArray()) {
                                    ArrayNode enumValues = objectMapper.createArrayNode();
                                    Iterator<JsonNode> dataElements = dataArray.elements();
                                    while (dataElements.hasNext()) {
                                        JsonNode item = dataElements.next();
                                        if (item.has("value")) {
                                            enumValues.add(item.get("value").asText());
                                        }
                                    }
                                    if (enumValues.size() > 0) {
                                        property.set("enum", enumValues);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // 如果ExtraSetting不是有效的JSON，则忽略
                        }
                    }

                    // 添加到properties
                    properties.set(fieldName, property);

                    // 处理required字段
                    boolean required = param.has("Required") && param.get("Required").asBoolean();
                    if (required) {
                        requiredArray.add(fieldName);
                    }
                }
            }

            if(properties.isEmpty()){
                ObjectNode property = objectMapper.createObjectNode();
                property.put("type", "string");
                property.put("description", "输入");
                properties.put("input",property);
            }

            // 只有当有必填字段时才添加required属性
            if (requiredArray.size() > 0) {
                schema.set("required", requiredArray);
            }

            return schema;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert params to JSON schema: " + e.getMessage(), e);
        }
    }

    /**
     * 解析JSON字符串，支持多种格式
     *
     * @param jsonString 可能是JSON字符串或转义的JSON字符串
     * @return 解析后的JsonNode或null（如果解析失败）
     */
    private static JsonNode parseJsonString(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }

        // 尝试直接解析
        try {
            JsonNode node = objectMapper.readTree(jsonString);
            // 如果解析出来的还是字符串，需要再次解析
            if (node.isTextual()) {
                return objectMapper.readTree(node.asText());
            }
            return node;
        } catch (Exception e1) {
            // 解析失败返回null,不要继续解析
            return null;
        }
    }

    /**
     * 将TypeVerbose和MethodType映射为JSON Schema类型
     *
     * @param typeVerbose TypeVerbose字段值
     * @param methodType MethodType字段值
     * @return JSON Schema类型
     */
    private static String mapToSchemaType(String typeVerbose, String methodType) {
        // 优先使用TypeVerbose进行映射
        switch (typeVerbose.toLowerCase()) {
            case "string":
                return "string";
            case "boolean":
                return "boolean";
            case "select":
                return "string"; // select类型在JSON Schema中通常表示为string
            case "number":
            case "integer":
                return "number";
            default:
                // 如果TypeVerbose没有匹配，尝试使用MethodType
                switch (methodType.toLowerCase()) {
                    case "string":
                        return "string";
                    case "boolean":
                        return "boolean";
                    case "integer":
                    case "number":
                        return "number";
                    default:
                        return "string"; // 默认为string类型
                }
        }
    }
}