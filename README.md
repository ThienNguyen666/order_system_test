# Order Fulfillment Saga

A teaching project that demonstrates the **Saga pattern** for distributed transactions using [Netflix Conductor](https://conductor.netflix.com/) as the workflow engine, Spring Boot as the backend, React as the frontend, MongoDB for persistence, and Redis as a human-task queue cache.

---

This project models placing an order:

```
                     ┌──────────────────────────────────────────┐
                     │  intake_approval  (HUMAN)                │
  Submit Order       │  risk_approval     (HUMAN)                │
      │              │  compliance_approval (HUMAN)               │
      ▼              └─────────────────┬──────────────────────────┘
  validate_order ───────────────────── │
  (INLINE/JS)                          ▼
                             startup_approval_decision (SWITCH)
                            /                              \
                      rejected                          approved
                         │                                  │
                  terminate workflow            ┌─────────────────────────┐
                                                  │      FORK (parallel)    │
                                                  │  reserve_inventory      │
                                                  │  authorize_payment      │
                                                  │  fraud_check            │
                                                  └────────────┬────────────┘
                                                               │ JOIN
                                                               ▼
                                                        saga_decision (SWITCH)
                                                       /                    \
                                                  success                compensation
                                                     │                       │
                                          approval_gate (SWITCH)     release_inventory
                                       requireApproval? → manual_approval   void_payment
                                                     │
                                          approval_decision (SWITCH)
                                         /                        \
                                    approved                   rejected
                                       │                           │
                            create_shipment              release_inventory
                          (sub-workflow)                  void_payment
                                       │
                            update_order_status
```

Three human reviewers (intake, risk, compliance) must approve before the order is fulfilled. Once approved, inventory, payment, and fraud checks run in parallel. If all pass, an optional manual approval gate (`requireApproval`) can require a fourth human sign-off before shipment. Any failure — at the parallel-checks stage or at manual approval — triggers the compensation path, which rolls back inventory and voids the payment.

---

## Tech stack

| Layer | Technology |
|---|---|
| Workflow engine | [Orkes Conductor Community](https://github.com/orkes-io/orkes-conductor-community) |
| Backend | Spring Boot 3.3, Java 17 |
| Workers | Custom polling loop via `RestTemplate` (no Orkes Cloud required) |
| Database | MongoDB 7 |
| Human-task queue cache | Redis 7 |
| Frontend | React + Vite + Nginx |
| Infrastructure | Docker Compose |

---

## Running locally

**Prerequisites:** Docker Desktop, ports 3000 / 8080 / 8085 / 6379 / 27017 free.

```bash
# 1. Start everything
docker compose up --build

# 2. Open the app
open http://localhost:3000
```

On startup, the backend automatically registers the task and workflow definitions from
`conductor-workflow.json` with Conductor (`ConductorMetadataService`) — no manual import step needed.

The backend is at `http://localhost:8080`, Conductor UI at `http://localhost:8085`.

---

## Services

| Container | Port | Purpose |
|---|---|---|
| `order-frontend` | 3000 | React UI |
| `order-backend` | 8080 | Spring Boot API |
| `order-conductor` | 8085 | Conductor workflow engine |
| `order-mongodb` | 27017 | MongoDB |
| `order-redis` | 6379 | Redis (human task queue cache) |

---

## Trying the saga

The frontend has three quick-fill presets:

- **Valid Order** — a normal $299.99 order that follows the success path all the way to shipment.
- **High-Value (Fraud / Chaos)** — a $15,000 order that trips the fraud-risk check; combined with Chaos Mode it routes to the compensation branch.
- **Retry + Approval** — sets `demoPaymentFailures` so payment authorization fails twice before succeeding (exercising the retry/backoff policy), and sets `requireApproval` so a manual approval task is created before shipment.

Other controls on the order form:
- **Demo payment failures** — force N payment authorization failures before it succeeds, to exercise `authorize_payment`'s retry policy.
- **Simulate payment timeout** — force a timeout to exercise the task's `timeoutPolicy`.
- **Require approval** — gates shipment behind a manual `HUMAN` task (`manual_approval_ref`).
- **Chaos Mode** toggle (top of page) — globally perturbs worker behavior to make failure/compensation paths easier to trigger on demand.
- **Clear All** button — deletes all orders and audit logs via `DELETE /api/orders`, for resetting demo state.

Every order also passes through three startup human-approval tasks (intake, risk, compliance) before any automated processing begins. Pending human tasks are listed per order in the UI and can be approved/rejected via `POST /api/orders/{orderId}/human-tasks/{taskReferenceName}/complete` (or `/approval` for the legacy manual-approval-only endpoint). Pending tasks are cached in Redis (`HumanTaskQueueService`) keyed by order, with a 6-hour TTL.

After submitting, open the Conductor UI at `http://localhost:8085`, navigate to **Executions → Workflows**, and watch the saga execute in real time. You can see each task transition through `SCHEDULED → IN_PROGRESS → COMPLETED`. The frontend's **Workflow Diagram** view (`WorkflowDiagram.jsx`) also renders live task status per order.

---

## Project layout

```
src/main/java/mdl/order_system_test/
├── config/
│   └── ConductorConfig.java        # RestTemplate bean + @EnableScheduling
├── controller/
│   ├── OrderController.java        # CRUD, logs, workflow status, human-task completion
│   └── ChaosController.java        # Chaos Mode get/toggle/set
├── dto/                            # Request / response shapes
├── model/                          # Order, AuditLog, OrderItem
├── repository/                     # MongoDB repos
├── service/
│   ├── OrderService.java              # Saves order, starts workflow via RestTemplate
│   ├── WorkerPollingService.java      # @Scheduled: polls Conductor every 250 ms
│   ├── ConductorMetadataService.java  # Registers task/workflow defs with Conductor on startup
│   ├── HumanTaskQueueService.java     # Redis-backed cache of pending HUMAN tasks per order
│   ├── ChaosToggleService.java        # In-memory Chaos Mode flag
│   ├── InventoryService.java
│   └── PaymentService.java
└── worker/                         # One Worker per Conductor task type
    ├── ReserveInventoryWorker.java
    ├── AuthorizePaymentWorker.java
    ├── FraudCheckWorker.java
    ├── CreateShipmentWorker.java
    ├── ReleaseInventoryWorker.java
    ├── VoidPaymentWorker.java
    ├── UpdateOrderStatusWorker.java
    └── WorkerUtils.java

frontend/src/
└── components/
    ├── OrderForm.jsx        # Order creation form with quick-fill presets
    ├── OrderGrid.jsx        # Order list/table
    ├── StatusBadge.jsx
    ├── ChaosToggle.jsx      # Chaos Mode toggle
    └── WorkflowDiagram.jsx  # Live per-order workflow visualization

conductor-workflow.json             # Workflow + sub-workflow definitions
```

---

## How the worker polling works

Rather than using the Orkes Cloud SDK (which requires credentials and doesn't work against the community server), the backend uses a simple `@Scheduled` loop:

```
every 250 ms:
  for each worker:
    GET /api/tasks/poll/{taskType}?workerid=order-backend-1
    if 200 and body has a taskId:
      execute the worker
      POST /api/tasks   ← updates result back to Conductor
```

This is all standard Netflix Conductor REST API — no proprietary client needed.

`HUMAN` tasks (intake/risk/compliance/manual approval) are not polled by a worker; they stay `SCHEDULED`/`IN_PROGRESS` until a reviewer completes them via the API, at which point Conductor advances the workflow.

---

## No API keys required

This project runs entirely locally. There is no dependency on Orkes Cloud.  
A `.env` file (gitignored) can optionally hold Orkes Cloud credentials if you want to point the backend at a cloud-hosted Conductor instead:

```
CONDUCTOR_SERVER_URL=https://developer.orkescloud.com/api
CONDUCTOR_CLIENT_KEY_ID=...
CONDUCTOR_CLIENT_SECRET=...
```

But for local Docker usage, none of that is needed.
