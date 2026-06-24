package mdl.order_system_test.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mdl.order_system_test.dto.WorkflowExecutionResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class HumanTaskQueueService {

    public static final Map<String, String> HUMAN_TASK_LABELS = Map.of(
            "intake_approval_ref", "Intake Approval",
            "risk_approval_ref", "Risk Approval",
            "compliance_approval_ref", "Compliance Approval",
            "payment_recovery_ref", "Payment Recovery",
            "manual_approval_ref", "Manual Approval"
    );

    private static final Set<String> WAITING_STATUSES = Set.of("SCHEDULED", "IN_PROGRESS");
    private static final Duration QUEUE_TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public boolean isAllowedHumanTask(String taskReferenceName) {
        return HUMAN_TASK_LABELS.containsKey(taskReferenceName);
    }

    public List<WorkflowExecutionResponse.HumanTaskInfo> syncPendingTasks(
            String orderId,
            Map<String, WorkflowExecutionResponse.TaskStatusInfo> tasks) {
        List<WorkflowExecutionResponse.HumanTaskInfo> pending = buildPendingTasks(tasks);
        try {
            String key = queueKey(orderId);
            redisTemplate.delete(key);
            if (!pending.isEmpty()) {
                Map<String, String> values = new LinkedHashMap<>();
                for (WorkflowExecutionResponse.HumanTaskInfo task : pending) {
                    values.put(task.getTaskReferenceName(), objectMapper.writeValueAsString(task));
                }
                redisTemplate.opsForHash().putAll(key, values);
                redisTemplate.expire(key, QUEUE_TTL);
            }
        } catch (Exception e) {
            log.warn("Redis human task queue sync skipped for order {}: {}", orderId, e.getMessage());
        }
        return pending;
    }

    public void markCompleted(String orderId, String taskReferenceName) {
        try {
            redisTemplate.opsForHash().delete(queueKey(orderId), taskReferenceName);
        } catch (Exception e) {
            log.warn("Redis human task queue completion skipped for order {} task {}: {}",
                    orderId, taskReferenceName, e.getMessage());
        }
    }

    private List<WorkflowExecutionResponse.HumanTaskInfo> buildPendingTasks(
            Map<String, WorkflowExecutionResponse.TaskStatusInfo> tasks) {
        List<WorkflowExecutionResponse.HumanTaskInfo> pending = new ArrayList<>();
        for (Map.Entry<String, WorkflowExecutionResponse.TaskStatusInfo> entry : tasks.entrySet()) {
            String taskRef = entry.getKey();
            WorkflowExecutionResponse.TaskStatusInfo task = entry.getValue();
            if (!isAllowedHumanTask(taskRef)
                    || !"HUMAN".equals(task.getTaskType())
                    || !WAITING_STATUSES.contains(task.getStatus())) {
                continue;
            }
            pending.add(WorkflowExecutionResponse.HumanTaskInfo.builder()
                    .taskReferenceName(taskRef)
                    .displayName(HUMAN_TASK_LABELS.get(taskRef))
                    .status(task.getStatus())
                    .build());
        }
        return pending;
    }

    private String queueKey(String orderId) {
        return "human-task-queue:" + orderId;
    }
}
