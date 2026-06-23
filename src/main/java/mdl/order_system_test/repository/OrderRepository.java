package mdl.order_system_test.repository;

import mdl.order_system_test.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends MongoRepository<Order, String> {
    Optional<Order> findByOrderId(String orderId);
    List<Order> findAllByOrderByCreatedAtDesc();
}
