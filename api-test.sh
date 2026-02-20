#!/bin/bash

BASE_URL=${1:-"http://localhost:8083"}
API_BASE="$BASE_URL/api/v1"

readonly GREEN='\033[0;32m'
readonly RED='\033[0;31m'
readonly NC='\033[0m'
readonly SLEEP_BETWEEN_TESTS=2

JWT_TOKEN=""
REFRESH_TOKEN=""

http_code_of() { echo "${1: -3}"; }
body_of()      { echo "${1:0:${#1}-3}"; }

pass() { echo -e "${GREEN}✓ $1${NC}"; }
fail() { echo -e "${RED}✗ $1${NC}"; }

test_login() {
    echo "Testing login..."
    body_file=$(mktemp) || { fail "Failed to create temp file"; return 1; }
    err_file=$(mktemp)  || { fail "Failed to create temp file"; return 1; }

    http_code=$(curl -s -X POST "$API_BASE/auth/authenticate" \
        -H "Content-Type: application/json" \
        -d '{"email": "olivia@example.com", "password": "p@ss1logic11"}' \
        -w "%{http_code}" -o "$body_file" 2>"$err_file")
    curl_exit=$?
    body=$(cat "$body_file")
    rm -f "$body_file" "$err_file"

    if [[ $curl_exit -ne 0 ]]; then
        fail "Login request failed (curl error $curl_exit)"
        echo "Error: $body"
        return 1
    fi

    if [[ "$http_code" == "200" ]]; then
        JWT_TOKEN=$(echo "$body" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        REFRESH_TOKEN=$(echo "$body" | grep -o '"refreshToken":"[^"]*' | cut -d'"' -f4)
        if [[ -z "$JWT_TOKEN" ]]; then
            fail "Login succeeded but token extraction failed"
            echo "Response: $body"
        else
            pass "Login successful"
            echo "Token: ${JWT_TOKEN:0:30}..."
        fi
    else
        fail "Login failed (HTTP $http_code)"
        echo "Response: $body"
    fi
}

test_products() {
    echo "Testing products..."
    response=$(curl -s -w "%{http_code}" "$API_BASE/products?page=0&size=3")
    if [[ $? -ne 0 ]]; then
        fail "Products request failed (curl error)"
        return 1
    fi

    http_code=$(http_code_of "$response")
    if [[ "$http_code" == "200" ]]; then
        pass "Products retrieved"
    else
        fail "Products failed (HTTP $http_code)"
        echo "Response: $(body_of "$response")"
    fi
}

test_refresh() {
    if [[ -z "$REFRESH_TOKEN" ]]; then
        fail "No refresh token for refresh test"
        return 1
    fi

    echo "Testing refresh endpoint..."
    response=$(curl -s -w "%{http_code}" -X POST "$API_BASE/auth/refresh" \
        -H "Authorization: Bearer $REFRESH_TOKEN" \
        -H "Content-Type: application/json")

    http_code=$(http_code_of "$response")
    if [[ "$http_code" == "200" ]]; then
        pass "Token refresh successful"
    else
        fail "Token refresh failed (HTTP $http_code)"
        echo "Response: $(body_of "$response")"
    fi
}

test_user_profile() {
    if [[ -z "$JWT_TOKEN" ]]; then
        fail "No token for user profile test"
        return 1
    fi

    echo "Testing user profile endpoint..."
    response=$(curl -s -w "%{http_code}" "$API_BASE/users" \
        -H "Authorization: Bearer $JWT_TOKEN")
    if [[ $? -ne 0 ]]; then
        fail "User profile curl failed"
        return 1
    fi

    http_code=$(http_code_of "$response")
    if [[ "$http_code" == "200" ]]; then
        pass "User profile retrieved"
    else
        fail "User profile failed (HTTP $http_code)"
        echo "Response: $(body_of "$response")"
    fi
}

test_reviews() {
    if [[ -z "$JWT_TOKEN" ]]; then
        fail "No token for reviews test"
        return 1
    fi

    echo "Testing reviews endpoints..."

    products=$(curl -s -w "%{http_code}" "$API_BASE/products?page=0&size=1")
    curl_exit=$?
    if [[ $curl_exit -ne 0 ]]; then
        fail "Failed to fetch products for reviews test (curl error $curl_exit)"
        return 1
    fi

    products_body=$(body_of "$products")
    product_id=$(echo "$products_body" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

    if [[ -z "$product_id" ]]; then
        fail "No product ID found for reviews test"
        return 1
    fi

    response=$(curl -s -w "%{http_code}" -X GET "$API_BASE/products/$product_id/reviews" \
        -H "Authorization: Bearer $JWT_TOKEN")
    http_code=$(http_code_of "$response")

    if [[ "$http_code" == "200" ]]; then
        pass "Get product reviews"
    else
        fail "Get product reviews failed (HTTP $http_code)"
        echo "Response: $(body_of "$response")"
    fi
}

test_logout() {
    if [[ -z "$JWT_TOKEN" ]]; then
        fail "No token for logout"
        return 1
    fi

    echo "Testing logout..."
    response=$(curl -s -w "%{http_code}" -X POST "$API_BASE/auth/logout" \
        -H "Authorization: Bearer $JWT_TOKEN")

    http_code=$(http_code_of "$response")
    if [[ "$http_code" == "200" ]]; then
        pass "Logout successful"
    else
        fail "Logout failed (HTTP $http_code)"
        echo "Response: $(body_of "$response")"
    fi
}

echo "=== Complete API Test ==="
test_login
sleep $SLEEP_BETWEEN_TESTS
test_products
sleep $SLEEP_BETWEEN_TESTS
test_refresh
sleep $SLEEP_BETWEEN_TESTS
test_user_profile
sleep $SLEEP_BETWEEN_TESTS
test_reviews
sleep $SLEEP_BETWEEN_TESTS
test_logout
echo "=== Done ==="
