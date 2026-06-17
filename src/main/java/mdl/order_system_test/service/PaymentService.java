package mdl.order_system_test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mdl.order_system_test.model.AuditLog;
import mdl.order_system_test.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final AuditLogRepository auditLogRepository;

    public String authorize(String orderId, String customerId, BigDecimal amount) {
        log.info("Authorizing payment order={} customer={} amount={}", orderId, customerId, amount);
        String authCode = UUID.randomUUID().toString();
        auditLogRepository.save(AuditLog.builder()
                .orderId(orderId)
                .action("PAYMENT_AUTHORIZED")
                .details("authCode=" + authCode + " amount=" + amount)
                .timestamp(LocalDateTime.now())
                .build());
        return authCode;
    }

    public void voidAuthorization(String orderId) {
        log.info("Voiding payment authorization (compensation) for order={}", orderId);
        auditLogRepository.save(AuditLog.builder()
                .orderId(orderId)
                .action("PAYMENT_VOIDED")
                .details("Compensation: authorization voided")
                .timestamp(LocalDateTime.now())
                .build());
    }
}
