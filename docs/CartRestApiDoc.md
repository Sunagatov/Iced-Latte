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

| Parameter     | Type      | Required | Description                    |
|---------------|------------|----------|--------------------------------|
| shoppingSessionId   | string    | true     | The identifier of the shopping session (the cart info)   |
| itemsQuantity | integer   | true     | The quantity of items          |
| totalPrice    | number    | true     | The total price of all items   |
| items      | item list | true     | The item list                  |

### Item

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionItemId                 | integer   | true     | The identifier of the item    |
| productsQuantity   | integer   | true     | The quantity of products      |
| totalProductsPrice | integer   | true     | The total products price      |
| productInfo            | integer   | true     | The product info       |

**Response Example:**

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemsQuantity": "2",
    "totalPrice": "3200",
    "items": [
        {
            "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsQuantity": "2",
            "totalProductsPrice": "1500",
            "productInfo": {
                "productInfo": "productData"
            }
        },
        {
            "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsQuantity": "3",
            "totalProductsPrice": "1700",
            "productInfo": {
                "productInfo": "productData"
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
    "code": 12 // TODO Create errorCode hierachy
}
```

```json
{
    "message": "Invalid 'shoppingSessionId' value.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 13 // TODO Create errorCode hierachy
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
| shoppingSessionItemId | body | string | true | The identifier of the item |

**Request Example:**

POST /cart/items/

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd"
}
```

### Responses:

**200 OK**

If the client sends an valid request, 200 HTTP OK and updated ShoppingSession as HTTP response should be returned.

### ShoppingSession

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionId                 | string    | true     | The identifier of the shopping session (the cart info)   |
| itemsQuantity      | integer   | true     | The quantity of items          |
| totalPrice         | number    | true     | The total price of all items |
| items           | item list | true     | The item list                |

### Item

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionItemId                 | integer   | true     | The identifier of the item    |
| productsQuantity   | integer   | true     | The quantity of products      |
| totalProductsPrice | integer   | true     | The total products price      |
| productInfo          | integer   | true     | The product description       |

**Response Example:**

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemsQuantity": "2",
    "totalPrice": "3200",
    "items": [
        {
            "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsQuantity": "2",
            "totalProductsPrice": "1500",
            "productInfo": {
                "productData": "productData"
            }
        },
        {
            "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsQuantity": "3",
            "totalProductsPrice": "1700",
            "productInfo": {
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
    "message": "The 'shoppingSessionId' cannot be empty",
    "timestamp": "2023-06-04 18:24:54",
    "code": 12 // TODO Create errorCode hierachy
}
```

```json
{
    "message": "Invalid 'shoppingSessionId' value",
    "timestamp": "2023-06-04 18:24:54",
    "code": 14 // TODO Create errorCode hierachy
}
```

```json
{
    "message": "The 'itemId' cannot be empty",
    "timestamp": "2023-06-04 18:24:54",
    "code": 15 // TODO Create errorCode hierachy
}
```

```json
{
    "message": "Invalid 'itemId' value",
    "timestamp": "2023-06-04 18:24:54",
    "code": 16 // TODO Create errorCode hierachy
}
```

------------------------------------------------------------------------------

## 3. Remove the item in the cart

### HTTP DELETE .../api/v1/cart/{shoppingSessionId}/items/

**Summary:**  
Returns 200 OK as the confirmation of deleting the specified item in the cart.

**Description:**

This API receives a JSON object containing a shoppingSessionId and an items. Returns 200 OK as the confirmation of
deleting the given items in the cart.

### Request:

| Parameter         | In | Type      | Required | Description                 |
|-------------------|----|-----------|----------|-----------------------------|
| shoppingSessionId | url query attribute | string    | true | The identifier of the cart |
| items             | body | item list | true | The item identifiers list  |

**Request Example:**

DELETE /cart/items/

```json
{
    "items": [
        {
            "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd"
        },
        {
            "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd"
        }
    ]
}
```

### Responses:

**200 OK**

If the client sends a valid request, 200 HTTP OK and updated ShoppingSession as HTTP response should be returned.

### ShoppingSession

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionId  | string    | true | The identifier of the shopping session (the cart info)   |
| itemsQuantity      | integer   | true | The quantity of items          |
| totalPrice         | number    | true | The total price of all items |
| items           | item list | true | The item list                |

### Item

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionItemId             | integer   | true | The identifier of the item    |
| productsQuantity     | integer   | true | The quantity of products      |
| totalProductsPrice | integer   | true | The total products price     |
| productInfo          | integer   | true | The product description       |

**Response Example:**

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemsQuantity": "2",
    "totalPrice": "3200",
    "items": [
        {
            "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsQuantity": "2",
            "totalProductsPrice": "1500",
            "productInfo": {
                "productData": "productData"
            }
        },
        {
            "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsQuantity": "3",
            "totalProductsPrice": "1700",
            "productInfo": {
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
    "message": "The 'items' cannot be empty.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 14
}
```

```json
{
    "message": "Invalid 'items' value.",
    "timestamp": "2023-06-04 18:24:54",
    "code": 15
}
```

--------------------------------------------------------------------------

## 4. Update the amount of the specified item products in the cart

### PATCH POST .../api/v1/cart/{shoppingSessionId}/items/{itemId}/amount

**Summary:**  
Returns 200 OK as the confirmation of changing The amount of the specified item products in the cart

**Description:**

This API receives a JSON object containing a shoppingSessionId, an itemId and the change of products amount in the item.
Returns 200 OK as the confirmation of changing The amount of the specified item products in the cart

### Request:

| Parameter         | In | Type  | Required | Description                                            |
|-------------------|----|-------|----------|--------------------------------------------------------|
| shoppingSessionId | url query attribute | string | true | The identifier of the cart                            |
| shoppingSessionItemId   | url query attribute | string | true | The identifier of the item                            |
| productsQuantityChange     | body | string | true | The amount of the specified item products in the cart |

**Request Example:**

PATCH .../api/v1/cart/items/{itemId}/amount

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
    "productsQuantityChange": "-1"
}
```

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
    "productsQuantityChange": "-7"
}
```

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
    "productsQuantityChange": "+1"
}
```

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
    "productsQuantityChange": "+6"
}
```

### Responses:

**200 OK**

If the client sends an valid request, 200 HTTP OK and updated ShoppingSession as HTTP response should be returned.

### ShoppingSession

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionId  | string    | true | The identifier of the shopping session (the cart info)   |
| itemsQuantity        | integer   | true | The quantity of items          |
| totalPrice         | number    | true | The total price of all items |
| items           | item list | true | The item list                |

### Item

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionItemId             | integer   | true | The identifier of the item    |
| productsQuantity     | integer   | true | The quantity of products      |
| totalProductsPrice | integer   | true | The total products price     |
| productInfo          | integer   | true | The product description       |

**Response Example:** 

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemsQuantity": "2",
    "totalPrice": "3200",
    "items": [
        {
            "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsQuantity": "2",
            "totalProductsPrice": "1500",
            "productInfo": {
                "productData": "productData"
            }
        },
        {
            "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
            "productsQuantity": "3",
            "totalProductsPrice": "1700",
            "productInfo": {
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
