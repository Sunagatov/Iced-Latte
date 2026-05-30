#!/usr/bin/env bash
# ╔══════════════════════════════════════════════════════════════╗
# ║           Iced Latte - API Integration Test Suite           ║
# ╚══════════════════════════════════════════════════════════════╝

set -uo pipefail

BASE_URL="${1:-http://localhost:8083}"
API="$BASE_URL/api/v1"
API_TEST_DELAY_SECONDS="${API_TEST_DELAY_SECONDS:-1.5}"

# ── Colours & symbols ────────────────────────────────────────────
GRN='\033[0;32m'; RED='\033[0;31m'; YLW='\033[0;33m'
BLU='\033[0;34m'; CYN='\033[0;36m'; DIM='\033[2m'
BLD='\033[1m';    NC='\033[0m'

PASS="[PASS]"; FAIL="[FAIL]"; SKIP="[SKIP]"; INFO="[INFO]"

# ── State ────────────────────────────────────────────────────────
JWT_TOKEN=""
REFRESH_TOKEN=""
CODE=""
BODY=""
PRODUCT_ID=""
CART_ITEM_ID=""
REVIEW_ID=""
REVIEW_CREATED_BY_TEST=false

TOTAL=0; PASSED=0; FAILED=0; SKIPPED=0
declare -a FAILURES=()

# ── Helpers ──────────────────────────────────────────────────────
section() { echo -e "\n${BLD}${BLU}---  $1  ---${NC}"; }

pass()  { TOTAL=$((TOTAL+1)); PASSED=$((PASSED+1));
          echo -e "  ${GRN}${PASS} $1${NC}"; }

fail()  { TOTAL=$((TOTAL+1)); FAILED=$((FAILED+1));
          FAILURES+=("$1");
          echo -e "  ${RED}${FAIL} $1${NC}"
          [[ -n "${2:-}" ]] && echo -e "  ${DIM}    > $2${NC}"; }

skip()  { TOTAL=$((TOTAL+1)); SKIPPED=$((SKIPPED+1));
          echo -e "  ${YLW}${SKIP} $1 ${DIM}(${2:-missing prerequisite})${NC}"; }

info()  { echo -e "  ${DIM}${INFO}  $1${NC}"; }

# Sets globals CODE and BODY - never called in a subshell
_BODY_FILE=$(mktemp)
trap 'rm -f "$_BODY_FILE"' EXIT

req() {
    sleep "$API_TEST_DELAY_SECONDS"   # stay under rate limit
    local method="$1"; shift
    local url="$1";    shift
    CODE=$(curl -sk -X "$method" "$url" "$@" -w "%{http_code}" -o "$_BODY_FILE" 2>/dev/null) || CODE="000"
    BODY=$(cat "$_BODY_FILE")
}

# assert LABEL [expected_codes...] - treats 429 as skip
assert() {
    local label="$1"; shift
    local codes=("$@")
    if [[ "$CODE" == "429" ]]; then
        skip "$label" "rate limited - 429"
        return
    fi
    for c in "${codes[@]}"; do
        [[ "$CODE" == "$c" ]] && { pass "$label -> $CODE"; return; }
    done
    fail "$label -> expected $(IFS=/; echo "${codes[*]}"), got $CODE" "$BODY"
}

json_val() { echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('$1',''))" 2>/dev/null; }

# ════════════════════════════════════════════════════════════════
#  AUTH
# ════════════════════════════════════════════════════════════════
section "Auth"

req POST "$API/auth/authenticate" \
    -H "Content-Type: application/json" \
    -d '{"email":"olivia@example.com","password":"p@ss1logic11"}'
if [[ "$CODE" == "200" ]]; then
    JWT_TOKEN=$(json_val token)
    REFRESH_TOKEN=$(json_val refreshToken)
    if [[ -n "$JWT_TOKEN" ]]; then
        pass "Login -> 200"
    else
        fail "Login -> token missing in response" "$BODY"
    fi
elif [[ "$CODE" == "429" ]]; then
    skip "Login" "rate limited - 429"
