# Order Module — OpenAPI Spec Design Reference

> **Parent document:** [Order Module Implementation Plan](order-module-plan.md)
> **Target file:** `src/main/resources/api-specs/order-openapi.yaml`

This document defines the full API contract for the enhanced order module. It replaces the existing `order-openapi.yaml` which only defines 2 endpoints.

---

## Endpoints Overview

| Method | Path | Tag | Description |
|--------|------|-----|-------------|
| `POST` | `/api/v1/orders` | Orders | Create order from cart |
| `GET` | `/api/v1/orders` | Orders | Paginated order history with filters |
| `GET` | `/api/v1/orders/{orderId}` | Orders | Single order detail |
| `POST` | `/api/v1/orders/{orderId}/cancel` | Orders | Cancel order |
| `POST` | `/api/v1/orders/{orderId}/refund` | Orders | Request refund |
| `POST` | `/api/v1/orders/{orderId}/reorder` | Orders | Re-order (add items to cart) |
| `GET` | `/api/v1/orders/{orderId}/history` | Orders | Order status change history |
| `GET` | `/api/v1/admin/orders` | Admin Orders | List all orders (admin) |
| `PATCH` | `/api/v1/admin/orders/{orderId}/status` | Admin Orders | Update order status (admin) |

---

## User Endpoints

### POST /api/v1/orders — Create Order

**Request body:** `CreateNewOrderRequestDto`

```yaml
CreateNewOrderRequestDto:
  type: object
  description: "Create a new order from the user's shopping cart."
  required:
    - recipientName
    - recipientSurname
  properties:
    deliveryAddressId:
      type: string
      format: uuid
      description: "ID of a saved delivery address. Mutually exclusive with 'address'."
    address:
      $ref: "user-openapi.yaml#/components/schemas/AddressDto"
      description: "Inline delivery address. Mutually exclusive with 'deliveryAddressId'."
    recipientName:
      type: string
      minLength: 2
      maxLength: 128
      example: "John"
    recipientSurname:
      type: string
      minLength: 2
      maxLength: 128
      example: "Doe"
    recipientPhone:
      type: string
      example: "+12025550123"
    paymentMethodId:
      type: string
      format: uuid
      description: "ID of a saved payment method. If omitted, Stripe shows card entry form."
```

**Request header (optional):**

```yaml
parameters:
  - name: Idempotency-Key
    in: header
    required: false
    schema:
      type: string
      format: uuid
    description: "Client-generated key to prevent duplicate orders. If an order with this key already exists, the existing order is returned (200 OK instead of 201)."
```

**Response:** `201 Created` → `OrderDetailDto` (or `200 OK` if idempotency key matched an existing order)

**Errors:** `400` (invalid input, empty cart), `401` (unauthorized), `404` (address/payment method not found), `409` (duplicate idempotency key)

---

### GET /api/v1/orders — Paginated Order History

**Query parameters:**

```yaml
parameters:
  - name: page
    in: query
    schema:
      type: integer
      default: 0
      minimum: 0
  - name: size
    in: query
    schema:
      type: integer
      default: 10
      minimum: 1
      maximum: 50
  - name: sortBy
    in: query
    schema:
      type: string
      enum: [createdAt, itemsTotalPrice]
      default: createdAt
  - name: sortDirection
    in: query
    schema:
      type: string
      enum: [ASC, DESC]
      default: DESC
  - name: status
    in: query
    schema:
      type: array
      items:
        $ref: "#/components/schemas/OrderStatus"
  - name: year
    in: query
    schema:
      type: integer
      example: 2026
  - name: dateFrom
    in: query
    schema:
      type: string
      format: date
  - name: dateTo
    in: query
    schema:
      type: string
      format: date
```

**Response:** `200 OK` → `OrderPageDto`

**Errors:** `400` (invalid pagination/filter parameters — e.g., negative page, size > 50, invalid sortBy field, dateFrom > dateTo), `401` (unauthorized), `500` (internal server error)

```yaml
OrderPageDto:
  type: object
  properties:
    content:
      type: array
      items:
        $ref: "#/components/schemas/OrderSummaryDto"
    page:
      type: integer
    size:
      type: integer
    totalElements:
      type: integer
      format: int64
    totalPages:
      type: integer
```

---

### GET /api/v1/orders/{orderId} — Order Detail

**Path parameter:** `orderId` (UUID)

**Response:** `200 OK` → `OrderDetailDto`

**Errors:** `401`, `403` (not your order), `404` (order not found), `500`

---

### POST /api/v1/orders/{orderId}/cancel — Cancel Order

**No request body.** Cancellation is a simple action.

**Response:** `200 OK` → `OrderDetailDto` (with updated status CANCELLED)

