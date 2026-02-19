#!/bin/bash

BASE_URL=${1:-"http://localhost:8083"}
API_BASE="$BASE_URL/api/v1"

readonly GREEN='\033[0;32m'
readonly RED='\033[0;31m'
readonly NC='\033[0m'

JWT_TOKEN=""

test_login() {
    echo "Testing login..."
    body_file=$(mktemp)
    http_code=$(curl -s -X POST "$API_BASE/auth/authenticate" \
        -H "Content-Type: application/json" \
        -d '{"email": "olivia@example.com", "password": "p@ss1logic11"}' \
        -w "%{http_code}" -o "$body_file" 2>&1)
    curl_exit=$?
    body=$(cat "$body_file")
    rm -f "$body_file"
    if [[ $curl_exit -ne 0 ]]; then
        echo -e "${RED}✗ Login request failed (curl error)${NC}"
        echo "Error: $body"
        return
    fi
    
    if [[ "$http_code" == "200" ]]; then
        JWT_TOKEN=$(echo "$body" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        if [[ -z "$JWT_TOKEN" ]]; then
            echo -e "${RED}✗ Login succeeded but token extraction failed${NC}"
            echo "Response: $body"
        else
            echo -e "${GREEN}✓ Login successful${NC}"
            echo "Token: ${JWT_TOKEN:0:30}..."
        fi
    else
        echo -e "${RED}✗ Login failed (HTTP $http_code)${NC}"
        echo "Response: $body"
    fi
}

test_products() {
    echo "Testing products..."
    response=$(curl -s -w "%{http_code}" "$API_BASE/products?page=0&size=3")
    http_code="${response: -3}"
    
    if [[ "$http_code" == "200" ]]; then
        echo -e "${GREEN}✓ Products retrieved${NC}"
    else
        echo -e "${RED}✗ Products failed (HTTP $http_code)${NC}"
    fi
}

test_refresh() {
    if [[ -z "$JWT_TOKEN" ]]; then
        echo -e "${RED}✗ No token for refresh test${NC}"
        return
    fi
    
    echo "Testing refresh endpoint..."
    response=$(curl -s -w "%{http_code}" -X POST "$API_BASE/auth/refresh" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json")
    
    http_code="${response: -3}"
    
    if [[ "$http_code" == "200" ]]; then
        echo -e "${GREEN}✓ Token refresh successful${NC}"
    else
        echo -e "${RED}✗ Token refresh failed (HTTP $http_code)${NC}"
    fi
}

test_redis_endpoints() {
    if [[ -z "$JWT_TOKEN" ]]; then
        echo -e "${RED}✗ No token for Redis endpoints test${NC}"
        return
    fi
    
    echo "Testing user profile endpoint..."
    response=$(curl -s -w "%{http_code}" -X GET "$API_BASE/users" \
        -H "Authorization: Bearer $JWT_TOKEN")
    
    http_code="${response: -3}"
    
    if [[ "$http_code" == "200" ]]; then
        echo -e "${GREEN}✓ User profile retrieved${NC}"
    else
        echo -e "${RED}✗ User profile failed (HTTP $http_code)${NC}"
    fi
}

test_reviews() {
    if [[ -z "$JWT_TOKEN" ]]; then
        echo -e "${RED}✗ No token for reviews test${NC}"
        return
    fi
    
    echo "Testing reviews endpoints..."
    
    # Get product ID for review test
    products=$(curl -s -w "%{http_code}" "$API_BASE/products?page=0&size=1")
    curl_exit=$?
    if [[ $curl_exit -ne 0 ]]; then
        echo -e "${RED}✗ Failed to fetch products for reviews test (curl error)${NC}"
        return
    fi
    products_http_code="${products: -3}"
    products_body="${products:0:${#products}-3}"
    product_id=$(echo "$products_body" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
    
    if [[ -n "$product_id" ]]; then
        # Get reviews for product
        response=$(curl -s -w "%{http_code}" -X GET "$API_BASE/products/$product_id/reviews" \
            -H "Authorization: Bearer $JWT_TOKEN")
        http_code="${response: -3}"
        
        if [[ "$http_code" == "200" ]]; then
            echo -e "${GREEN}✓ Get product reviews${NC}"
        else
            echo -e "${RED}✗ Get product reviews failed (HTTP $http_code)${NC}"
        fi
    else
        echo -e "${RED}✗ No product ID found for reviews test${NC}"
    fi
}

test_logout() {
    if [[ -z "$JWT_TOKEN" ]]; then
        echo -e "${RED}✗ No token for logout${NC}"
        return
    fi
    
    echo "Testing logout..."
    response=$(curl -s -w "%{http_code}" -X POST "$API_BASE/auth/logout" \
        -H "Authorization: Bearer $JWT_TOKEN")
    
    http_code="${response: -3}"
    
    if [[ "$http_code" == "200" ]]; then
        echo -e "${GREEN}✓ Logout successful${NC}"
    else
        echo -e "${RED}✗ Logout failed (HTTP $http_code)${NC}"
    fi
}

echo "=== Complete API Test ==="
test_login
sleep 2
test_products
sleep 2
test_refresh
sleep 2
test_redis_endpoints
sleep 2
test_reviews
sleep 2
test_logout
echo "=== Done ==="