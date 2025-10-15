# Redis Rate Limiting

This project demonstrates how to implement and compare different **rate limiting algorithms** using **Spring Boot** and **Redis**.  

It provides practical examples of how these algorithms behave under load and how Lua scripting can be used to ensure atomic operations in Redis.

---

## 🚀 Project Overview

The goal of this project is to explore and compare the performance and behavior of various **rate limiting strategies** implemented in a distributed environment.  

Each algorithm is implemented in two ways:
1. Using direct Redis operations from Java.
2. Using **Lua scripts** for atomic execution within Redis.

All Java-based implementations remain commented out for educational purposes, allowing you to easily compare both approaches.

---

## 🧩 Implemented Algorithms

| Algorithm                             | Description                                                                                    |
|---------------------------------------|------------------------------------------------------------------------------------------------|
| **Token Bucket**                      | Allows requests as long as there are available tokens. Tokens are replenished at a fixed rate. |
| **Leaky Bucket**                      | Requests are processed at a steady rate, smoothing out traffic spikes.                         |
| **Fixed Window**                      | Counts requests within a fixed time window. Simple but can suffer from boundary effects.       |
| **Sliding Window Counter (Weighted)** | Uses proportionally weighted sub-windows for smoother and more accurate rate control.                                       |
| **Sliding Window Log**                | Tracks timestamps of recent requests for the most precise limiting.                            |

Each algorithm has its own annotation and Lua script to perform the rate-limiting check atomically within Redis.

---

## ⚡ Why Lua Scripts?

Redis operations such as incrementing counters, adding timestamps, and trimming old entries must often be **atomic** to prevent race conditions in distributed systems.

While Spring Data Redis operations can achieve this to some extent, they involve multiple round trips. Lua scripts execute directly **inside Redis**, ensuring:
- Atomicity (no partial updates)
- Better performance under high concurrency
- Reduced network overhead

Each algorithm in this project includes a Lua script that encapsulates the rate-limiting logic.

### ⚙️ Script Execution Optimization

In this project, Lua scripts are executed using **`evalsha`** via low-level `RedisCommands` instead of Spring’s `RedisTemplate`.  
This approach provides significant performance benefits:

- **`RedisTemplate`** executes scripts using `EVAL`, which means the script body is sent to Redis on every call.
- **`RedisCommands` + `EVALSHA`** executes scripts by their SHA1 hash, allowing Redis to reuse the cached script instead of reloading it each time.

If the script is not yet cached (i.e., `NOSCRIPT` error occurs), the implementation automatically falls back to a one-time `EVAL` call, then resumes using `EVALSHA` for all subsequent executions.

This ensures both **atomic execution** and **high efficiency** even under heavy request loads.

---

## 🧠 Design Notes

- **Annotation-driven architecture**

  Each rate limiting strategy is exposed through a dedicated annotation, such as:
  ```java
  @TokenBucketRateLimit(...)
  @LeakyBucketRateLimit(...)
  @FixedWindowRateLimit(...)
  @SlidingWindowCounterLimit(...)
  @SlidingWindowLogLimit(...)
  ```
  These annotations are intercepted via **Spring AOP**, allowing the rate limiting logic to remain separate from business concerns while keeping the controller layer clean.


- **Lua scripts for atomicity**

  Although the first version of the project performed Redis operations directly from Java, this approach introduced potential race conditions in high-concurrency environments.

  To address this, all algorithms were reimplemented using **Lua scripts**, ensuring atomic, server-side execution inside Redis.

  The original Java implementations remain in the codebase as commented examples for educational comparison.


- **Sliding Window Counter (Weighted approach)**

  This project adopts the weighted sliding window counter variant rather than the basic counter model.

  In the traditional counter method, each sub-window has equal weight, which may cause abrupt rate changes at window boundaries.

  The weighted version, however, applies proportional weighting based on how much of the current time falls within each sub-window — providing smoother transitions and more accurate rate control under fluctuating load.


- **Sliding Window Algorithms Optimization**

  In many reference implementations, requests that exceed the rate limit are still added to Redis.

  This project deliberately avoids storing **rejected requests**.

  The reason: when every incoming request (including rejected ones) is logged, the sliding window becomes permanently saturated during high traffic, blocking all further requests.
  
  By excluding rejected requests, the limiter preserves **capacity awareness**, allowing new valid requests once earlier ones expire — resulting in a more stable throughput under heavy load.

---

## 🧪 Example API Usage

All endpoints are accessible under:
```bash
http://localhost:8080/rate-limiter/
```

Endpoints:
```bash
GET /rate-limiter/token-bucket
GET /rate-limiter/leaky-bucket
GET /rate-limiter/fixed-window
GET /rate-limiter/sliding-window-counter
GET /rate-limiter/sliding-window-log
```

Each endpoint is annotated with the corresponding rate limiter annotation, demonstrating how the request flow is controlled.

### 🧾 Response Behavior

All endpoints return only HTTP status codes — there is no response body.

| Status Code               | Meaning                                                                       |
|---------------------------|-------------------------------------------------------------------------------|
| **202 Accepted**          | The request has been accepted and processed successfully (within rate limit). |
| **429 Too Many Requests** | The request has been rejected because the rate limit has been reached.        |

This lightweight response design makes it easier to benchmark and observe the limiter’s behavior under different load conditions.

---

## 🧰 Running the Project

#### 1. Start a Redis instance (via Podman, Docker, or locally):
```bash
podman run --name myredis -p 6379:6379 -d redis redis-server --requirepass s3cret
```

#### 2. Clone this repository:
```bash
git clone https://github.com/ercansormaz/redis-rate-limiting.git
```

#### 3. Navigate to the project folder and build:
```bash
mvn clean install
```

#### 4. Run the application:
```bash
mvn spring-boot:run
```

#### 5. Access any of the test endpoints listed above to observe the rate limiting behavior.

---

## 🤝 Contributing
Contributions are welcome! Feel free to fork the repo, submit pull requests or open issues.

---

## 📜 License
This project is licensed under the MIT License.