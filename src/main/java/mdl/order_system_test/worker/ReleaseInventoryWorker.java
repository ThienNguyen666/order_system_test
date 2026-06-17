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
public class ReleaseInventoryWorker implements Worker {

    private final InventoryService inventoryService;

    @Override
    public String getTaskDefName() {
        return "release_inventory";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) task.getInputData().get("items");

        log.info("[release_inventory] COMPENSATION orderId={}", orderId);

        List<OrderItem> items = WorkerUtils.toOrderItems(rawItems);
        inventoryService.release(orderId, items);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("inventoryReleased", true);
        return result;
    }
}
