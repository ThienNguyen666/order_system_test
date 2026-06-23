package mdl.order_system_test.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ConductorMetadataService {

    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<>() {};

    private final RestTemplate restTemplate;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Value("${conductor.server.url}")
    private String conductorUrl;

    public ConductorMetadataService(
            @Qualifier("conductorRestTemplate") RestTemplate restTemplate,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerMetadata() {
        try {
            registerTaskDefinitions();
            registerWorkflowDefinitions();
        } catch (Exception e) {
            log.warn("Conductor metadata registration skipped: {}", e.getMessage());
        }
    }

    private void registerTaskDefinitions() throws Exception {
        List<Map<String, Object>> taskDefs = readList("classpath:conductor-taskdefs.json");
        for (Map<String, Object> taskDef : taskDefs) {
            try {
                restTemplate.put(conductorUrl + "/metadata/taskdefs", taskDef);
            } catch (HttpStatusCodeException e) {
                restTemplate.postForEntity(conductorUrl + "/metadata/taskdefs", List.of(taskDef), String.class);
            }
        }
        log.info("Registered {} Conductor task definitions", taskDefs.size());
    }

    private void registerWorkflowDefinitions() throws Exception {
        List<Map<String, Object>> workflowDefs = readList("classpath:conductor-workflow.json");
        restTemplate.put(conductorUrl + "/metadata/workflow", workflowDefs);
        log.info("Registered {} Conductor workflow definitions", workflowDefs.size());
    }

    private List<Map<String, Object>> readList(String location) throws Exception {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, LIST_OF_MAPS);
        }
    }
}
