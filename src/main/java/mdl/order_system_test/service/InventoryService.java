package mdl.order_system_test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mdl.order_system_test.model.AuditLog;
import mdl.order_system_test.model.OrderItem;
import mdl.order_system_test.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final AuditLogRepository auditLogRepository;

    public String reserve(String orderId, List<OrderItem> items) {
        log.info("Reserving inventory for order={} items={}", orderId, items.size());
        String reservationId = UUID.randomUUID().toString();
        auditLogRepository.save(AuditLog.builder()
                .orderId(orderId)
                .action("INVENTORY_RESERVED")
                .details("reservationId=" + reservationId + " itemCount=" + items.size())
                .timestamp(LocalDateTime.now())
                .build());
        return reservationId;
    }

    public void release(String orderId, List<OrderItem> items) {
        log.info("Releasing inventory (compensation) for order={}", orderId);
        auditLogRepository.save(AuditLog.builder()
                .orderId(orderId)
                .action("INVENTORY_RELEASED")
                .details("Compensation: released " + items.size() + " reservations")
                .timestamp(LocalDateTime.now())
                .build());
    }
}
