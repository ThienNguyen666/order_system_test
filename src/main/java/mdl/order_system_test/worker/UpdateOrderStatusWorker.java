package mdl.order_system_test.worker;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mdl.order_system_test.model.OrderStatus;
import mdl.order_system_test.service.OrderService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateOrderStatusWorker implements Worker {

    private final OrderService orderService;

    @Override
    public String getTaskDefName() {
        return "update_order_status";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId        = (String) task.getInputData().get("orderId");
        String statusStr      = (String) task.getInputData().get("status");
        String reason         = (String) task.getInputData().getOrDefault("reason", null);
        String trackingNumber = (String) task.getInputData().getOrDefault("trackingNumber", null);

        OrderStatus status = OrderStatus.valueOf(statusStr);
        log.info("[update_order_status] orderId={} status={}", orderId, status);

        orderService.updateStatus(orderId, status, reason, trackingNumber);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("updated", true);
        result.addOutputData("status", statusStr);
        return result;
    }
}