**Errors:** `401`, `403`, `404`, `409` (not cancellable — wrong status or window expired), `500`

---

### POST /api/v1/orders/{orderId}/refund — Request Refund

**Request body:**

```yaml
RefundRequestDto:
  type: object
  properties:
    reason:
      type: string
      maxLength: 500
      description: "Optional reason for the refund request."
      example: "Product arrived damaged"
```

**Response:** `200 OK` → `OrderDetailDto` (with status REFUND_REQUESTED)

**Errors:** `401`, `403`, `404`, `409` (not refundable — wrong status), `500`

---

### POST /api/v1/orders/{orderId}/reorder — Buy Again

**No request body.**

**Response:** `200 OK` → `ReorderResponseDto`

```yaml
ReorderResponseDto:
  type: object
  properties:
    cartId:
      type: string
      format: uuid
    addedItems:
      type: integer
    unavailableItems:
      type: array
      items:
        $ref: "#/components/schemas/UnavailableItemDto"

UnavailableItemDto:
  type: object
  properties:
    productName:
      type: string
    reason:
      type: string
```

**Errors:** `401`, `403`, `404`, `500`

---

### GET /api/v1/orders/{orderId}/history — Status Change History

**Response:** `200 OK` → array of `OrderStatusHistoryDto`

```yaml
OrderStatusHistoryDto:
  type: object
  properties:
    id:
      type: string
      format: uuid
    oldStatus:
      $ref: "#/components/schemas/OrderStatus"
    newStatus:
      $ref: "#/components/schemas/OrderStatus"
    changedBy:
      type: string
      format: uuid
    reason:
      type: string
    changedAt:
      type: string
      format: date-time
```

**Errors:** `401`, `403` (not your order), `404` (order not found), `500`

---

## Admin Endpoints

### GET /api/v1/admin/orders — List All Orders

Same query parameters as `GET /api/v1/orders` plus:

```yaml
parameters:
  - name: userId
    in: query
    schema:
      type: string
      format: uuid
    description: "Filter by specific user"
```

**Response:** `200 OK` → `OrderPageDto` (same paginated structure)

**Auth:** Requires ADMIN role

**Errors:** `400` (invalid parameters), `401`, `403` (not admin), `500`

---

### PATCH /api/v1/admin/orders/{orderId}/status — Update Order Status

**Request body:**

```yaml
AdminOrderStatusUpdateDto:
  type: object
  required:
    - event
  properties:
    event:
      $ref: "#/components/schemas/OrderEvent"
      description: "The event to trigger on the order."
    reason:
      type: string
      maxLength: 500
      description: "Optional reason for the status change."
```

**Response:** `200 OK` → `OrderDetailDto`

**Errors:** `401`, `403` (not admin), `404`, `409` (invalid transition), `500`

---

## Schemas

### OrderStatus (expanded)

```yaml
OrderStatus:
  type: string
  enum:
    - CREATED
    - PAID
    - SHIPPED
    - DELIVERED
    - CANCELLED
    - REFUND_REQUESTED
    - REFUNDED
```

**Note:** Replaces the existing 4-status enum (CREATED, PAID, DELIVERY, FINISHED). The rename of DELIVERY → SHIPPED and FINISHED → DELIVERED is more standard e-commerce terminology.

### OrderEvent

```yaml
OrderEvent:
  type: string
  enum:
    - PAYMENT_CONFIRMED
    - SHIP
    - DELIVER
    - CANCEL
    - REQUEST_REFUND
    - REFUND_CONFIRMED
```

### OrderSummaryDto (for list view)

```yaml
OrderSummaryDto:
  type: object
  description: "Lightweight order representation for list views."
  properties:
    id:
      type: string
      format: uuid
    status:
      $ref: "#/components/schemas/OrderStatus"
    createdAt:
      type: string
      format: date-time
    itemsQuantity:
      type: integer
    itemsTotalPrice:
      type: number
      format: double
    firstItemName:
      type: string
      description: "Name of the first item (for preview display)."
    firstItemImageUrl:
      type: string
      description: "Image URL of the first item (for preview display)."
```

**Implementation note:** `firstItemName` is available from `order_item.product_name`. `firstItemImageUrl` requires a lookup against the `product_image` table via `order_item.product_id` → `product_image.product_id`. This is a cross-module read (order → product). Two options: (a) resolve at query time via a JOIN or a separate call to `ProductImageReceiver`, or (b) snapshot the image URL into `order_item` at order creation time (add `product_image_url VARCHAR(512)` column). Option (b) is more consistent with the snapshot pattern already used for name/price, but adds a column. Decision should be made during Phase 3 implementation.

