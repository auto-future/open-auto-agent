package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.dto.HttpToolConfigDto;
import cn.unicom.soc.servers.dto.ToolDto;
import cn.unicom.soc.servers.dto.ToolSetDto;
import cn.unicom.soc.servers.entity.HttpToolConfigEntity;
import cn.unicom.soc.servers.entity.ToolEntity;
import cn.unicom.soc.servers.entity.ToolSetEntity;
import cn.unicom.soc.servers.repository.HttpToolConfigRepository;
import cn.unicom.soc.servers.repository.ToolRepository;
import cn.unicom.soc.servers.repository.ToolSetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ToolSetService {

    @Autowired
    private ToolSetRepository toolSetRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private HttpToolConfigRepository httpToolConfigRepository;

    /**
     * 创建工具集
     */
    @Transactional
    public ToolSetDto createToolSet(ToolSetDto toolSetDto) {
        ToolSetEntity toolSetEntity = new ToolSetEntity();
        BeanUtils.copyProperties(toolSetDto, toolSetEntity);

        ToolSetEntity savedEntity = toolSetRepository.save(toolSetEntity);
        return convertToDto(savedEntity);
    }

    /**
     * 更新工具集
     */
    @Transactional
    public ToolSetDto updateToolSet(String id, ToolSetDto toolSetDto) {
        ToolSetEntity existingEntity = toolSetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ToolSet not found with id: " + id));

        BeanUtils.copyProperties(toolSetDto, existingEntity, "id", "createdAt");
        existingEntity.setId(id); // 确保ID不变

        ToolSetEntity updatedEntity = toolSetRepository.save(existingEntity);
        return convertToDto(updatedEntity);
    }

    /**
     * 删除工具集
     */
    @Transactional
    public void deleteToolSet(String id) {
        ToolSetEntity toolSetEntity = toolSetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ToolSet not found with id: " + id));

        // 删除关联的工具
        List<ToolEntity> tools = toolRepository.findByToolSetId(id);
        toolRepository.deleteAll(tools);

        // 删除关联的HTTP工具
        List<HttpToolConfigEntity> httpTools = httpToolConfigRepository.findByToolSetId(id);
        httpToolConfigRepository.deleteAll(httpTools);

        // 删除工具集本身
        toolSetRepository.delete(toolSetEntity);
    }

    /**
     * 获取所有工具集
     */
    @Transactional(readOnly = true)
    public List<ToolSetDto> getAllToolSets() {
        return toolSetRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取工具集
     */
    @Transactional(readOnly = true)
    public ToolSetDto getToolSetById(String id) {
        ToolSetEntity toolSetEntity = toolSetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ToolSet not found with id: " + id));

        return convertToDto(toolSetEntity);
    }

    /**
     * 根据类型获取工具集
     */
    @Transactional(readOnly = true)
    public List<ToolSetDto> getToolSetsByType(String type) {
        return toolSetRepository.findByType(type).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 根据状态获取工具集
     */
    @Transactional(readOnly = true)
    public List<ToolSetDto> getToolSetsByStatus(Integer status) {
        return toolSetRepository.findByStatus(status).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 启用/禁用工具集
     */
    @Transactional
    public ToolSetDto toggleToolSetStatus(String id, Integer status) {
        ToolSetEntity toolSetEntity = toolSetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ToolSet not found with id: " + id));

        toolSetEntity.setStatus(status);
        ToolSetEntity updatedEntity = toolSetRepository.save(toolSetEntity);

        return convertToDto(updatedEntity);
    }

    /**
     * 添加工具到工具集
     */
    @Transactional
    public ToolDto addToolToSet(String toolSetId, ToolDto toolDto) {
        ToolSetEntity toolSet = toolSetRepository.findById(toolSetId)
                .orElseThrow(() -> new EntityNotFoundException("ToolSet not found with id: " + toolSetId));

        ToolEntity toolEntity = new ToolEntity();
        BeanUtils.copyProperties(toolDto, toolEntity);
        toolEntity.setToolSetId(toolSetId);

        ToolEntity savedTool = toolRepository.save(toolEntity);
        return convertToolToDto(savedTool);
    }

    /**
     * 添加HTTP工具到工具集
     */
    @Transactional
    public HttpToolConfigDto addHttpToolToSet(String toolSetId, HttpToolConfigDto httpToolDto) {
        ToolSetEntity toolSet = toolSetRepository.findById(toolSetId)
                .orElseThrow(() -> new EntityNotFoundException("ToolSet not found with id: " + toolSetId));

        HttpToolConfigEntity httpToolEntity = new HttpToolConfigEntity();
        BeanUtils.copyProperties(httpToolDto, httpToolEntity);
        httpToolEntity.setToolSetId(toolSetId);

        HttpToolConfigEntity savedHttpTool = httpToolConfigRepository.save(httpToolEntity);
        return convertHttpToolToDto(savedHttpTool);
    }

    /**
     * 获取所有工具
     */
    @Transactional(readOnly = true)
    public List<ToolDto> getAllTools() {
        return toolRepository.findAll().stream()
                .map(this::convertToolToDto)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取工具
     */
    @Transactional(readOnly = true)
    public ToolDto getToolById(String id) {
        ToolEntity toolEntity = toolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tool not found with id: " + id));

        return convertToolToDto(toolEntity);
    }

    /**
     * 根据工具集ID获取工具
     */
    @Transactional(readOnly = true)
    public List<ToolDto> getToolsBySetId(String toolSetId) {
        return toolRepository.findByToolSetId(toolSetId).stream()
                .map(this::convertToolToDto)
                .collect(Collectors.toList());
    }

    /**
     * 根据类型获取工具
     */
    @Transactional(readOnly = true)
    public List<ToolDto> getToolsByType(String type) {
        return toolRepository.findByType(type).stream()
                .map(this::convertToolToDto)
                .collect(Collectors.toList());
    }

    /**
     * 分页条件查询工具
     */
    @Transactional(readOnly = true)
    public Page<ToolDto> getToolsByPage(String type, String toolSetId, String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ToolEntity> toolPage = toolRepository.findByConditions(type, toolSetId, name, pageable);
        List<ToolDto> toolDtos = toolPage.getContent().stream()
                .map(this::convertToolToDto)
                .collect(Collectors.toList());
        return new PageImpl<>(toolDtos, pageable, toolPage.getTotalElements());
    }

    /**
     * 更新工具
     */
    @Transactional
    public ToolDto updateTool(String id, ToolDto toolDto) {
        ToolEntity existingEntity = toolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tool not found with id: " + id));

        // 只更新允许修改的字段，保留原有值
        if (toolDto.getName() != null) {
            existingEntity.setName(toolDto.getName());
        }
        if (toolDto.getDescription() != null) {
            existingEntity.setDescription(toolDto.getDescription());
        }
        if (toolDto.getType() != null) {
            existingEntity.setType(toolDto.getType());
        }
        if (toolDto.getInputSchema() != null) {
            existingEntity.setInputSchema(toolDto.getInputSchema());
        }
        if (toolDto.getStatus() != null) {
            existingEntity.setStatus(toolDto.getStatus());
        }

        ToolEntity updatedEntity = toolRepository.save(existingEntity);
        return convertToolToDto(updatedEntity);
    }

    /**
     * 删除工具
     */
    @Transactional
    public void deleteTool(String id) {
        ToolEntity toolEntity = toolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tool not found with id: " + id));

        toolRepository.delete(toolEntity);
    }

    /**
     * 启用/禁用工具
     */
    @Transactional
    public ToolDto toggleToolStatus(String id, Integer status) {
        ToolEntity toolEntity = toolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tool not found with id: " + id));

        toolEntity.setStatus(status);
        ToolEntity updatedEntity = toolRepository.save(toolEntity);

        return convertToolToDto(updatedEntity);
    }

    /**
     * 根据ID获取HTTP工具
     */
    @Transactional(readOnly = true)
    public HttpToolConfigDto getHttpToolById(String id) {
        HttpToolConfigEntity entity = httpToolConfigRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("HTTP Tool not found with id: " + id));
        return convertHttpToolToDto(entity);
    }

    /**
     * 更新HTTP工具
     */
    @Transactional
    public HttpToolConfigDto updateHttpTool(String id, HttpToolConfigDto httpToolDto) {
        HttpToolConfigEntity existingEntity = httpToolConfigRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("HTTP Tool not found with id: " + id));

        if (httpToolDto.getName() != null) {
            existingEntity.setName(httpToolDto.getName());
        }
        if (httpToolDto.getDescription() != null) {
            existingEntity.setDescription(httpToolDto.getDescription());
        }
        if (httpToolDto.getMethod() != null) {
            existingEntity.setMethod(httpToolDto.getMethod());
        }
        if (httpToolDto.getUrl() != null) {
            existingEntity.setUrl(httpToolDto.getUrl());
        }
        if (httpToolDto.getHeaders() != null) {
            existingEntity.setHeaders(httpToolDto.getHeaders());
        }
        if (httpToolDto.getRequestBodyTemplate() != null) {
            existingEntity.setRequestBodyTemplate(httpToolDto.getRequestBodyTemplate());
        }
        if (httpToolDto.getResponseParsingPattern() != null) {
            existingEntity.setResponseParsingPattern(httpToolDto.getResponseParsingPattern());
        }
        if (httpToolDto.getStatus() != null) {
            existingEntity.setStatus(httpToolDto.getStatus());
        }

        HttpToolConfigEntity updatedEntity = httpToolConfigRepository.save(existingEntity);
        return convertHttpToolToDto(updatedEntity);
    }

    /**
     * 删除HTTP工具
     */
    @Transactional
    public void deleteHttpTool(String id) {
        HttpToolConfigEntity entity = httpToolConfigRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("HTTP Tool not found with id: " + id));
        httpToolConfigRepository.delete(entity);
    }

    /**
     * 启用/禁用HTTP工具
     */
    @Transactional
    public HttpToolConfigDto toggleHttpToolStatus(String id, Integer status) {
        HttpToolConfigEntity entity = httpToolConfigRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("HTTP Tool not found with id: " + id));
        entity.setStatus(status);
        HttpToolConfigEntity updatedEntity = httpToolConfigRepository.save(entity);
        return convertHttpToolToDto(updatedEntity);
    }

    /**
     * 批量上传 Skill 目录（zip 文件）
     * zip 中每个一级子目录视为一个 skill，目录中需包含 skill.json 元数据文件
     */
    @Transactional
    public List<ToolDto> uploadSkillDirectories(String toolSetId, MultipartFile zipFile) throws IOException {
        ToolSetEntity toolSet = toolSetRepository.findById(toolSetId)
                .orElseThrow(() -> new EntityNotFoundException("ToolSet not found with id: " + toolSetId));

        if (!"skills".equals(toolSet.getTag())) {
            throw new IllegalArgumentException("Only skills type toolset supports directory upload");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List<ToolDto> result = new ArrayList<>();

        // 基础存储路径
        Path basePath = Paths.get("data", "skills", toolSetId);
        Files.createDirectories(basePath);

        // 解压到临时目录
        Path tempDir = Files.createTempDirectory("skills-upload-");
        try {
            // 解压 zip
            try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path entryPath = tempDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }

            // 遍历临时目录下的一级子目录（每个子目录是一个 skill）
            try (Stream<Path> dirs = Files.list(tempDir)) {
                List<Path> skillDirs = dirs.filter(Files::isDirectory).collect(Collectors.toList());

                for (Path skillDir : skillDirs) {
                    Path skillJsonPath = skillDir.resolve("skill.json");
                    if (!Files.exists(skillJsonPath)) {
                        continue; // 跳过没有 skill.json 的目录
                    }

                    // 解析 skill.json
                    String skillJsonContent = Files.readString(skillJsonPath, StandardCharsets.UTF_8);
                    JsonNode skillNode = objectMapper.readTree(skillJsonContent);

                    String name = skillNode.has("name") ? skillNode.get("name").asText() : skillDir.getFileName().toString();
                    String description = skillNode.has("description") ? skillNode.get("description").asText() : "";
                    String inputSchema = skillNode.has("inputSchema") ? skillNode.get("inputSchema").toString() : "{}";
                    String entryFile = skillNode.has("entry") ? skillNode.get("entry").asText() : "";

                    // 生成 skill ID 并复制到最终目录
                    String skillId = UUID.randomUUID().toString();
                    Path targetDir = basePath.resolve(skillId);
                    copyDirectory(skillDir, targetDir);

                    // 读取入口脚本内容
                    String scriptContent = "";
                    if (!entryFile.isEmpty()) {
                        Path entryPath = targetDir.resolve(entryFile);
                        if (Files.exists(entryPath)) {
                            scriptContent = Files.readString(entryPath, StandardCharsets.UTF_8);
                        }
                    }

                    // 创建 ToolEntity
                    ToolEntity toolEntity = new ToolEntity();
                    toolEntity.setName(name);
                    toolEntity.setDescription(description);
                    toolEntity.setToolSetId(toolSetId);
                    toolEntity.setType("skills");
                    toolEntity.setInputSchema(inputSchema);
                    toolEntity.setResourcePath(targetDir.toString());
                    toolEntity.setScriptContent(scriptContent);
                    toolEntity.setStatus(1);

                    ToolEntity savedTool = toolRepository.save(toolEntity);
                    result.add(convertToolToDto(savedTool));
                }
            }
        } finally {
            // 清理临时目录
            deleteDirectory(tempDir);
        }

        return result;
    }

    /**
     * 复制目录
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(srcPath -> {
            try {
                Path relative = source.relativize(srcPath);
                Path destPath = target.resolve(relative);
                if (Files.isDirectory(srcPath)) {
                    Files.createDirectories(destPath);
                } else {
                    Files.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(path -> {
            try {
                Files.delete(path);
            } catch (IOException e) {
                // 忽略删除失败
            }
        });
    }

    /**
     * 将实体转换为DTO
     */
    private ToolSetDto convertToDto(ToolSetEntity entity) {
        ToolSetDto dto = new ToolSetDto();
        BeanUtils.copyProperties(entity, dto);

        // 获取关联的工具
        List<ToolEntity> tools = toolRepository.findByToolSetId(entity.getId());
        List<ToolDto> toolDtos = tools.stream()
                .map(this::convertToolToDto)
                .collect(Collectors.toList());
        dto.setTools(toolDtos);

        // 获取关联的HTTP工具
        List<HttpToolConfigEntity> httpTools = httpToolConfigRepository.findByToolSetId(entity.getId());
        List<HttpToolConfigDto> httpToolDtos = httpTools.stream()
                .map(this::convertHttpToolToDto)
                .collect(Collectors.toList());
        dto.setHttpTools(httpToolDtos);

        return dto;
    }

    /**
     * 将工具实体转换为DTO
     */
    private ToolDto convertToolToDto(ToolEntity entity) {
        ToolDto dto = new ToolDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    /**
     * 将HTTP工具实体转换为DTO
     */
    private HttpToolConfigDto convertHttpToolToDto(HttpToolConfigEntity entity) {
        HttpToolConfigDto dto = new HttpToolConfigDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}