else
    fail "Login -> expected 200, got $CODE" "$BODY"
fi

req POST "$API/auth/authenticate" \
    -H "Content-Type: application/json" \
    -d '{"email":"olivia@example.com","password":"wrong"}'
assert "Login with wrong password (rejected)" "401" "400"

req POST "$API/auth/authenticate" \
    -H "Content-Type: application/json" \
    -d '{"email":"not-an-email","password":""}'
assert "Login with malformed payload (rejected)" "400"

req POST "$API/auth/register" \
    -H "Content-Type: application/json" \
    -d '{"firstName":"Olivia","lastName":"Taylor","email":"olivia@example.com","password":"Password123"}'
assert "Register duplicate email (rejected)" "400" "409"

req POST "$API/auth/register" \
    -H "Content-Type: application/json" \
    -d '{"firstName":"O","lastName":"","email":"bad","password":"weak"}'
assert "Register invalid payload (rejected)" "400"

req POST "$API/auth/password/forgot" \
    -H "Content-Type: application/json" \
    -d '{"email":"not-an-email"}'
assert "Forgot password invalid email (rejected)" "400"

req POST "$API/auth/password/change" \
    -H "Content-Type: application/json" \
    -d '{"code":"invalid","password":"Password123"}'
assert "Password reset invalid code (rejected)" "400"

req GET "$API/auth/oauth/unsupported"
assert "Unsupported OAuth provider (not found)" "404"

if [[ -n "$REFRESH_TOKEN" ]]; then
    req POST "$API/auth/refresh" -H "Authorization: Bearer $REFRESH_TOKEN"
    assert "Token refresh" "200"
else
    skip "Token refresh"
fi

req POST "$API/auth/refresh" -H "Authorization: Bearer invalid.token.here"
assert "Refresh with invalid token (rejected)" "401" "403"

req GET "$API/users"
assert "Protected endpoint without token (rejected)" "401" "403"

if [[ -n "$JWT_TOKEN" ]]; then
    req GET "$API/auth/sessions" -H "Authorization: Bearer $JWT_TOKEN"
    assert "List auth sessions" "200"

    req DELETE "$API/auth/sessions/00000000-0000-0000-0000-000000000000" \
        -H "Authorization: Bearer $JWT_TOKEN"
    assert "Revoke missing session (not found)" "404"
else
    skip "List auth sessions"
    skip "Revoke missing session"
fi

# ════════════════════════════════════════════════════════════════
#  PRODUCTS  (public)
# ════════════════════════════════════════════════════════════════
section "Products"

req GET "$API/products?page=0&size=5"
if [[ "$CODE" == "200" ]]; then
    PRODUCT_ID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['products'][0]['id'])" 2>/dev/null)
    pass "List products -> 200"
    [[ -n "$PRODUCT_ID" ]] && info "first product id: $PRODUCT_ID"
else
    assert "List products" "200"
fi

if [[ -n "$PRODUCT_ID" ]]; then
    req GET "$API/products/$PRODUCT_ID"
    assert "Get product by ID" "200"
else
    skip "Get product by ID"
fi

req GET "$API/products?page=0&size=3&sort_attribute=price&sort_direction=desc&min_price=1"
assert "Products with price/sort filters" "200"

req GET "$API/products?page=0&size=3&sort_attribute=averageRating&sort_direction=desc&minimum_average_rating=1"
assert "Products filtered by min rating" "200"

req GET "$API/products?page=-1&size=3"
assert "Products invalid page (rejected)" "400"

req GET "$API/products?page=0&size=3&sort_attribute=unknown"
assert "Products invalid sort attribute (rejected)" "400"

req GET "$API/products?page=0&size=3&minimum_average_rating=9"
assert "Products invalid rating filter (rejected)" "400"

req GET "$API/products/00000000-0000-0000-0000-000000000000"
assert "Get non-existent product (not found)" "404" "400"

req GET "$API/products/not-a-uuid"
assert "Get malformed product ID (rejected)" "400"

