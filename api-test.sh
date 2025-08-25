#!/bin/bash

BASE_URL=${1:-"http://localhost:8083"}
API_BASE="$BASE_URL/api/v1"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

JWT_TOKEN=""

test_login() {
    echo "Testing login..."
    response=$(curl -s -w "%{http_code}" -X POST "$API_BASE/auth/authenticate" \
        -H "Content-Type: application/json" \
        -d '{"email": "olivia@example.com", "password": "p@ss1logic11"}')
    
    http_code="${response: -3}"
    body="${response%???}"
    
    if [[ "$http_code" == "200" ]]; then
        JWT_TOKEN=$(echo "$body" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        echo -e "${GREEN}✓ Login successful${NC}"
        echo "Token: ${JWT_TOKEN:0:30}..."
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
    products=$(curl -s "$API_BASE/products?page=0&size=1")
    product_id=$(echo "$products" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
    
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
test_products
test_refresh
test_redis_endpoints
test_reviews
test_logout
echo "=== Done ==="