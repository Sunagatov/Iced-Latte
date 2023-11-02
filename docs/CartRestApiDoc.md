# CART REST API DOCUMENTATION

## 1. Get the shopping session (the cart info)

### HTTP GET .../api/v1/cart

**Summary:**  
Returns the shopping session (the cart info).

**Description:**  
This API returns the shopping session (the cart info) for authorized user.

### Request example:
GET [http://localhost:8083/api/v1/cart]()

### Response:
#### 200 OK  
If the client sends a valid request, 200 HTTP OK and shoppingSession (the cart) as HTTP response should be returned.

#### ShoppingSession
| Parameter        | Type       | Description                                     |
|------------------|------------|-------------------------------------------------|
| id               | UUID       | The identifier of the shoppingSession           |
| userId           | UUID       | The identifier of the user                      |
| items            | Collection | The list of items                               |
| itemsQuantity    | Integer    | The quantity of items                           |
| itemsTotalPrice  | BigDecimal | The total price of all items in shoppingSession |
| productsQuantity | Integer    | The quantity of products                        |
| createdAt        | Timestamp  | The time when shoppingSession was created       |
| closedAt         | Timestamp  | The time when shoppingSession was closed        |

#### Items
| Parameter        | Type        | Description                |
|------------------|-------------|----------------------------|
| id               | UUID        | The identifier of the item |
| productInfo      | productInfo | The product info           |
| productsQuantity | Integer     | The quantity of products   |         

#### 200 OK Response Example
```json
{
    "id": "b84e857f-132c-4f9d-8ed1-ae9ee759145b",
    "userId": "11111111-1111-1111-1111-111111111111",
    "items": [
        {
            "id": "6ec97c1e-4084-4e12-9e04-3ff15648c318",
            "productInfo": {
                "id": "e6a4d7f2-d40e-4e5f-93b8-5d56ce6724c5",
                "name": "Americano",
                "description": "Espresso Diluted with Hot Water",
                "price": 4.49,
                "quantity": 70,
                "active": true
            },
            "productsQuantity": 3
        },
        {
            "id": "ff6f15e6-9182-4a45-9c38-9c95f67c1f80",
            "productInfo": {
                "id": "46f97165-00a7-4b45-9e5c-09f8168b0047",
                "name": "Macchiato",
                "description": "Espresso with a Dash of Frothy Milk",
                "price": 3.99,
                "quantity": 90,
                "active": true
            },
            "productsQuantity": 1
        }
    ],
    "itemsQuantity": 2,
    "itemsTotalPrice": null,
    "productsQuantity": 4,
    "createdAt": "2023-07-21T14:00:00",
    "closedAt": "2023-07-21T16:00:00"
}
```
--------------------------------------------------------------------------

## 2. Add a new item to the cart

### HTTP POST .../api/v1/cart/items 

**Summary:**  
Returns shopping session (the caty info) with added a new item in the cart.

**Description:**  
This API receives a JSON object containing a shoppingSessionId and an productId. Returns 200 OK and the updated 
shoppingSession (the cart) as the confirmation of adding a new product to the cart.

### Request:
### Items
| Parameter        | Type    | Description                   | Required |
|------------------|---------|-------------------------------|----------|
| productId        | UUID    | The identifier of the product | true     |
| productsQuantity | Integer | The quantity of products      | true     |

### Request Example:
POST [http://localhost:8083/api/v1/cart/items]()
```json
{
    "items": [
        {
            "productId": "ad0ef2b7-816b-4a11-b361-dfcbe705fc96",
            "productsQuantity": 1
        }
    ]
}
```

### Responses:
#### 200 OK
If the client sends a valid request, 200 HTTP OK and updated shoppingSession (the cart) as HTTP response should be returned.

#### ShoppingSession
| Parameter        | Type       | Description                                     |
|------------------|------------|-------------------------------------------------|
| id               | UUID       | The identifier of the shoppingSession           |
| userId           | UUID       | The identifier of the user                      |
| items            | Collection | The list of items                               |
| itemsQuantity    | Integer    | The quantity of items                           |
| itemsTotalPrice  | BigDecimal | The total price of all items in shoppingSession |
| productsQuantity | Integer    | The quantity of products                        |
| createdAt        | Timestamp  | The time when shoppingSession was created       |
| closedAt         | Timestamp  | The time when shoppingSession was closed        |

#### Items                                                     
| Parameter        | Type        | Description                |
|------------------|-------------|----------------------------|
| id               | UUID        | The identifier of the item |
| productInfo      | productInfo | The product info           |
| productsQuantity | Integer     | The quantity of products   |

#### 200 OK Response Example:
```json
{                                                                    
    "id": "b84e857f-132c-4f9d-8ed1-ae9ee759145b",                    
    "userId": "11111111-1111-1111-1111-111111111111",                
    "items": [                                                       
        {                                                            
            "id": "6ec97c1e-4084-4e12-9e04-3ff15648c318",            
            "productInfo": {                                         
                "id": "e6a4d7f2-d40e-4e5f-93b8-5d56ce6724c5",        
                "name": "Americano",                                 
                "description": "Espresso Diluted with Hot Water",    
                "price": 4.49,                                       
                "quantity": 70,                                      
                "active": true                                       
            },                                                       
            "productsQuantity": 3                                    
        },                                                           
        {                                                            
            "id": "ff6f15e6-9182-4a45-9c38-9c95f67c1f80",            
            "productInfo": {                                         
                "id": "46f97165-00a7-4b45-9e5c-09f8168b0047",        
                "name": "Macchiato",                                 
                "description": "Espresso with a Dash of Frothy Milk",
                "price": 3.99,                                       
                "quantity": 90,                                      
                "active": true                                       
            },                                                       
            "productsQuantity": 1                                    
        },
        {
            "id": "51bb046a-5dac-4a71-b71c-a56e65a69dd4",
            "productInfo": {
                "id": "ad0ef2b7-816b-4a11-b361-dfcbe705fc96",
                "name": "Espresso",
                "description": "Strong and Intense Coffee Shot",
                "price": 4.99,
                "quantity": 120,
                "active": true
            },
            "productsQuantity": 1
        }
    ],                                                               
    "itemsQuantity": 3,                              
    "itemsTotalPrice": null,                              
    "productsQuantity": 5,                              
    "createdAt": "2023-07-21T14:00:00",                              
    "closedAt": "2023-07-21T16:00:00"                              
}                                                                    
```
------------------------------------------------------------------------------

## 3. Delete the list of items from the cart

### HTTP DELETE .../api/v1/cart/items

**Summary:**  
Returns the shopping session (the cart info) after deleting the specified list of items.

**Description:**  
This API receives a JSON object containing a list of shoppingSessionItemIds. Returns the updated shoppingSession (the cart). 

### Request:
| Parameter              | Type | Description                           | Required |
|------------------------|------|---------------------------------------|----------|
| shoppingSessionItemIds | List | The list of deleting item ids         | true     |

### Request Example:
DELETE [http://localhost:8083/api/v1/cart/items]()   
```json
{
    "shoppingSessionItemIds": 
        ["4ce6c88e-ec0b-4713-881f-8ee21b8c3209"]
}
```

### Responses:
#### 200 OK
If the client sends a valid request, 200 HTTP OK and updated shoppingSession as HTTP response should be returned.

#### ShoppingSession
| Parameter        | Type       | Description                                     |
|------------------|------------|-------------------------------------------------|
| id               | UUID       | The identifier of the shoppingSession           |
| userId           | UUID       | The identifier of the user                      |
| items            | Collection | The list of items                               |
| itemsQuantity    | Integer    | The quantity of items                           |
| itemsTotalPrice  | BigDecimal | The total price of all items in shoppingSession |
| productsQuantity | Integer    | the quantity of products                        |
| createdAt        | timestamp  | The time when shoppingSession was created at    |
| closedAt         | timestamp  | The time when shoppingSession was closed at     |

#### Items
| Parameter       | Type        | Description                |
|-----------------|-------------|----------------------------|
| id              | UUID        | The identifier of the item |
| productsInfo    | productInfo | The product info           |
| productQuantity | Integer     | The quantity of products   |

#### 200 OK Response Example:
```json
{                                                                    
    "id": "b84e857f-132c-4f9d-8ed1-ae9ee759145b",                    
    "userId": "11111111-1111-1111-1111-111111111111",                
    "items": [                                                       
        {                                                            
            "id": "6ec97c1e-4084-4e12-9e04-3ff15648c318",            
            "productInfo": {                                         
                "id": "e6a4d7f2-d40e-4e5f-93b8-5d56ce6724c5",        
                "name": "Americano",                                 
                "description": "Espresso Diluted with Hot Water",    
                "price": 4.49,                                       
                "quantity": 70,                                      
                "active": true                                       
            },                                                       
            "productsQuantity": 3                                    
        },                                                           
        {                                                            
            "id": "ff6f15e6-9182-4a45-9c38-9c95f67c1f80",            
            "productInfo": {                                         
                "id": "46f97165-00a7-4b45-9e5c-09f8168b0047",        
                "name": "Macchiato",                                 
                "description": "Espresso with a Dash of Frothy Milk",
                "price": 3.99,                                       
                "quantity": 90,                                      
                "active": true                                       
            },                                                       
            "productsQuantity": 1                                    
        }                                                            
    ],                                                               
    "itemsQuantity": 2,                                              
    "itemsTotalPrice": null,                                         
    "productsQuantity": 4,                                           
    "createdAt": "2023-07-21T14:00:00",                              
    "closedAt": "2023-07-21T16:00:00"                                
}                                                                    
```
 
#### 400 Bad Request   
in progress (invalid shoppingSessionItemId)

--------------------------------------------------------------------------

## 4. Update the amount of the specified item products in the cart

### HTTP PATCH .../api/v1/cart/items

**Summary:**  
Returns shopping session (the cart info) with updated amount of the specified items in the cart.

**Description:**  
This API receives a JSON object containing an shoppingSessionItemId and productQuantityChange (the change of products amount in the item).
Returns 200 OK and the updated shoppingSession (the cart) as the confirmation of updating the amount of the specified item products in the cart.

### Request:
| Parameter              | In                  | Type   | Required | Description                                           |
|------------------------|---------------------|--------|----------|-------------------------------------------------------|
| userId                 | body                | string | true     | The identifier of the user                            |
| shoppingSessionItemId  | url query attribute | string | true     | The identifier of the item                            |
| productsQuantityChange | body                | string | true     | The amount of the specified item products in the cart |

### Request Examples:
PATCH [http://localhost:8083/api/v1/cart/items]()   
```json
{
    "shoppingSessionItemId": "6ec97c1e-4084-4e12-9e04-3ff15648c318",
    "productsQuantityChange": -1
}
```
```json
{
    "shoppingSessionItemId": "ff6f15e6-9182-4a45-9c38-9c95f67c1f80",
    "productsQuantityChange": 1
}
```

### Responses:
#### 200 OK
If the client sends a valid request, 200 HTTP OK and updated shoppingSession as HTTP response should be returned.

#### ShoppingSession
| Parameter        | Type       | Description                                     |
|------------------|------------|-------------------------------------------------|
| id               | UUID       | The identifier of the shoppingSession           |
| userId           | UUID       | The identifier of the user                      |
| items            | Collection | The list of items                               |
| itemsQuantity    | Integer    | The quantity of items                           |
| itemsTotalPrice  | BigDecimal | The total price of all items in shoppingSession |
| productsQuantity | Integer    | The quantity of products                        |
| createdAt        | Timestamp  | The time when shoppingSession was created       |
| closedAt         | Timestamp  | The time when shoppingSession was closed        |
                                                                                   
#### Items
| Parameter        | Type        | Description                |
|------------------|-------------|----------------------------|
| id               | UUID        | The identifier of the item |
| productInfo      | productInfo | The product info           |
| productsQuantity | Integer     | The quantity of products   |

#### 200 OK Response Example:
```json
{                                                                    
    "id": "b84e857f-132c-4f9d-8ed1-ae9ee759145b",                    
    "userId": "11111111-1111-1111-1111-111111111111",                
    "items": [                                                       
        {                                                            
            "id": "6ec97c1e-4084-4e12-9e04-3ff15648c318",            
            "productInfo": {                                         
                "id": "e6a4d7f2-d40e-4e5f-93b8-5d56ce6724c5",        
                "name": "Americano",                                 
                "description": "Espresso Diluted with Hot Water",    
                "price": 4.49,                                       
                "quantity": 70,                                      
                "active": true                                       
            },                                                       
            "productsQuantity": 2                                    
        },                                                           
        {                                                            
            "id": "ff6f15e6-9182-4a45-9c38-9c95f67c1f80",            
            "productInfo": {                                         
                "id": "46f97165-00a7-4b45-9e5c-09f8168b0047",        
                "name": "Macchiato",                                 
                "description": "Espresso with a Dash of Frothy Milk",
                "price": 3.99,                                       
                "quantity": 90,                                      
                "active": true                                       
            },                                                       
            "productsQuantity": 2                                    
        }                                                            
    ],                                                               
    "itemsQuantity": 2,                                              
    "itemsTotalPrice": null,                                         
    "productsQuantity": 4,                                           
    "createdAt": "2023-07-21T14:00:00",                              
    "closedAt": "2023-07-21T16:00:00"                                
}                                                                    
```
 

#### 400 Bad Request
in progress (invalid shoppingSessionItemId)