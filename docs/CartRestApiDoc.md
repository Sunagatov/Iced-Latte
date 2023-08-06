# CART REST API DOCUMENTATION

## 1. Get the shopping session (the cart info)

### HTTP GET .../api/v1/users/{userId}/cart/

**Summary:**  
Returns the shopping session (the cart info).

**Description:**

This API receives userId as the url query attribute. Returns the shopping session (the cart info).

### Request:

| Parameter | In | Type  | Required | Description                                            |
|-----------|----|-------|----------|--------------------------------------------------------|
| userId   | url query attribute | string | true | The identifier of the user |

### Responses:

**200 OK**

If the client sends an valid request, 200 HTTP OK and ShoppingSession as HTTP response should be returned.

### ShoppingSession

| Parameter     | Type      | Required | Description                    |
|---------------|------------|----------|--------------------------------|
| shoppingSessionId   | string    | true     | The identifier of the shopping session (the cart info)   |
| itemsQuantity | integer   | true     | The quantity of items          |
| productsQuantity | integer   | true     | The quantity of products         |
| createdAt    | timestamp    | true     | The time when shoppingSession was created at   |
| closedAt    | timestamp    | true     | The time when shoppingSession was closed at  |
| items      | item list | true     | The item list                  |

### Item

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionItemId  | integer   | true     | The identifier of the item    |
| productsQuantity   | integer   | true     | The quantity of products      |
| productInfo            | integer   | true     | The product info       |

**Response Example:**

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemsQuantity": "2",
    "productsQuantity": "5",
    "createdAt": "2023-06-04 18:24:54",
    "closedAt": "2023-06-05 00:00:00",
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

--------------------------------------------------------------------------

## 2. Add a new item to the cart

### HTTP POST .../api/v1/cart/items/ 

**Summary:**  
Returns 200 OK and the updated shoppingSession (the cart) as the confirmation of adding a new product to the cart.

**Description:**

This API receives a JSON object containing a shoppingSessionId and an productId. Returns 200 OK and the updated 
shoppingSession (the cart) as the confirmation of adding a new product to the cart.

### Request:

| Parameter | In | Type  | Required | Description                 |
|-----------|----|-------|----------|-----------------------------|
| userId | body | string | true | The identifier of the user |
| productId | body | string | true | The identifier of the product |

**Request Example:**

POST /cart/items/

```json
{
    "userId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "productId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd"
}
```

### Responses:

**200 OK**

If the client sends an valid request, 200 HTTP OK and updated ShoppingSession as HTTP response should be returned.

### ShoppingSession

| Parameter     | Type      | Required | Description                    |
|---------------|------------|----------|--------------------------------|
| shoppingSessionId   | string    | true     | The identifier of the shopping session (the cart info)   |
| itemsQuantity | integer   | true     | The quantity of items          |
| productsQuantity | integer   | true     | The quantity of products         |
| createdAt    | timestamp    | true     | The time when shoppingSession was created at   |
| closedAt    | timestamp    | true     | The time when shoppingSession was closed at  |
| items      | item list | true     | The item list                  |

### Item

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionItemId  | integer   | true     | The identifier of the item    |
| productsQuantity   | integer   | true     | The quantity of products      |
| productInfo            | integer   | true     | The product info       |

**Response Example:**

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemsQuantity": "2",
    "productsQuantity": "5",
    "createdAt": "2023-06-04 18:24:54",
    "closedAt": "2023-06-05 00:00:00",
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

| Parameter     | Type      | Required | Description                    |
|---------------|------------|----------|--------------------------------|
| shoppingSessionId   | string    | true     | The identifier of the shopping session (the cart info)   |
| itemsQuantity | integer   | true     | The quantity of items          |
| productsQuantity | integer   | true     | The quantity of products         |
| createdAt    | timestamp    | true     | The time when shoppingSession was created at   |
| closedAt    | timestamp    | true     | The time when shoppingSession was closed at  |
| items      | item list | true     | The item list                  |

### Item

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionItemId  | integer   | true     | The identifier of the item    |
| productsQuantity   | integer   | true     | The quantity of products      |
| productInfo            | integer   | true     | The product info       |

**Response Example:**

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemsQuantity": "2",
    "productsQuantity": "5",
    "createdAt": "2023-06-04 18:24:54",
    "closedAt": "2023-06-05 00:00:00",
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

--------------------------------------------------------------------------

## 4. Update the amount of the specified item products in the cart

### PATCH POST .../api/v1/cart/items/{itemId}/amount

**Summary:**  
Returns 200 OK and the updated shoppingSession (the cart) 
as the confirmation of updating the amount of the specified item products in the cart

**Description:**

This API receives a JSON object containing a userId, an itemId and the change of products amount in the item.
Returns 200 OK and the updated shoppingSession (the cart)
as the confirmation of updating the amount of the specified item products in the cart

### Request:

| Parameter         | In | Type  | Required | Description                                            |
|-------------------|----|-------|----------|--------------------------------------------------------|
| userId | body| string | true | The identifier of the user                            |
| shoppingSessionItemId   | url query attribute | string | true | The identifier of the item                            |
| productsQuantityChange     | body | string | true | The amount of the specified item products in the cart |

**Request Example:**

PATCH .../api/v1/cart/items/{itemId}/amount

```json
{
    "userId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
    "productsQuantityChange": "-1"
}
```

```json
{
    "userId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
    "productsQuantityChange": "-7"
}
```

```json
{
    "userId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
    "productsQuantityChange": "+1"
}
```

```json
{
    "userId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "shoppingSessionItemId": "463463gfd-436fdhtgery3-hdhgdfghdgh46-hdhdghd",
    "productsQuantityChange": "+6"
}
```

### Responses:

**200 OK**

If the client sends an valid request, 200 HTTP OK and updated ShoppingSession as HTTP response should be returned.

### ShoppingSession

| Parameter     | Type      | Required | Description                    |
|---------------|------------|----------|--------------------------------|
| shoppingSessionId   | string    | true     | The identifier of the shopping session (the cart info)   |
| itemsQuantity | integer   | true     | The quantity of items          |
| productsQuantity | integer   | true     | The quantity of products         |
| createdAt    | timestamp    | true     | The time when shoppingSession was created at   |
| closedAt    | timestamp    | true     | The time when shoppingSession was closed at  |
| items      | item list | true     | The item list                  |

### Item

| Parameter          | Type      | Required | Description                   |
|--------------------|-----------|----------|-------------------------------|
| shoppingSessionItemId  | integer   | true     | The identifier of the item    |
| productsQuantity   | integer   | true     | The quantity of products      |
| productInfo            | integer   | true     | The product info       |

**Response Example:**

```json
{
    "shoppingSessionId": "fddfgd-fdgdfgdfh-hdfhdfh-436346dfhd-hdfhdf",
    "itemsQuantity": "2",
    "productsQuantity": "5",
    "createdAt": "2023-06-04 18:24:54",
    "closedAt": "2023-06-05 00:00:00",
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
