# CART REST API DOCUMENTATION

## 1. Get the shopping session (the cart info)

### HTTP GET .../api/v1/cart/{shoppingSessionId}

**Summary:**  
Returns the shopping session (the cart info).

**Description:**

This API receives a JSON object containing a shoppingSessionId. Returns the shopping session (the cart info).

### Request:

| Parameter | In | Type  | Required | Description                                            |
|-----------|----|-------|----------|--------------------------------------------------------|
| shoppingSessionId   | url query attribute | string | true | The identifier of the cart |

### Responses:

**200 OK**

If the client sends an valid request, 200 HTTP OK and ShoppingSession as HTTP response should be returned.

### ShoppingSession

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionId  | string    | true | The identifier of the shopping session (the cart info)   |
| itemsAmount        | integer   | true | The amount of items          |
| totalPrice         | number    | true | The total price of all items |
| itemList           | item list | true | The item list                |

### Item

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| itemId             | integer   | true | The identifier of the item    |
| productsAmount     | integer   | true | The amount of products      |
| totalProductsPrice | integer   | true | The total products price     |
| product            | integer   | true | The product description       |

**Response Example:**

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemsAmount": "2",
    "totalPrice": "3200",
    "itemList": [
        {
            "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsAmount": "2",
            "totalProductsPrice": "1500",
            "product": {
                "productData": "productData"
            }
        },
        {
            "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsAmount": "3",
            "totalProductsPrice": "1700",
            "product": {
                "productData": "productData"
            }
        }
    ]
}
```

**400 Bad Request**

If the client sends an invalid request, a 400 Bad Request should be returned.

| Parameter | Type      | Required | Description                                         |
|-----------|-----------|----------|-----------------------------------------------------|
| message | string    | true | Error message indicating the reason for the failure |
| timestamp | timestamp | true | Indicating the failure time                        |
| code | integer   | true | Error code    |

**Response Examples:**

```json
{
    "message": "The 'shoppingSessionId' cannot be empty.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 12
}
```

```json
{
    "message": "Invalid 'shoppingSessionId' value.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 13
}
```

--------------------------------------------------------------------------

## 2. Add a new item to the cart

### HTTP POST .../api/v1/cart/items/

**Summary:**  
Returns 200 OK as the confirmation of adding a new item to the cart.

**Description:**

This API receives a JSON object containing a shoppingSessionId and an itemId. Returns 200 OK as the confirmation of
adding a new item to the cart.

### Request:

| Parameter | In | Type  | Required | Description                 |
|-----------|----|-------|----------|-----------------------------|
| shoppingSessionId | body | string | true | The identifier of the cart |
| itemId | body | string | true | The identifier of the item |

**Request Example:**

POST /cart/items/

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd"
}
```

### Responses:

**200 OK**

If the client sends an valid request, 200 HTTP OK and updated ShoppingSession as HTTP response should be returned.

### ShoppingSession

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionId  | string    | true | The identifier of the shopping session (the cart info)   |
| itemsAmount        | integer   | true | The amount of items          |
| totalPrice         | number    | true | The total price of all items |
| itemList           | item list | true | The item list                |

### Item

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| itemId             | integer   | true | The identifier of the item    |
| productsAmount     | integer   | true | The amount of products      |
| totalProductsPrice | integer   | true | The total products price     |
| product            | integer   | true | The product description       |

**Response Example:**

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemsAmount": "2",
    "totalPrice": "3200",
    "itemList": [
        {
            "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsAmount": "2",
            "totalProductsPrice": "1500",
            "product": {
                "productData": "productData"
            }
        },
        {
            "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsAmount": "3",
            "totalProductsPrice": "1700",
            "product": {
                "productData": "productData"
            }
        }
    ]
}
```

**400 Bad Request**

If the client sends an invalid request, a 400 Bad Request should be returned.

| Parameter | Type      | Required | Description                                         |
|-----------|-----------|----------|-----------------------------------------------------|
| message | string    | true | Error message indicating the reason for the failure. |
| timestamp | timestamp | true | Indicating the failure time.                        |
| code | integer   | true | Error code.    |

**Response Examples:**

```json
{
    "message": "The 'shoppingSessionId' cannot be empty.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 12
}
```

```json
{
    "message": "Invalid 'shoppingSessionId' value.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 14
}
```

```json
{
    "message": "The 'itemId' cannot be empty.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 15
}
```

```json
{
    "message": "Invalid 'itemId' value.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 16
}
```

------------------------------------------------------------------------------

## 3. Remove the item in the cart

### HTTP DELETE .../api/v1/cart/items/

**Summary:**  
Returns 200 OK as the confirmation of deleting the specified item in the cart.

**Description:**

This API receives a JSON object containing a shoppingSessionId and an itemList. Returns 200 OK as the confirmation of
deleting the given itemList in the cart.

### Request:

| Parameter         | In | Type      | Required | Description                 |
|-------------------|----|-----------|----------|-----------------------------|
| shoppingSessionId | body | string    | true | The identifier of the cart |
| itemList          | body | item list | true | The item identifiers list  |

**Request Example:**

POST /cart/items/

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemList": [
        {
            "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd"
        },
        {
            "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd"
        }
    ]
}
```