req GET "$API/products/sellers"
assert "List sellers" "200"

req GET "$API/products/brands"
assert "List brands" "200"

if [[ -n "$PRODUCT_ID" ]]; then
    req POST "$API/products/ids" \
        -H "Content-Type: application/json" \
        -d "{\"productIds\":[\"$PRODUCT_ID\"]}"
    assert "Get products by IDs" "200"

    req POST "$API/products/ids" \
        -H "Content-Type: application/json" \
        -d '{"productIds":[]}'
    assert "Get products by empty IDs (rejected)" "400"
else
    skip "Get products by IDs"
    skip "Get products by empty IDs"
fi

# ════════════════════════════════════════════════════════════════
#  USER PROFILE
# ════════════════════════════════════════════════════════════════
section "User Profile"

if [[ -z "$JWT_TOKEN" ]]; then
    skip "Get user profile"; skip "Update user profile"
    skip "Get avatar link"; skip "Change password"
else
    req GET "$API/users" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get user profile" "200"

    req PUT "$API/users" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"firstName":"Olivia","lastName":"Example","phoneNumber":"+1234567890"}'
    assert "Update user profile" "200"

    req PUT "$API/users" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"firstName":"O","lastName":"","phoneNumber":"+1234567890"}'
    assert "Update user profile invalid payload (rejected)" "400"

    req GET "$API/users/avatar" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get avatar link" "200" "404"

    req DELETE "$API/users/avatar" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Delete missing avatar (not found or no-op)" "200" "204" "404"

    req PATCH "$API/users" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"oldPassword":"WrongPassword1","newPassword":"AnotherPassword1"}'
    assert "Change password with wrong old password (rejected)" "401" "403"
fi

# ════════════════════════════════════════════════════════════════
#  SHOPPING CART
# ════════════════════════════════════════════════════════════════
section "Shopping Cart"

if [[ -z "$JWT_TOKEN" ]]; then
    skip "Get cart"; skip "Add item to cart"
    skip "Update cart item quantity"; skip "Delete cart item"
else
    req GET "$API/cart"
    assert "Get cart without token (rejected)" "401" "403"

    req GET "$API/cart" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get cart" "200" "404"

    if [[ -n "$PRODUCT_ID" ]]; then
        req POST "$API/cart/items" \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"items\":[{\"productId\":\"$PRODUCT_ID\",\"productQuantity\":0}]}"
        assert "Add zero quantity cart item (rejected)" "400"

        req POST "$API/cart/items" \
            -H "Content-Type: application/json" \
            -d "{\"items\":[{\"productId\":\"$PRODUCT_ID\",\"productQuantity\":1}]}"
        assert "Add cart item without token (rejected)" "401" "403"

        req POST "$API/cart/items" \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"items\":[{\"productId\":\"$PRODUCT_ID\",\"productQuantity\":1}]}"
        if [[ "$CODE" == "200" ]]; then
            CART_ITEM_ID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['items'][0]['id'])" 2>/dev/null)
            pass "Add item to cart -> 200"
            [[ -n "$CART_ITEM_ID" ]] && info "cart item id: $CART_ITEM_ID"
        else
            assert "Add item to cart" "200"
        fi
    else
        skip "Add item to cart"
    fi

    req PATCH "$API/cart/items" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"shoppingCartItemId":"00000000-0000-0000-0000-000000000000","productQuantityChange":1}'
    assert "Update missing cart item (not found)" "404"

    req DELETE "$API/cart/items" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"shoppingCartItemIds":[]}'
    assert "Delete empty cart item list (rejected)" "400"

    if [[ -n "$CART_ITEM_ID" ]]; then
        req PATCH "$API/cart/items" \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"shoppingCartItemId\":\"$CART_ITEM_ID\",\"productQuantityChange\":1}"
        assert "Update cart item quantity" "200"

        req DELETE "$API/cart/items" \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"shoppingCartItemIds\":[\"$CART_ITEM_ID\"]}"
        assert "Delete cart item" "200"
    else
        skip "Update cart item quantity"
        skip "Delete cart item"
    fi
