package mdl.order_system_test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecutionResponse {
    private String workflowId;
    private String status;
    private Map<String, TaskStatusInfo> tasks;

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
}
