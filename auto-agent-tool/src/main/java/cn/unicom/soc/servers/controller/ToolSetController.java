package cn.unicom.soc.servers.controller;

import cn.unicom.soc.servers.dto.HttpToolConfigDto;
import cn.unicom.soc.servers.dto.ToolDto;
import cn.unicom.soc.servers.dto.ToolSetDto;
import cn.unicom.soc.servers.service.ToolSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tool-sets")
@RequiredArgsConstructor
public class ToolSetController {
    
    @Autowired
    private final ToolSetService toolSetService;
    
    /**
     * 获取所有工具集
     */
    @GetMapping
    public ResponseEntity<List<ToolSetDto>> getAllToolSets() {
        List<ToolSetDto> toolSets = toolSetService.getAllToolSets();
        return ResponseEntity.ok(toolSets);
    }
    
    /**
     * 根据ID获取工具集
     */
    @GetMapping("/{id}")
    public ResponseEntity<ToolSetDto> getToolSetById(@PathVariable String id) {
        ToolSetDto toolSet = toolSetService.getToolSetById(id);
        return ResponseEntity.ok(toolSet);
    }
    
    /**
     * 根据类型获取工具集
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<ToolSetDto>> getToolSetsByType(@PathVariable String type) {
        List<ToolSetDto> toolSets = toolSetService.getToolSetsByType(type);
        return ResponseEntity.ok(toolSets);
    }
    
    /**
     * 根据状态获取工具集
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ToolSetDto>> getToolSetsByStatus(@PathVariable Integer status) {
        List<ToolSetDto> toolSets = toolSetService.getToolSetsByStatus(status);
        return ResponseEntity.ok(toolSets);
    }
    
    /**
     * 创建工具集
     */
    @PostMapping
    public ResponseEntity<ToolSetDto> createToolSet(@RequestBody ToolSetDto toolSetDto) {
        ToolSetDto createdToolSet = toolSetService.createToolSet(toolSetDto);
        return ResponseEntity.ok(createdToolSet);
    }
    
    /**
     * 更新工具集
     */
    @PutMapping("/{id}")
    public ResponseEntity<ToolSetDto> updateToolSet(@PathVariable String id, @RequestBody ToolSetDto toolSetDto) {
        ToolSetDto updatedToolSet = toolSetService.updateToolSet(id, toolSetDto);
        return ResponseEntity.ok(updatedToolSet);
    }
    
    /**
     * 删除工具集
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteToolSet(@PathVariable String id) {
        toolSetService.deleteToolSet(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 启用/禁用工具集
     */
    @PatchMapping("/{id}/status/{status}")
    public ResponseEntity<ToolSetDto> toggleToolSetStatus(@PathVariable String id, @PathVariable Integer status) {
        ToolSetDto updatedToolSet = toolSetService.toggleToolSetStatus(id, status);
        return ResponseEntity.ok(updatedToolSet);
    }
    
    /**
     * 添加工具到工具集
     */
    @PostMapping("/{toolSetId}/tools")
    public ResponseEntity<ToolDto> addToolToSet(@PathVariable String toolSetId, @RequestBody ToolDto toolDto) {
        ToolDto addedTool = toolSetService.addToolToSet(toolSetId, toolDto);
        return ResponseEntity.ok(addedTool);
    }
    
    /**
     * 添加HTTP工具到工具集
     */
    @PostMapping("/{toolSetId}/http-tools")
    public ResponseEntity<HttpToolConfigDto> addHttpToolToSet(@PathVariable String toolSetId, @RequestBody HttpToolConfigDto httpToolDto) {
        HttpToolConfigDto addedHttpTool = toolSetService.addHttpToolToSet(toolSetId, httpToolDto);
        return ResponseEntity.ok(addedHttpTool);
    }

    /**
     * 根据ID获取HTTP工具
     */
    @GetMapping("/{toolSetId}/http-tools/{id}")
    public ResponseEntity<HttpToolConfigDto> getHttpToolById(@PathVariable String toolSetId, @PathVariable String id) {
        HttpToolConfigDto httpTool = toolSetService.getHttpToolById(id);
        return ResponseEntity.ok(httpTool);
    }

    /**
     * 更新HTTP工具
     */
    @PutMapping("/{toolSetId}/http-tools/{id}")
    public ResponseEntity<HttpToolConfigDto> updateHttpTool(@PathVariable String toolSetId, @PathVariable String id, @RequestBody HttpToolConfigDto httpToolDto) {
        HttpToolConfigDto updatedHttpTool = toolSetService.updateHttpTool(id, httpToolDto);
        return ResponseEntity.ok(updatedHttpTool);
    }

    /**
     * 删除HTTP工具
     */
    @DeleteMapping("/{toolSetId}/http-tools/{id}")
    public ResponseEntity<Void> deleteHttpTool(@PathVariable String toolSetId, @PathVariable String id) {
        toolSetService.deleteHttpTool(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 启用/禁用HTTP工具
     */
    @PatchMapping("/{toolSetId}/http-tools/{id}/status/{status}")
    public ResponseEntity<HttpToolConfigDto> toggleHttpToolStatus(@PathVariable String toolSetId, @PathVariable String id, @PathVariable Integer status) {
        HttpToolConfigDto updatedHttpTool = toolSetService.toggleHttpToolStatus(id, status);
        return ResponseEntity.ok(updatedHttpTool);
    }

    /**
     * 批量上传 Skill 目录（zip 文件）
     */
    @PostMapping("/{toolSetId}/skills/upload")
    public ResponseEntity<Map<String, Object>> uploadSkillDirectories(
            @PathVariable String toolSetId,
            @RequestParam("file") MultipartFile zipFile) {
        try {
            List<ToolDto> tools = toolSetService.uploadSkillDirectories(toolSetId, zipFile);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "成功导入 " + tools.size() + " 个 Skill");
            result.put("tools", tools);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "文件处理失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}