fi

# ════════════════════════════════════════════════════════════════
#  FAVORITES
# ════════════════════════════════════════════════════════════════
section "Favorites"

if [[ -z "$JWT_TOKEN" || -z "$PRODUCT_ID" ]]; then
    skip "Add to favorites"; skip "Get favorites"; skip "Remove from favorites"
else
    req GET "$API/favorites"
    assert "Get favorites without token (rejected)" "401" "403"

    req POST "$API/favorites" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"productIds":[]}'
    assert "Add empty favorites list (rejected)" "400"

    req POST "$API/favorites" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"productIds\":[\"$PRODUCT_ID\"]}"
    assert "Add product to favorites" "200"

    req GET "$API/favorites" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get favorites list" "200"

    req DELETE "$API/favorites/$PRODUCT_ID" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Remove product from favorites" "200"

    req DELETE "$API/favorites/00000000-0000-0000-0000-000000000000" \
        -H "Authorization: Bearer $JWT_TOKEN"
    assert "Remove missing favorite (not found or no-op)" "200" "404"
fi

# ════════════════════════════════════════════════════════════════
#  PRODUCT REVIEWS
# ════════════════════════════════════════════════════════════════
section "Product Reviews"

if [[ -n "$PRODUCT_ID" ]]; then
    req GET "$API/products/$PRODUCT_ID/reviews?page=0&size=5"
    assert "List product reviews (public)" "200"

    req GET "$API/products/$PRODUCT_ID/reviews/statistics"
    assert "Review statistics (public)" "200"

    req GET "$API/products/$PRODUCT_ID/reviews?sort_attribute=productRating&sort_direction=desc"
    assert "Reviews sorted by rating desc" "200"

    req GET "$API/products/$PRODUCT_ID/reviews?productRatings=1,1"
    assert "Reviews duplicate rating filter (rejected)" "400"

    req GET "$API/products/$PRODUCT_ID/reviews?productRatings=9"
    assert "Reviews invalid rating filter (rejected)" "400"

    req GET "$API/products/$PRODUCT_ID/reviews?sort_attribute=unknown"
    assert "Reviews invalid sort attribute (rejected)" "400"

    req GET "$API/products/00000000-0000-0000-0000-000000000000/reviews/statistics"
    assert "Review statistics missing product (not found)" "404"
else
    skip "List product reviews"; skip "Review statistics"; skip "Reviews sorted by rating"
fi

if [[ -z "$JWT_TOKEN" || -z "$PRODUCT_ID" ]]; then
    skip "Get own review"; skip "Add review"; skip "Like review"; skip "Delete review"
else
    req POST "$API/products/$PRODUCT_ID/reviews" \
        -H "Content-Type: application/json" \
        -d '{"text":"Unauthorized review","rating":5}'
    assert "Add review without token (rejected)" "401" "403"

    req POST "$API/products/$PRODUCT_ID/reviews" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"text":"","rating":9}'
    assert "Add invalid review (rejected)" "400"

    req GET "$API/products/$PRODUCT_ID/review" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get own review before create" "200" "404"

    req POST "$API/products/$PRODUCT_ID/reviews" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"text":"Great coffee, highly recommend!","rating":5}'
    if [[ "$CODE" == "200" ]]; then
        REVIEW_ID=$(json_val productReviewId)
        REVIEW_CREATED_BY_TEST=true
        pass "Add product review -> 200"
        [[ -n "$REVIEW_ID" ]] && info "review id: $REVIEW_ID"
    elif [[ "$CODE" == "400" ]]; then
        pass "Add product review -> 400 (already exists - expected)"
    else
        assert "Add product review" "200" "400"
    fi

    if [[ "$REVIEW_CREATED_BY_TEST" == true && -n "$REVIEW_ID" ]]; then
        req POST "$API/products/$PRODUCT_ID/reviews/$REVIEW_ID/likes" \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d '{"isLike":true}'
        assert "Like a review" "200"

        req DELETE "$API/products/$PRODUCT_ID/reviews/$REVIEW_ID" \
            -H "Authorization: Bearer $JWT_TOKEN"
        assert "Delete own review" "200"
    else
        skip "Like a review" "review was not created by this test run"
        skip "Delete own review" "review was not created by this test run"
    fi

    req POST "$API/products/$PRODUCT_ID/reviews/00000000-0000-0000-0000-000000000000/likes" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"isLike":true}'
    assert "Like missing review (not found)" "404"
