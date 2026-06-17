package mdl.order_system_test.worker;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mdl.order_system_test.model.AuditLog;
import mdl.order_system_test.repository.AuditLogRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateShipmentWorker implements Worker {

    private final AuditLogRepository auditLogRepository;

    @Override
    public String getTaskDefName() {
        return "create_shipment";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId    = (String) task.getInputData().get("orderId");
        String customerId = (String) task.getInputData().get("customerId");

        log.info("[create_shipment] orderId={} customer={}", orderId, customerId);

        String trackingNumber = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        auditLogRepository.save(AuditLog.builder()
                .orderId(orderId)
                .action("SHIPMENT_CREATED")
                .details("trackingNumber=" + trackingNumber)
                .timestamp(LocalDateTime.now())
                .build());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("shipmentCreated", true);
        result.addOutputData("trackingNumber", trackingNumber);
        return result;
    }
}
