<ol>
  <li>
    Auth endpoints
    <ol>
      <li>
        Register user: POST `/api/auth/register`
        <ul>
          <li>Body for request must be provided in the following format:</li>
        </ul>
        <pre>
          <code>
          {
            "firstName": "David",
            "lastName": "Kalachyan",
            "username": "david",
            "password": "david",
            "address": {
              "line": "line",
              "city": "city",
              "country": "country"
            },
            "email": "email@example.com"
          }
          </code>
        </pre>
        <ul>
          <li>All parameters are mandatory - If one or more parameters are blank, the corresponding message in the response must be returned with a 400 code response.</li>
          <li>Email parameter must be provided in the following format: "some@some.com" - If this parameter is in the wrong format, the corresponding message in the response must be returned with a 400 code response.</li>
          <li>Authorization not needed for this endpoint - It is possible to perform the request with no authorization.</li>
          <li>If all parameters are provided in the correct format, the response must return a JWT token for authorizing this user.</li>
          <li>Users can authorize all next requests without performing additional AUTH requests (e.g., GET user by Id).</li>
          <li>Users can authorize with registered username and password via the AUTH endpoint and obtain a JWT token.</li>
        </ul>
      </li>
      <li>
        Auth user: POST `/api/auth/authenticate`
        <ul>
          <li>It is possible to get a JWT token by providing the username and password in the body in the following format:</li>
        </ul>
        <pre>
          <code>
          {
            "username": "{{username}}",
            "password": "{{password}}"
          }
          </code>
        </pre>
        <ul>
          <li>All these parameters are mandatory - If one or more parameters are blank, a 403 response code must be returned with no data (user enumeration isn't possible).</li>
        </ul>
      </li>
      <li>
        Logout user: POST `/api/auth/logout`
        <ul>
          <li>It is possible to perform the request with an existing and not expired JWT token.</li>
          <li>After performing this request, the JWT token must become invalid, and it isn't possible to perform all secured endpoints with them.</li>
        </ul>
      </li>
    </ol>
  </li>
  <li>
    User endpoints
    <ol>
      <li>
        Get user by ID: GET `/api/v1/users/{userId}`
        <ul>
          <li>It is possible to get already existing user data by ID.</li>
          <li>It is possible to get just created user data by ID.</li>
          <li>Every existing user data must be returned by ID.</li>
          <li>It isn't possible to get user data by ID if the request isn't authorized.</li>
          <li>If an unauthorized request performs a 403 response code must be returned and no data provided.</li>
          <li>If a non-existent UserId is provided or in the wrong format (empty/not UID) in the request, a 403 response code must be returned and no data provided.</li>
        </ul>
        <pre>
          <code>
          {
            "userId": "22222222-2222-2222-2222-222222222222",
            "firstName": "Jane",
            "lastName": "Smith",
            "username": "janesmith",
            "email": "jane@example.com",
            "password": "pass456",
            "address": {
              "line": "456 Park Avenue",
              "city": "New York",
              "country": "USA"
            }
          }
          </code>
        </pre>
      </li>
    </ol>
  </li>
</ol>
