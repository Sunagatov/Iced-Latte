### Payment endpoints
<ol>
<li>Payment creation and processing: POST `/api/v1/payment`</li>
<ul>
<li>When the request body is in the required format then the response code is 200:</li>
<pre>
<code>
{
  "paymentMethodId": string,
  "priceDetails" : {
    "totalPrice" : float,
    "currency" : string
  }
}
</code>
</pre>

<li>When one or more parameters in request are blank then returned response code 400 with description</li>
<li>When the parameter `paymentMethodId` contains a non-existent value then the response code is 400 with a description</li>
<li>When the parameter `paymentMethodId` are blank then the response code is 400 with description</li>
<li>When the parameter `priceDetails.totalPrice` is less than 0 then the response code is 400 with a description</li>
</ul>

<li>The details of a particular payment transaction: GET `/api/v1/payment/{paymentId}`</li>
<ul>
<li>When `paymentId` is the existing value then the response code is 200 with data:</li>
<pre>
<code>
{
  "paymentId": integer,
  "totalPrice": float,
  "paymentIntentId": string,
  "currency": string,
  "status": string,
  "description": string
}
</code>
</pre>
<li>When `paymentId` non-existent value then the response code is 400 with description</li>
</ul>
<li>Getting a payment method token: POST `/api/v1/payment/method`</li>
<ul>
<li>When the request body is in the required format then the response code is 200:</li>
<pre>
<code>
{
  "cardNumber" : string,
  "expMonth" : integer,
  "expYear" : integer,
  "cvc" : string
}
</code>
</pre>
<li>When one or more parameters in request are blank then returned response code 400 with description</li>
<li>When the parameters `expMonth` and `expYear` are contains value before now date then returned response code 400 with description</li>
</ul>
</ol>