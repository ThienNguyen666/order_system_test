package mdl.order_system_test.worker;

import mdl.order_system_test.model.OrderItem;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class WorkerUtils {

    private WorkerUtils() {}

    static List<OrderItem> toOrderItems(List<Map<String, Object>> raw) {
        if (raw == null) return Collections.emptyList();
        return raw.stream().map(m -> OrderItem.builder()
                .productId((String) m.get("productId"))
                .productName((String) m.get("productName"))
                .quantity(m.get("quantity") instanceof Number n ? n.intValue() : 0)
                .unitPrice(m.get("unitPrice") != null ? new BigDecimal(m.get("unitPrice").toString()) : BigDecimal.ZERO)
                .build()
        ).collect(Collectors.toList());
    }
}
