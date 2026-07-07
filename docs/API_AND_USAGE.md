# 📡 API Reference & Usage Guide

This guide covers interacting with the API Gateway, utilizing automated testing collections, and simulating error resilience.

---

## 1. Core Endpoint: `GET /aggregate`

Fetches post and author data in parallel from two external REST APIs, extracts specific attributes (`title` and `name`), and returns a unified JSON response.

### HTTP Request
```http
GET /aggregate HTTP/1.1
Host: localhost:8080
Accept: application/json
```

### Curl Command
```bash
curl -i http://localhost:8080/aggregate
```

### Successful Response (`200 OK`)
```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 98

{
  "post_title": "Vert.x 5 Asynchronous API Gateway Assessment",
  "author_name": "Siddharth Shinde"
}
```

---

## 2. Error Handling & Circuit Breaker Fallbacks

If either upstream service fails (HTTP 500, connection timeout, DNS failure, or malformed JSON), the Gateway catches the exception asynchronously and returns a clean error response without crashing or exposing stack traces.

### Error Response (`500 Internal Server Error`)
```http
HTTP/1.1 500 Internal Server Error
content-type: application/json
content-length: 44

{
  "error": "Failed to fetch external service"
}
```

### Simulating Circuit Breaker Tripping
To test circuit breaker resilience locally:
1. Temporarily set an invalid upstream URL in `src/main/resources/application.json` (e.g., `"post.url": "http://localhost:9999/invalid"`).
2. Start the server (`mvn compile exec:java`).
3. Make 3 rapid curl requests:
   ```bash
   curl http://localhost:8080/aggregate
   curl http://localhost:8080/aggregate
   curl http://localhost:8080/aggregate
   ```
4. On the 4th request, notice that the response is **instantaneous** ($0\text{ ms}$ latency). The circuit breaker has tripped to `OPEN` and short-circuits the request locally to protect resources.
5. Wait 10 seconds (the reset timeout). The breaker transitions to `HALF-OPEN` and attempts a recovery probe.

---

## 3. Postman Automated Testing Collection

A complete, pre-configured Postman collection is included in the project root: **`postman_collection.json`**.

### How to Import & Run in Postman:
1. Open Postman.
2. Click **Import** (top left) and select `postman_collection.json`.
3. Select the imported **"Vert.x API Gateway Assessment"** collection.
4. Ensure the collection variable `base_url` is set to `http://localhost:8080`.
5. Click **Run Collection** to execute all automated test assertions:
   - ✅ Asserts HTTP status code is `200`.
   - ✅ Asserts response body contains string properties `post_title` and `author_name`.
   - ✅ Asserts total response latency is under $3000\text{ ms}$.

---

## 4. OpenAPI 3.0 Specification

An industry-standard OpenAPI 3.0 (Swagger) specification is provided in **`openapi.yaml`**.

### Viewing the API Docs:
You can paste the contents of `openapi.yaml` into [editor.swagger.io](https://editor.swagger.io/) or import it into any modern API gateway / portal (Kong, AWS API Gateway, Postman, Stoplight) to generate interactive Swagger UI documentation and SDK client stubs.