fi

# ════════════════════════════════════════════════════════════════
#  DELIVERY ADDRESSES
# ════════════════════════════════════════════════════════════════
section "Delivery Addresses"

ADDRESS_ID=""
if [[ -z "$JWT_TOKEN" ]]; then
    skip "Get delivery addresses"; skip "Create delivery address"
    skip "Update delivery address"; skip "Set default address"; skip "Delete delivery address"
else
    req GET "$API/users/addresses" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get delivery addresses" "200"

    req POST "$API/users/addresses" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"label":"","line":"","city":"","country":"","postcode":""}'
    assert "Create delivery address invalid payload (rejected)" "400"

    req PUT "$API/users/addresses/00000000-0000-0000-0000-000000000000" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"label":"Missing","line":"1 Test St","city":"London","country":"UK","postcode":"SW1A 1AA"}'
    assert "Update missing delivery address (not found)" "404"

    req PATCH "$API/users/addresses/00000000-0000-0000-0000-000000000000/default" \
        -H "Authorization: Bearer $JWT_TOKEN"
    assert "Set missing default address (not found)" "404"

    req POST "$API/users/addresses" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"label":"Home","line":"123 Test St","city":"London","country":"UK","postcode":"SW1A 1AA"}'
    if [[ "$CODE" == "201" ]]; then
        ADDRESS_ID=$(json_val id)
        pass "Create delivery address -> 201"
        [[ -n "$ADDRESS_ID" ]] && info "address id: $ADDRESS_ID"
    else
        assert "Create delivery address" "201"
    fi

    if [[ -n "$ADDRESS_ID" ]]; then
        req PUT "$API/users/addresses/$ADDRESS_ID" \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d '{"label":"Home","line":"456 Updated St","city":"London","country":"UK","postcode":"SW1A 2BB"}'
        assert "Update delivery address" "200"

        req PATCH "$API/users/addresses/$ADDRESS_ID/default" \
            -H "Authorization: Bearer $JWT_TOKEN"
        assert "Set default address" "200"

        req DELETE "$API/users/addresses/$ADDRESS_ID" \
            -H "Authorization: Bearer $JWT_TOKEN"
        assert "Delete delivery address" "204"
    else
        skip "Update delivery address"; skip "Set default address"; skip "Delete delivery address"
    fi
fi

# ════════════════════════════════════════════════════════════════
#  USER REVIEWS
# ════════════════════════════════════════════════════════════════
section "User Reviews"

if [[ -z "$JWT_TOKEN" ]]; then
    skip "Get user reviews"
else
    req GET "$API/users/reviews?page=0&size=5&sort_attribute=createdAt&sort_direction=desc" \
        -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get user reviews" "200"

    req GET "$API/users/reviews?page=-1&size=5" \
        -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get user reviews invalid page (rejected)" "400"
fi

# ════════════════════════════════════════════════════════════════
#  ORDERS
# ════════════════════════════════════════════════════════════════
section "Orders"

if [[ -z "$JWT_TOKEN" ]]; then
    skip "Get orders"; skip "Get orders filtered by status"
