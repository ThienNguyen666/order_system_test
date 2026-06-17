package mdl.order_system_test.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ChaosToggleService {

    private final AtomicBoolean chaosEnabled = new AtomicBoolean(false);

    public boolean isChaosEnabled() {
        return chaosEnabled.get();
    }

    public boolean toggle() {
        boolean current;
        boolean next;
        do {
            current = chaosEnabled.get();
            next = !current;
        } while (!chaosEnabled.compareAndSet(current, next));
        return next;
    }

    public void set(boolean enabled) {
        chaosEnabled.set(enabled);
    }
}