### OrderDetailDto (for single order view)

```yaml
OrderDetailDto:
  type: object
  description: "Full order representation with all details."
  properties:
    id:
      type: string
      format: uuid
    status:
      $ref: "#/components/schemas/OrderStatus"
    createdAt:
      type: string
      format: date-time
    updatedAt:
      type: string
      format: date-time
    itemsQuantity:
      type: integer
    itemsTotalPrice:
      type: number
      format: double
    items:
      type: array
      items:
        $ref: "#/components/schemas/OrderItemDto"
    deliveryAddress:
      $ref: "#/components/schemas/OrderAddressDto"
    recipientName:
      type: string
    recipientSurname:
      type: string
    recipientPhone:
      type: string
    cancellationDeadline:
      type: string
      format: date-time
      description: "Deadline after which the order cannot be cancelled."
    canCancel:
      type: boolean
      description: "Whether the order can currently be cancelled."
    canRefund:
      type: boolean
      description: "Whether a refund can currently be requested."
```

### OrderItemDto

```yaml
OrderItemDto:
  type: object
  properties:
    id:
      type: string
      format: uuid
    productId:
      type: string
      format: uuid
    productName:
      type: string
    productPrice:
      type: number
      format: double
    productsQuantity:
      type: integer
```

### OrderAddressDto

```yaml
OrderAddressDto:
  type: object
  description: "Snapshot of the delivery address at order time."
  properties:
    line:
      type: string
    city:
      type: string
    country:
      type: string
    postcode:
      type: string
```

---

## Migration Notes

**Breaking changes from current API:**

1. `GET /api/v1/orders` response changes from a flat array to a paginated `OrderPageDto` object. Frontend must update to read `content` array from the response.
2. `OrderStatus` enum values change: `DELIVERY` → `SHIPPED`, `FINISHED` → `DELIVERED`, plus new statuses added. Frontend must handle new values.
3. `CreateNewOrderRequestDto`: the `address` field changes from **required** to **optional** (now mutually exclusive with `deliveryAddressId`). New optional fields added: `deliveryAddressId`, `paymentMethodId`. Existing requests that include `address` continue to work. The `Idempotency-Key` header is also new but optional.
4. New endpoints added: `GET /{orderId}`, `POST /{orderId}/cancel`, `POST /{orderId}/refund`, `POST /{orderId}/reorder`, `GET /{orderId}/history`, and admin endpoints at `/api/v1/admin/orders`. These are purely additive — no existing endpoints are removed.

**Backward compatibility strategy:**
- **Step 1:** Update the OpenAPI spec to include BOTH old and new status values temporarily: `CREATED, PAID, DELIVERY, SHIPPED, FINISHED, DELIVERED, CANCELLED, REFUND_REQUESTED, REFUNDED`. This generates a Java enum that accepts both old and new values. Deploy backend.
- **Step 2:** Run the DB data migration (Migration 4) to rename existing rows: `DELIVERY` → `SHIPPED`, `FINISHED` → `DELIVERED`.
- **Step 3:** Update the frontend to use new status values. Key files:
  - `src/features/orders/types/orderTypes.ts` — update `Order.status` union type
  - `src/features/orders/components/OrderStatusBadge.tsx` — update `STATUS_CONFIG` keys and labels
  - `src/features/orders/components/OrderHistory.tsx` — update `FILTERS` array values (`DELIVERY` → `SHIPPED`, `FINISHED` → `DELIVERED`)
  - The frontend currently sends `?status=DELIVERY` in filter tabs — this must change to `?status=SHIPPED`
- **Step 4:** Remove old status values (`DELIVERY`, `FINISHED`) from the OpenAPI spec. Deploy backend. Now only new values are accepted.

**Important:** Steps 1–2 (backend) and Step 3 (frontend) can be deployed independently because the backend temporarily accepts both old and new values. Step 4 must happen AFTER the frontend is deployed.

**Frontend files requiring updates** (in `/Users/zufar/IdeaProjects/Iced-Latte-Frontend`):

| Breaking change | Frontend file(s) to update |
|---|---|
| Paginated response (#1) | `src/features/orders/api/ordersApi.ts` — read `response.data.content` instead of `response.data`; `src/features/orders/hooks/useOrders.ts` — handle pagination metadata |
| Status enum rename (#2) | `src/features/orders/types/orderTypes.ts` — update `Order.status` union type; `src/features/orders/components/OrderStatusBadge.tsx` — update badge mappings for SHIPPED, DELIVERED, and new statuses |
| Address optional (#3) | `src/features/checkout/hooks/useCheckoutForm.ts` — send `deliveryAddressId` when saved address selected instead of always sending inline `address` |
