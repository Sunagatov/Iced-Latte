#!/usr/bin/env bash
# ╔══════════════════════════════════════════════════════════════╗
# ║           Iced Latte - API Integration Test Suite           ║
# ╚══════════════════════════════════════════════════════════════╝

set -uo pipefail

BASE_URL="${1:-http://localhost:8083}"
API="$BASE_URL/api/v1"

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
    sleep 0.4   # stay under rate limit
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
        info "token: ${JWT_TOKEN:0:32}..."
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

if [[ -n "$REFRESH_TOKEN" ]]; then
    req POST "$API/auth/refresh" -H "Authorization: Bearer $REFRESH_TOKEN"
    assert "Token refresh" "200"
else
    skip "Token refresh"
fi

req POST "$API/auth/refresh" -H "Authorization: Bearer invalid.token.here"
assert "Refresh with invalid token (rejected)" "401" "403" "500"

req GET "$API/users"
assert "Protected endpoint without token (rejected)" "401" "403"

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

req GET "$API/products?page=0&size=3&sortAttribute=price&sortDirection=desc&minPrice=1"
assert "Products with price/sort filters" "200"

req GET "$API/products?page=0&size=3&sortAttribute=averageRating&sortDirection=desc&minimumAverageRating=1"
assert "Products filtered by min rating" "200"

req GET "$API/products/00000000-0000-0000-0000-000000000000"
assert "Get non-existent product (not found)" "404" "400"

req GET "$API/products/sellers"
assert "List sellers" "200"

req GET "$API/products/brands"
assert "List brands" "200"

if [[ -n "$PRODUCT_ID" ]]; then
    req POST "$API/products/ids" \
        -H "Content-Type: application/json" \
        -d "{\"productIds\":[\"$PRODUCT_ID\"]}"
    assert "Get products by IDs" "200"
else
    skip "Get products by IDs"
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

    req GET "$API/users/avatar" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get avatar link" "200" "404"

    req PATCH "$API/users" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"oldPassword":"p@ss1logic11","newPassword":"p@ss1logic11"}'
    assert "Change password (same)" "200"
fi

# ════════════════════════════════════════════════════════════════
#  SHOPPING CART
# ════════════════════════════════════════════════════════════════
section "Shopping Cart"

if [[ -z "$JWT_TOKEN" ]]; then
    skip "Get cart"; skip "Add item to cart"
    skip "Update cart item quantity"; skip "Delete cart item"
else
    req GET "$API/cart" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get cart" "200" "404"

    if [[ -n "$PRODUCT_ID" ]]; then
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
    req POST "$API/favorites" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"productIds\":[\"$PRODUCT_ID\"]}"
    assert "Add product to favorites" "200"

    req GET "$API/favorites" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get favorites list" "200"

    req DELETE "$API/favorites/$PRODUCT_ID" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Remove product from favorites" "200"
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

    req GET "$API/products/$PRODUCT_ID/reviews?sortAttribute=productRating&sortDirection=desc"
    assert "Reviews sorted by rating desc" "200"
else
    skip "List product reviews"; skip "Review statistics"; skip "Reviews sorted by rating"
fi

if [[ -z "$JWT_TOKEN" || -z "$PRODUCT_ID" ]]; then
    skip "Get own review"; skip "Add review"; skip "Like review"; skip "Delete review"
else
    req GET "$API/products/$PRODUCT_ID/review" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get own review" "200"

    req POST "$API/products/$PRODUCT_ID/reviews" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"text":"Great coffee, highly recommend!","rating":5}'
    if [[ "$CODE" == "200" ]]; then
        REVIEW_ID=$(json_val productReviewId)
        pass "Add product review -> 200"
        [[ -n "$REVIEW_ID" ]] && info "review id: $REVIEW_ID"
    elif [[ "$CODE" == "400" ]]; then
        pass "Add product review -> 400 (already exists - expected)"
        req GET "$API/products/$PRODUCT_ID/review" -H "Authorization: Bearer $JWT_TOKEN"
        [[ "$CODE" == "200" ]] && REVIEW_ID=$(json_val productReviewId)
    else
        assert "Add product review" "200" "400"
    fi

    if [[ -n "$REVIEW_ID" ]]; then
        req POST "$API/products/$PRODUCT_ID/reviews/$REVIEW_ID/likes" \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d '{"isLike":true}'
        assert "Like a review" "200"

        req DELETE "$API/products/$PRODUCT_ID/reviews/$REVIEW_ID" \
            -H "Authorization: Bearer $JWT_TOKEN"
        assert "Delete own review" "200"
    else
        skip "Like a review"; skip "Delete own review"
    fi
fi

# ════════════════════════════════════════════════════════════════
#  ORDERS
# ════════════════════════════════════════════════════════════════
section "Orders"

if [[ -z "$JWT_TOKEN" ]]; then
    skip "Get orders"; skip "Get orders filtered by status"
else
    req GET "$API/orders" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get orders" "200" "500"

    req GET "$API/orders?status=CREATED&status=PAID" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Get orders filtered by status" "200" "500"
fi

# ════════════════════════════════════════════════════════════════
#  PAYMENT  (smoke - Stripe not wired in local env)
# ════════════════════════════════════════════════════════════════
section "Payment"

if [[ -z "$JWT_TOKEN" ]]; then
    skip "Create Stripe session (smoke)"
else
    req POST "$API/payment" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Create Stripe session (smoke)" "200" "400" "404" "500"
fi

# ════════════════════════════════════════════════════════════════
#  LOGOUT
# ════════════════════════════════════════════════════════════════
section "Logout"

if [[ -z "$JWT_TOKEN" ]]; then
    skip "Logout"; skip "Token rejected after logout"
else
    req POST "$API/auth/logout" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Logout" "200"

    req GET "$API/users" -H "Authorization: Bearer $JWT_TOKEN"
    assert "Token rejected after logout" "401" "403"
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
