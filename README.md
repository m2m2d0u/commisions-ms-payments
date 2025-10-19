# Payment Commission Service

BCEAO-compliant commission fee calculation and revenue tracking microservice.

## Overview

The Payment Commission Service is responsible for calculating and tracking all transaction fees on the platform. It implements BCEAO-compliant fee rules and manages platform revenue.

### Key Features

- ✅ **BCEAO Fee Calculation** - Calculate transaction fees based on BCEAO rules
- ✅ **Commission Rules Management** - Manage fee rules per provider and transfer type
- ✅ **Revenue Tracking** - Track platform commission revenue per transaction
- ✅ **Settlement Management** - Track settlement status with providers
- ✅ **Revenue Reporting** - Generate revenue reports by provider, period, currency
- ✅ **Rule Versioning** - Support time-based rule activation/expiration
- ✅ **Multi-Currency Support** - Handle XOF and XAF commissions

### BCEAO Fee Rules

The service implements BCEAO-compliant fees:

- **Transfers ≤ 5,000 XOF**: **FREE** (financial inclusion mandate)
- **Transfers > 5,000 XOF**: **100 XOF fixed + 0.5% of amount**, capped at **1,000 XOF**
- **Formula**: `min(100 + (amount × 0.005), 1000)`

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Message Broker**: Apache Kafka
- **Build Tool**: Gradle 8.5+
- **API Documentation**: OpenAPI 3.0 (Swagger)

## Prerequisites

- Java 17 or higher
- Docker & Docker Compose
- Gradle 8.5+ (or use the wrapper)
- PostgreSQL 15+ (if running locally)
- Redis 7+ (if running locally)
- Apache Kafka (if running locally)

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd payment-commission-service
```

### 2. Build the Project

```bash
./gradlew clean build
```

### 3. Run with Docker Compose (Recommended)

```bash
docker-compose up -d
```

This will start:
- PostgreSQL database (port 5432)
- Redis cache (port 6379)
- Kafka + Zookeeper (port 9092)
- Commission Service (port 8086)

### 4. Run Locally (Alternative)

Ensure PostgreSQL, Redis, and Kafka are running, then:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## API Documentation

Once the service is running, access the API documentation at:

- **Swagger UI**: http://localhost:8086/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8086/v3/api-docs

## API Endpoints

### Commission Calculation

#### Calculate Fee

```http
POST /api/v1/commissions/calculate
Content-Type: application/json

{
  "amount": 50000,
  "currency": "XOF",
  "providerId": "660e8400-e29b-41d4-a716-446655440001",
  "transferType": "SAME_WALLET",
  "kycLevel": "LEVEL_2"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "amount": 50000,
    "currency": "XOF",
    "commissionAmount": 350,
    "ruleId": "770e8400-e29b-41d4-a716-446655440002",
    "transferType": "SAME_WALLET",
    "calculationDetails": {
      "percentageFee": 0.005,
      "percentageAmount": 250,
      "fixedAmount": 100,
      "finalAmount": 350
    }
  }
}
```

#### Calculate BCEAO Fee

```http
GET /api/v1/commissions/bceao-fee/3000
```

**Response**:
```json
{
  "success": true,
  "data": {
    "amount": 3000,
    "currency": "XOF",
    "commissionAmount": 0,
    "rule": "BCEAO_STANDARD"
  }
}
```

### Commission Rules Management

#### Create Commission Rule

```http
POST /api/v1/commissions/rules
Content-Type: application/json
X-User-ID: <admin-user-id>

{
  "providerId": "660e8400-e29b-41d4-a716-446655440001",
  "currency": "XOF",
  "transferType": "SAME_WALLET",
  "percentage": 0.005,
  "fixedAmount": 100,
  "minAmount": 50,
  "maxAmount": 1000,
  "priority": 10,
  "description": "Standard BCEAO fee"
}
```

#### Get All Rules

```http
GET /api/v1/commissions/rules?page=0&size=20&providerId=<uuid>
```

#### Update Rule

```http
PUT /api/v1/commissions/rules/{ruleId}
Content-Type: application/json

