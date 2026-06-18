package cn.unicom.soc.servers.controller;

import cn.unicom.soc.servers.entity.HttpToolConfig;
import cn.unicom.soc.servers.service.HttpApiManagerService;
import cn.unicom.soc.servers.util.SqliteDBManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 工具管理控制器
 * 提供 REST API 接口来管理 HTTP 工具
 */
@RestController
@RequestMapping("/api/http-tools")
@CrossOrigin(origins = "*") // 生产环境中应限制特定域名
public class HttpToolController {

    @Autowired
    private HttpApiManagerService httpApiManagerService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    /**
     * 添加或更新单个 HTTP 工具
     */
    @PostMapping
    public ResponseEntity<String> addOrUpdateHttpTool(@RequestBody HttpToolConfig config) {
        try {
            // 将对象转换为 Map 以适应服务方法
            Map<String, Object> params = new HashMap<>();
            params.put("toolConfig", objectMapper.convertValue(config, Map.class));
            String result = httpApiManagerService.addOrUpdateHttpApi(params);
            
            Map<String, Object> resultMap = objectMapper.readValue(result, Map.class);
            if ((Boolean) resultMap.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            
            try {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(objectMapper.writeValueAsString(errorResult));
            } catch (JsonProcessingException jsonException) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"success\": false, \"error\": \"Internal error occurred\"}");
            }
        }
    }

    /**
     * 批量添加或更新 HTTP 工具
     */
    @PostMapping("/batch")
    public ResponseEntity<String> batchAddHttpTools(@RequestBody List<HttpToolConfig> configs) {
        try {
            // 构建包含工具数组的Map对象
            Map<String, Object> toolsList = new HashMap<>();
            List<Map<String, Object>> tools = new ArrayList<>();
            for (HttpToolConfig config : configs) {
                tools.add(objectMapper.convertValue(config, Map.class));
            }
            toolsList.put("tools", tools);
            
            Map<String, Object> params = new HashMap<>();
            params.put("toolsConfig", toolsList);
            
            String result = httpApiManagerService.batchAddHttpApis(params);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            
            try {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(objectMapper.writeValueAsString(errorResult));
            } catch (JsonProcessingException jsonException) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"success\": false, \"error\": \"Internal error occurred\"}");
            }
        }
    }
    
    @PutMapping("/batch")
    public ResponseEntity<String> batchUpdateHttpTools(@RequestBody List<HttpToolConfig> configs) {
        try {
            // 构建包含工具数组的Map对象
            Map<String, Object> toolsList = new HashMap<>();
            List<Map<String, Object>> tools = new ArrayList<>();
            for (HttpToolConfig config : configs) {
                tools.add(objectMapper.convertValue(config, Map.class));
            }
            toolsList.put("tools", tools);
            
            Map<String, Object> params = new HashMap<>();
            params.put("toolsConfig", toolsList);
            
            String result = httpApiManagerService.batchUpdateHttpApis(params);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            
            try {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(objectMapper.writeValueAsString(errorResult));
            } catch (JsonProcessingException jsonException) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"success\": false, \"error\": \"Internal error occurred\"}");
            }
        }
    }

    /**
     * 根据工具名称删除 HTTP 工具
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteHttpTool(@PathVariable int id) {
        try {
            // 将ID转换为Map参数
            Map<String, Object> params = new HashMap<>();
            params.put("id", String.valueOf(id));
            
            String result = httpApiManagerService.deleteHttpApi(params);
            
            Map<String, Object> resultMap = objectMapper.readValue(result, Map.class);
            if ((Boolean) resultMap.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            
            try {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(objectMapper.writeValueAsString(errorResult));
            } catch (JsonProcessingException jsonException) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"success\": false, \"error\": \"Internal error occurred\"}");
            }
        }
    }

    /**
     * 获取所有 HTTP 工具列表
     */
    @GetMapping
    public ResponseEntity<String> listHttpTools() {
        try {
            Map<String, Object> params = new HashMap<>();
            String result = httpApiManagerService.listHttpApis(params);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            
            try {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(objectMapper.writeValueAsString(errorResult));
            } catch (JsonProcessingException jsonException) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"success\": false, \"error\": \"Internal error occurred\"}");
            }
        }
    }

    /**
     * 根据 ID 获取单个 HTTP 工具
     */
    @GetMapping("/{id}")
    public ResponseEntity<String> getHttpToolById(@PathVariable int id) {
        try {
            HttpToolConfig config = SqliteDBManager.findById(id);
            if (config != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("tool", config);
                return ResponseEntity.ok(objectMapper.writeValueAsString(result));
            } else {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", "Tool not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(objectMapper.writeValueAsString(errorResult));
            }
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            
            try {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(objectMapper.writeValueAsString(errorResult));
            } catch (JsonProcessingException jsonException) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"success\": false, \"error\": \"Internal error occurred\"}");
            }
        }
    }
}