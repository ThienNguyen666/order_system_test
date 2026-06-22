package mdl.order_system_test.repository;

import mdl.order_system_test.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findByOrderIdOrderByTimestampAsc(String orderId);
}
