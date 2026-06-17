package mdl.order_system_test.service;

import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import lombok.extern.slf4j.Slf4j;
import mdl.order_system_test.dto.CreateOrderRequest;
import mdl.order_system_test.dto.OrderResponse;
import mdl.order_system_test.model.AuditLog;
import mdl.order_system_test.model.Order;
import mdl.order_system_test.model.OrderStatus;
import mdl.order_system_test.repository.AuditLogRepository;
import mdl.order_system_test.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;
    private final RestTemplate restTemplate;
    private final String conductorUrl;

    public OrderService(
            OrderRepository orderRepository,
            AuditLogRepository auditLogRepository,
            @Qualifier("conductorRestTemplate") RestTemplate restTemplate,
            @Value("${conductor.server.url}") String conductorUrl) {
        this.orderRepository = orderRepository;
        this.auditLogRepository = auditLogRepository;
        this.restTemplate = restTemplate;
        this.conductorUrl = conductorUrl;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .items(request.getItems())
                .status(OrderStatus.PENDING)
                .build();

        order = orderRepository.save(order);
        log.info("Order {} saved, triggering workflow", order.getOrderId());

        StartWorkflowRequest wfRequest = new StartWorkflowRequest();
        wfRequest.setName("order_fulfillment_saga");
        wfRequest.setVersion(1);
        wfRequest.setCorrelationId(order.getOrderId());
        wfRequest.setInput(buildWorkflowInput(order));

        ResponseEntity<String> wfResponse = restTemplate.postForEntity(
                conductorUrl + "/workflow", wfRequest, String.class);
        String workflowId = wfResponse.getBody();
        log.info("Workflow started: id={}", workflowId);

        order.setWorkflowId(workflowId);
        order = orderRepository.save(order);

        auditLogRepository.save(AuditLog.builder()
                .orderId(order.getOrderId())
                .action("WORKFLOW_STARTED")
                .details("workflowId=" + workflowId)
                .timestamp(LocalDateTime.now())
                .build());

        return toResponse(order);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    public OrderResponse updateStatus(String orderId, OrderStatus status, String reason, String trackingNumber) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        order.setStatus(status);
        order.setFailureReason(reason);
        order.setTrackingNumber(trackingNumber);
        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    public List<AuditLog> getAuditLogs(String orderId) {
        return auditLogRepository.findByOrderIdOrderByTimestampAsc(orderId);
    }

    private Map<String, Object> buildWorkflowInput(Order order) {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", order.getOrderId());
        input.put("customerId", order.getCustomerId());
        input.put("amount", order.getAmount());
        input.put("items", order.getItems());
        return input;
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .amount(order.getAmount())
                .items(order.getItems())
                .status(order.getStatus())
                .workflowId(order.getWorkflowId())
                .failureReason(order.getFailureReason())
                .trackingNumber(order.getTrackingNumber())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
