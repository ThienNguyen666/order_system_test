package mdl.order_system_test.controller;

import lombok.RequiredArgsConstructor;
import mdl.order_system_test.service.ChaosToggleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chaos")
@RequiredArgsConstructor
public class ChaosController {

    private final ChaosToggleService chaosToggleService;

    @GetMapping
    public ResponseEntity<Map<String, Boolean>> getStatus() {
        return ResponseEntity.ok(Map.of("chaosEnabled", chaosToggleService.isChaosEnabled()));
    }

    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Boolean>> toggle() {
        boolean newState = chaosToggleService.toggle();
        return ResponseEntity.ok(Map.of("chaosEnabled", newState));
    }

    @PutMapping
    public ResponseEntity<Map<String, Boolean>> set(@RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("chaosEnabled", false);
        chaosToggleService.set(enabled);
        return ResponseEntity.ok(Map.of("chaosEnabled", enabled));
    }
}