### Responses:

**200 OK**

If the client sends an valid request, 200 HTTP OK and updated ShoppingSession as HTTP response should be returned.

### ShoppingSession

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionId  | string    | true | The identifier of the shopping session (the cart info)   |
| itemsAmount        | integer   | true | The amount of items          |
| totalPrice         | number    | true | The total price of all items |
| itemList           | item list | true | The item list                |

### Item

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| itemId             | integer   | true | The identifier of the item    |
| productsAmount     | integer   | true | The amount of products      |
| totalProductsPrice | integer   | true | The total products price     |
| product            | integer   | true | The product description       |

**Response Example:**

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemsAmount": "2",
    "totalPrice": "3200",
    "itemList": [
        {
            "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsAmount": "2",
            "totalProductsPrice": "1500",
            "product": {
                "productData": "productData"
            }
        },
        {
            "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsAmount": "3",
            "totalProductsPrice": "1700",
            "product": {
                "productData": "productData"
            }
        }
    ]
}
```

**400 Bad Request**

If the client sends an invalid request, a 400 Bad Request should be returned.

| Parameter | Type      | Required | Description                                         |
|-----------|-----------|----------|-----------------------------------------------------|
| message | string    | true | Error message indicating the reason for the failure. |
| timestamp | timestamp | true | Indicating the failure time.                        |
| code | integer   | true | Error code.    |

**Response Examples:**

```json
{
    "message": "The 'shoppingSessionId' cannot be empty.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 12
}
```

```json
{
    "message": "Invalid 'shoppingSessionId' value.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 13
}
```

```json
{
    "message": "The 'itemList' cannot be empty.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 14
}
```

```json
{
    "message": "Invalid 'itemList' value.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 15
}
```

--------------------------------------------------------------------------

## 4. Update the amount of the specified item products in the cart

### HTTP POST .../api/v1/cart/items/amount

**Summary:**  
Returns 200 OK as the confirmation of changing The amount of the specified item products in the cart

**Description:**

This API receives a JSON object containing a shoppingSessionId, an itemId and the change of products amount in the item.
Returns 200 OK as the confirmation of changing The amount of the specified item products in the cart

### Request:

| Parameter         | In | Type  | Required | Description                                            |
|-------------------|----|-------|----------|--------------------------------------------------------|
| shoppingSessionId | body | string | true | The identifier of the cart                            |
| itemId            | body | string | true | The identifier of the item                            |
| change            | body | string | true | The amount of the specified item products in the cart |

**Request Example:**

POST /cart/items/

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
    "change": "-1"
}
```

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
    "change": "-7"
}
```

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
    "change": "+1"
}
```

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
    "change": "+6"
}
```

### Responses:

**200 OK**

If the client sends an valid request, 200 HTTP OK and updated ShoppingSession as HTTP response should be returned.

### ShoppingSession

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionId  | string    | true | The identifier of the shopping session (the cart info)   |
| itemsAmount        | integer   | true | The amount of items          |
| totalPrice         | number    | true | The total price of all items |
| itemList           | item list | true | The item list                |

### Item

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| itemId             | integer   | true | The identifier of the item    |
| productsAmount     | integer   | true | The amount of products      |
| totalProductsPrice | integer   | true | The total products price     |
| product            | integer   | true | The product description       |

**Response Example:**

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemsAmount": "2",
    "totalPrice": "3200",
    "itemList": [
        {
            "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsAmount": "2",
            "totalProductsPrice": "1500",
            "product": {
                "productData": "productData"
            }
        },
        {
            "itemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsAmount": "3",
            "totalProductsPrice": "1700",
            "product": {
                "productData": "productData"
            }
        }
    ]
}
```

**400 Bad Request**

If the client sends an invalid request, a 400 Bad Request should be returned.

| Parameter | Type      | Required | Description                                         |
|-----------|-----------|----------|-----------------------------------------------------|
| message | string    | true | Error message indicating the reason for the failure. |
| timestamp | timestamp | true | Indicating the failure time.                        |
| code | integer   | true | Error code.    |

**Response Examples:**

```json
{
    "message": "The 'shoppingSessionId' cannot be empty.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 12
}
```

```json
{
    "message": "Invalid 'shoppingSessionId' value.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 13
}
```

```json
{
    "message": "The 'itemId' cannot be empty.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 14
}
```

```json
{
    "message": "Invalid 'itemId' value.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 15
}
```
