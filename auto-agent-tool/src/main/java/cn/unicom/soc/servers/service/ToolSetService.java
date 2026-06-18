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
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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