else
    req GET "$API/orders"
    assert "Get orders without token (rejected)" "401" "403"

    req GET "$API/orders" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get orders" "200" "404"

    req GET "$API/orders?status=CREATED&status=PAID" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get orders filtered by status" "200" "404"

    req GET "$API/orders?status=NOT_A_STATUS" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get orders invalid status (rejected)" "400"

    req GET "$API/orders/00000000-0000-0000-0000-000000000000" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get missing order (not found)" "404"

    req GET "$API/orders/00000000-0000-0000-0000-000000000000/history" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get missing order history (not found)" "404"

    req POST "$API/orders/00000000-0000-0000-0000-000000000000/cancel" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Cancel missing order (not found)" "404"

    req POST "$API/orders/00000000-0000-0000-0000-000000000000/refund" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"reason":"test"}'
    assert "Refund missing order (not found)" "404"

    req POST "$API/orders/00000000-0000-0000-0000-000000000000/reorder" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Reorder missing order (not found)" "404"

    req POST "$API/orders" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"recipientName":"O","recipientSurname":"","address":{"country":"","city":"","line":"","postcode":""}}'
    assert "Create order invalid payload (rejected)" "400"

    req GET "$API/admin/orders" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Admin orders as normal user (forbidden)" "403"
fi

# ════════════════════════════════════════════════════════════════
#  PAYMENT  (smoke - Stripe not wired in local env)
# ════════════════════════════════════════════════════════════════
section "Payment"

if [[ -z "$JWT_TOKEN" ]]; then
    skip "Create Stripe session (smoke)"
else
    req POST "$API/payment/checkout" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -H "Idempotency-Key: api-test-checkout" \
        -d '{"recipientName":"Olivia","recipientSurname":"Example"}'
    assert "Create Stripe checkout (smoke)" "200" "400" "404"

    req GET "$API/payment/checkout/00000000-0000-0000-0000-000000000000/status" \
        -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get checkout status missing/disabled (smoke)" "400" "404"

    req POST "$API/payment/stripe/webhook" \
        -H "Content-Type: application/json" \
        -d '{}'
    assert "Stripe webhook missing signature (rejected or disabled)" "400" "404"
fi

# ════════════════════════════════════════════════════════════════
#  LOGOUT
# ════════════════════════════════════════════════════════════════
section "Logout"

if [[ -z "$JWT_TOKEN" ]]; then
    skip "Logout"; skip "Token rejected after logout"
else
    LOGOUT_OK=false
    req POST "$API/auth/logout" -H "Authorization: Bearer $JWT_TOKEN"
    if [[ "$CODE" == "200" ]]; then
        LOGOUT_OK=true
        pass "Logout -> 200"
    else
        assert "Logout" "200"
    fi

    if [[ "$LOGOUT_OK" == true ]]; then
        req GET "$API/users" -H "Authorization: Bearer $JWT_TOKEN"
        assert "Token rejected after logout" "401" "403"
    else
        skip "Token rejected after logout" "logout did not complete"
    fi
fi

# ════════════════════════════════════════════════════════════════
#  SUMMARY DASHBOARD
# ════════════════════════════════════════════════════════════════
echo ""
echo -e "${BLD}${CYN}+--------------------------------------+${NC}"
echo -e "${BLD}${CYN}|         Test Results Summary         |${NC}"
echo -e "${BLD}${CYN}+--------------------------------------+${NC}"
printf "${BLD}${CYN}|${NC}  %-10s ${BLD}%s${NC}\n"  "Total:"   "$TOTAL"
printf "${BLD}${CYN}|${NC}  ${GRN}%-10s %s${NC}\n"  "Passed:"  "$PASSED"
printf "${BLD}${CYN}|${NC}  ${RED}%-10s %s${NC}\n"  "Failed:"  "$FAILED"
printf "${BLD}${CYN}|${NC}  ${YLW}%-10s %s${NC}\n"  "Skipped:" "$SKIPPED"
echo -e "${BLD}${CYN}+--------------------------------------+${NC}"

if [[ ${#FAILURES[@]} -gt 0 ]]; then
    echo -e "\n${RED}${BLD}Failed tests:${NC}"
    for f in "${FAILURES[@]}"; do
        echo -e "  ${RED}${FAIL} $f${NC}"
    done
fi

echo ""
if [[ "$FAILED" -eq 0 ]]; then
    echo -e "${GRN}${BLD}All tests passed! ${NC}"
    exit 0
else
    echo -e "${RED}${BLD}$FAILED test(s) failed.${NC}"
    exit 1
fi
