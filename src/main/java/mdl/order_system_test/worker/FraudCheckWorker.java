package mdl.order_system_test.worker;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mdl.order_system_test.model.AuditLog;
import mdl.order_system_test.repository.AuditLogRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudCheckWorker implements Worker {

    private final AuditLogRepository auditLogRepository;

    @Override
    public String getTaskDefName() {
        return "fraud_check";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId    = (String) task.getInputData().get("orderId");
        String customerId = (String) task.getInputData().get("customerId");
        BigDecimal amount = new BigDecimal(task.getInputData().get("amount").toString());

        log.info("[fraud_check] orderId={} customer={} amount={}", orderId, customerId, amount);

        // Naive rule: orders over $10,000 are flagged high-risk for demo purposes
        boolean highRisk = amount.compareTo(BigDecimal.valueOf(10_000)) > 0;
        String riskLevel = highRisk ? "HIGH" : "LOW";

        auditLogRepository.save(AuditLog.builder()
                .orderId(orderId)
                .action("FRAUD_CHECK")
                .details("riskLevel=" + riskLevel + " amount=" + amount)
                .timestamp(LocalDateTime.now())
                .build());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("fraudCleared", !highRisk);
        result.addOutputData("riskLevel", riskLevel);
        return result;
    }
}
