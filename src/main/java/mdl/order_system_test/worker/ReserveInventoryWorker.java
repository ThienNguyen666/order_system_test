package mdl.order_system_test.worker;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mdl.order_system_test.model.OrderItem;
import mdl.order_system_test.service.InventoryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReserveInventoryWorker implements Worker {

    private final InventoryService inventoryService;

    @Override
    public String getTaskDefName() {
        return "reserve_inventory";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) task.getInputData().get("items");

        log.info("[reserve_inventory] orderId={} items={}", orderId, rawItems != null ? rawItems.size() : 0);

        List<OrderItem> items = WorkerUtils.toOrderItems(rawItems);
        String reservationId = inventoryService.reserve(orderId, items);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("inventoryReserved", true);
        result.addOutputData("reservationId", reservationId);
        return result;
    }
}
