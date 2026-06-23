package mdl.order_system_test.worker;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mdl.order_system_test.service.PaymentService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoidPaymentWorker implements Worker {

    private final PaymentService paymentService;

    @Override
    public String getTaskDefName() {
        return "void_payment_authorization";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");

        log.info("[{}] COMPENSATION orderId={}", task.getTaskDefName(), orderId);

        paymentService.voidAuthorization(orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("paymentVoided", true);
        return result;
    }
}
