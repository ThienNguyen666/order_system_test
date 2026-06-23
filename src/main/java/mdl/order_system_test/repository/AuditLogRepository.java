package mdl.order_system_test.repository;

import mdl.order_system_test.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findByOrderIdOrderByTimestampAsc(String orderId);
}