{
  "percentage": 0.0075,
  "maxAmount": 1500,
  "isActive": true
}
```

#### Deactivate Rule

```http
DELETE /api/v1/commissions/rules/{ruleId}
```

### Revenue Reports

```http
GET /api/v1/commissions/revenue?startDate=2025-01-01&endDate=2025-01-31&groupBy=PROVIDER
```

## Database Schema

### commission_rules

Stores commission calculation rules.

| Column | Type | Description |
|--------|------|-------------|
| rule_id | UUID | Primary key |
| provider_id | UUID | Wallet provider ID |
| currency | VARCHAR(3) | XOF or XAF |
| transfer_type | VARCHAR(20) | SAME_WALLET, CROSS_WALLET, INTERNATIONAL |
| percentage | NUMERIC(5,4) | Fee percentage (e.g., 0.0050 for 0.5%) |
| fixed_amount | BIGINT | Fixed fee amount |
| min_amount | BIGINT | Minimum commission |
| max_amount | BIGINT | Maximum commission (cap) |
| is_active | BOOLEAN | Rule active status |
| priority | INTEGER | Rule priority (higher evaluated first) |

### commission_transactions

Tracks platform commission revenue.

| Column | Type | Description |
|--------|------|-------------|
| commission_id | UUID | Primary key |
| transaction_id | UUID | Associated transaction |
| rule_id | UUID | Applied commission rule |
| provider_id | UUID | Wallet provider |
| amount | BIGINT | Commission amount |
| status | VARCHAR(20) | PENDING, COMPLETED, REFUNDED |
| settled | BOOLEAN | Settlement status |
| settlement_date | TIMESTAMP | Settlement date |

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | localhost |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | Database name | commission_db |
| `DB_USERNAME` | Database username | postgres |
| `DB_PASSWORD` | Database password | postgres |
| `REDIS_HOST` | Redis host | localhost |
| `REDIS_PORT` | Redis port | 6379 |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka servers | localhost:9092 |
| `SERVER_PORT` | Service port | 8086 |

### Application Profiles

- **dev**: Development mode (verbose logging, SQL logging)
- **prod**: Production mode (optimized logging, pooling)

## Events

The service publishes the following Kafka events:

### commission-events Topic

- **COMMISSION_COLLECTED**: When commission is recorded
- **COMMISSION_REFUNDED**: When transaction is refunded
- **COMMISSION_SETTLED**: When commission is settled

## Health Checks

```http
GET /actuator/health
```

```http
GET /actuator/info
```

## Testing

### Run All Tests

```bash
./gradlew test
```

### Run Integration Tests

```bash
./gradlew integrationTest
```

## Deployment

### Build Docker Image

```bash
docker build -t payment-commission-service:1.0.0 .
```

### Run with Docker

```bash
docker run -d \
  -p 8086:8086 \
  -e DB_HOST=postgres \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  --name commission-service \
  payment-commission-service:1.0.0
```

## Monitoring

The service exposes Prometheus metrics at:

```
http://localhost:8086/actuator/prometheus
```

## Troubleshooting

### Database Connection Issues

```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check database logs
docker logs commission-db
```

### Kafka Connection Issues

```bash
# Check Kafka is running
docker ps | grep kafka

# Check topics
docker exec -it commission-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Redis Connection Issues

```bash
# Check Redis is running
docker exec -it commission-redis redis-cli ping
```

## Development

### Code Style

The project follows standard Java/Spring Boot conventions:
- Use Lombok for boilerplate reduction
- MapStruct for object mapping
- Jakarta Bean Validation for input validation

### Adding New Commission Rules

1. Create commission rule via API or insert into database
2. Set priority (higher = evaluated first)
3. Set effective dates
4. Activate the rule

## License

Proprietary - Payment System Team

## Support

For issues or questions, contact: support@payment.com

---

**Version**: 1.0.0
**Last Updated**: 2025-10-19
**Maintained By**: Payment System Team
