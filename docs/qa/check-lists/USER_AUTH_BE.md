<code>DISCLAIMER: This checklist contains some requirements derived from the source code.
I know that the checklist should contain only checks, but since our requirements are not complete, we have to resort to reverse engineering to extract the requirements from the code. In the future, these requirements should be studied and moved to the appropriate place and removed from here.
</code>
<ol>
  <li>
    Auth endpoints
    <ol>
      <li>
        Register user: POST `/api/v1/auth/register`
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
          <li>Email parameter must meet RFC 5321 standard - If this parameter doesn't meet this format, the corresponding message in the response must be returned with a 400 code response.</li>
          <li>RFC 5321 requirements: Email addresses are defined as a local part followed by the "@" symbol and then a domain part. The local part should consist of printable ASCII characters, while the domain part should be a valid domain name. </li>
          <li> Registering accounts with the same usernames is impossible.</li>
          <li>Authentication not needed for this endpoint - It is possible to perform the request with no authentication.</li>
          <li>If all parameters are provided in the correct format, the response must return a JWT token.</li>
          <li>Users can authenticate all next requests with this token.</li>
          <li>Obtained token must expire after 6044800 ms (validity time).</li>
          <li>For obtaining new JWT token user must use /auth/authenticate endpoint.</li>
        </ul>
      </li>
      <li>
        Auth user: POST `/api/v1/auth/authenticate`
        <ul>
          <li>It is possible to obtain a JWT token by providing the username and password registered previously in the body in the following format:</li>
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
          <li>All these parameters are mandatory - If one or more parameters are blank, a 400 response code must be returned with the corresponding message.</li>
          <li>If an existing username and wrong password are passed - a 401 response code must be returned with general message e.g. "Failed to log in" (username enumeration is impossible).</li>
        </ul>
      </li>
      <li>
        Logout user: POST `/api/v1/auth/logout`
        <ul>
          <li>It is possible to perform the request with not expired JWT token.</li>
          <li>After performing this request, the JWT token must become invalid, and it isn't possible to perform all secured endpoints with it.</li>
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
          <li>Response body must be provided in the following format:</li>
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
        <ul>
          <li>There is no password in response.</li>
          <li>It is possible to get just created user data by ID.</li>
          <li>It isn't possible to get user data by ID if the request with no authentication.</li>
          <li>If an unauthenticated request performs a 401 (or 403) response code must be returned and no data provided.</li>
          <li>If a non-existent UserId is provided, a 404 response code must be returned.</li>
          <li>If UserId is provided in the not UID format in the request, a 400 response code must be returned with the corresponding message.</li>
        </ul>
      </li>
    </ol>
  </li>
</ol>