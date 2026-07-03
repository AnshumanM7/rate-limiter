# Configurable Persistent Rate Limiter Service

A production-ready, stateless, and persistent **Rate Limiter Service** built with Spring Boot, Spring Data JPA, and PostgreSQL (AWS RDS). 

This project implements multiple stateful rate-limiting algorithms, utilizing **pessimistic database locking** to guarantee thread safety and prevent race conditions (double-spend/leak problems) across multiple concurrent request streams and horizontal application nodes.

---


* **Persisted Rate-Limiting**: Strategy states are fully persistent in SQL tables rather than local server memory.
* **Horizontal Scaling Support**: Stateless architecture allows safe deployment across load-balanced cluster nodes.
* **4 Rate-Limiting Strategies**:
  1. **Fixed Window**: Tracks request counts inside static, fixed-interval windows.
  2. **Sliding Window**: Uses rolling timestamp request logs to support high-granularity sliding window evictions.
  3. **Token Bucket**: Distributes requests based on token capacity and a continuous refill interval.
  4. **Leaky Bucket**: Processes requests using a queue capacity and a fixed-rate continuous leak process.
* **Container Ready**: Packaged using a multi-stage Docker build for lightweight production images.
* **Automated CI/CD**: Pre-configured GitHub Actions workflow compiling, testing, and verifying Docker container builds.

---


* **Java Version**: 21
* **Framework**: Spring Boot 3.5.4
* **Database**: PostgreSQL (Production/Staging), H2 (Local In-Memory Development)
* **ORM**: Spring Data JPA / Hibernate
* **Dependency Manager**: Maven
* **CI/CD & Containerization**: Docker, GitHub Actions

---



All rate-limiter properties are dynamically configurable using system environment variables with fallback defaults defined in [application.properties](src/main/resources/application.properties):

| Property Name | Environment Variable | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `rate-limiter.enabled` | `RATE_LIMITER_ENABLED` | `true` | Globally toggle the rate-limiting filter `on` / `off` |
| `rate-limiter.strategy` | `RATE_LIMITER_STRATEGY` | `fixed-window` | Active strategy bean: `fixed-window`, `sliding-window`, `token-bucket`, or `leaky-bucket` |
| `rate-limiter.max-requests` | `RATE_LIMITER_MAX_REQUESTS` | `10` | Maximum requests allowed within window (Fixed/Sliding/Token) |
| `rate-limiter.window-seconds` | `RATE_LIMITER_WINDOW_SECONDS` | `60` | Time window duration in seconds |
| `rate-limiter.leaky-capacity` | `RATE_LIMITER_LEAKY_CAPACITY` | `5` | Leaky bucket queue size capacity |
| `rate-limiter.leaky-rate` | `RATE_LIMITER_LEAKY_RATE` | `1` | Number of requests leaking from Leaky Bucket per second |

---


To run the application locally without setting up an AWS RDS instance, you can use the built-in, local **H2 database** configuration.

1. **Clone the Repository**:
   ```bash
   git clone <repository-url>
   cd rate-limiter
   ```

2. **Verify Dependencies and Run Tests**:
   Runs the test suite locally against the in-memory H2 database:
   ```bash
   ./mvnw clean test
   ```

3. **Start the Application**:
   ```bash
   ./mvnw spring-boot:run
   ```
   *The server will boot on port `8080` with context path `/rate-limiter`.*

---


To deploy to production with persistent state across multiple nodes, configure the datasource properties to point to your **AWS RDS PostgreSQL** instance:

1. **Configure Environment Variables** on your hosting environment (ECS, EC2, Elastic Beanstalk):
   ```bash
   export RDS_HOSTNAME="your-rds-endpoint.amazonaws.com"
   export RDS_DB_NAME="ratelimiter"
   export RDS_USERNAME="admin"
   export RDS_PASSWORD="securepassword"
   ```

2. **Add Properties** to `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://${RDS_HOSTNAME}:5432/${RDS_DB_NAME}
   spring.datasource.username=${RDS_USERNAME}
   spring.datasource.password=${RDS_PASSWORD}
   spring.datasource.driver-class-name=org.postgresql.Driver
   spring.jpa.hibernate.ddl-auto=update
   ```

3. **Inbound Port Access**:
   Make sure the AWS RDS Security Group inbound rule allows TCP connection on port `5432` from your application instance's IP.

---


To build and run the Docker container locally:

```bash
# Build the Docker image
docker build -t rate-limiter .

# Run the container (e.g. configuring the Leaky Bucket strategy)
docker run -p 8080:8080 \
  -e RATE_LIMITER_STRATEGY=leaky-bucket \
  -e RATE_LIMITER_LEAKY_CAPACITY=5 \
  -e RATE_LIMITER_LEAKY_RATE=1 \
  rate-limiter
```

---


All endpoints require the API Key header to pass the security filter.

### Health Check Endpoint
* **HTTP Method**: `GET`
* **URL**: `http://localhost:8080/rate-limiter/api/health`
* **Header**: `X-API-KEY: user_api_key_123`

#### Example Curl Call:
```bash
curl -i -H "X-API-KEY: user_api_key_123" http://localhost:8080/rate-limiter/api/health
```

#### Expected Responses:
* **`200 OK`**: Under the rate limit threshold.
* **`429 Too Many Requests`**: Rate limit exceeded.
* **`401 Unauthorized`**: If the `X-API-KEY` header is missing or empty.