## Asynchronous API Gateway using Vert.x

## Objective:

Implement a simple API Gateway using Vert.x that receives incoming HTTP requests, makes parallel API calls to two different services, aggregates the responses, and returns a combined JSON response.

## Requirements:

- 1. Create an HTTP server in Vert.x that listens on port 8080.

- 2. Define an endpoint (/aggregate) that:

- Makes two parallel HTTP requests to external APIs:

- GET https://jsonplaceholder.typicode.com/posts/1

- GET https://jsonplaceholder.typicode.com/users/1

- Extracts the title from the first API and name from the second API.

## Returns a JSON response like:

```
{
"post_title": "API response title",
"author_name": "API response name"
}
```

- 3. Ensure non-blocking and asynchronous execution using Vert.x.

- 4. Handle errors gracefully (e.g., if an API call fails, return a proper error message).

## Evaluation Criteria:

- Proper use of Vert.x HTTP server and client.

- Efficient handling of asynchronous calls (Future, Promise, or CompositeFuture).


- Proper error handling and JSON response formatting.

- Code modularity and readability.

## Bonus Points:

- Add unit tests using VertxUnit or JUnit5.

- Implement circuit breaker for API failures.
