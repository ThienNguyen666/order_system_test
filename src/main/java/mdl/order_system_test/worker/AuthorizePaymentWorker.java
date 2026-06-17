package mdl.order_system_test.worker;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mdl.order_system_test.service.ChaosToggleService;
import mdl.order_system_test.service.PaymentService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizePaymentWorker implements Worker {

    private final PaymentService paymentService;
    private final ChaosToggleService chaosToggleService;

    @Override
    public String getTaskDefName() {
        return "authorize_payment";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId    = (String) task.getInputData().get("orderId");
        String customerId = (String) task.getInputData().get("customerId");
        BigDecimal amount = new BigDecimal(task.getInputData().get("amount").toString());

        log.info("[authorize_payment] orderId={} amount={} chaos={}", orderId, amount, chaosToggleService.isChaosEnabled());

        // ── Chaos Mode ───────────────────────────────────────────────────────
        // Roll once; two outcomes to exercise both Conductor mechanics:
        //   < 0.4  → throws exception  → Conductor retries the task (timeout/retry demo)
        //   >= 0.4 → returns false     → SWITCH routes to compensation branch (saga demo)
        if (chaosToggleService.isChaosEnabled()) {
            double roll = Math.random();
            if (roll < 0.4) {
                log.warn("[CHAOS] Simulating gateway timeout for order={} — Conductor will retry", orderId);
                throw new RuntimeException("Payment gateway timeout (chaos mode)");
            }
            log.warn("[CHAOS] Payment declined for order={} — compensation branch will run", orderId);
            TaskResult declined = new TaskResult(task);
            declined.setStatus(TaskResult.Status.COMPLETED);
            declined.addOutputData("paymentAuthorized", false);
            declined.addOutputData("reason", "Payment declined by issuer (chaos mode)");
            return declined;
        }
        // ─────────────────────────────────────────────────────────────────────

        String authCode = paymentService.authorize(orderId, customerId, amount);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("paymentAuthorized", true);
        result.addOutputData("authCode", authCode);
        return result;
    }
}
