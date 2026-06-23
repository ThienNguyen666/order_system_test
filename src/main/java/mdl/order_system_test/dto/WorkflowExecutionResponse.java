package mdl.order_system_test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecutionResponse {
    private String workflowId;
    private String status;
    private Map<String, TaskStatusInfo> tasks;
    private List<HumanTaskInfo> pendingHumanTasks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStatusInfo {
        private String status;
        private String taskType;
        private Long startTime;
        private Long endTime;
        private String reasonForIncompletion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HumanTaskInfo {
        private String taskReferenceName;
        private String displayName;
        private String status;
    }
}
