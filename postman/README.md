# Commission Service - Postman Collection

This directory contains Postman collection and local environment for testing the Payment Commission Service API.

## Files

- **Commission-Service.postman_collection.json** - Complete API collection with all endpoints
- **Commission-Service-Local.postman_environment.json** - Local development environment (localhost:8086)

## Getting Started

### 1. Import Collection

1. Open Postman
2. Click **Import** button
3. Select `Commission-Service.postman_collection.json`
4. The collection will appear in your Collections sidebar

### 2. Import Environment

1. Click **Import** button
2. Select `Commission-Service-Local.postman_environment.json`
3. Select "Commission Service - Local" from the environment dropdown in the top-right corner

### 3. Start the Service

Before running requests, ensure the commission service is running:

```bash
cd /Users/mac/IdeaProjects/payment-system-microservices/commissions
./gradlew bootRun
```

The service should start on `http://localhost:8086`

### 4. Run Migrations

Ensure database migrations have been applied:

```bash
./gradlew flywayMigrate
```

## Collection Structure

### 📁 Commissions
Fee calculation endpoints:
- **Calculate Transaction Fee** - Calculate commission based on active rules
- **Calculate BCEAO Fee** - Calculate using standard BCEAO rules

### 📁 Commission Rules
CRUD operations for managing commission rules:
- **Create Commission Rule** - Create new fee rules
- **Get All Commission Rules** - List all rules (paginated)
- **Get Rules by Currency** - Get all rules for a specific currency
- **Get Active Rules by Currency** - Get only active rules for a currency
- **Get Commission Rule by ID** - Get specific rule
- **Update Commission Rule** - Modify existing rule
- **Activate Commission Rule** - Enable a deactivated rule
- **Deactivate Commission Rule** - Disable a rule (soft delete)

### 📁 Revenue Reports
Commission revenue reporting:
- **Get Revenue Report** - Generate revenue reports with filtering and grouping
- **Get Revenue Report - By Currency** - Revenue breakdown by currency
- **Get Revenue Report - By Provider** - Revenue breakdown by provider

### 📁 Test Scenarios
Pre-configured test cases:
- Small Transaction (Free Tier)
- Medium Transaction (Standard Fee)
- Large Transaction (VIP Rate)
- Cross-Wallet Transfer
- XAF Currency Transaction

## Environment Variables

The local environment includes the following variables:

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `baseUrl` | Service base URL | `http://localhost:8086` |
| `apiVersion` | API version | `v1` |
| `userId` | User ID for audit tracking | Auto-generated UUID |
| `ruleId` | Commission rule ID | Automatically saved after creating a rule |
| `host` | Server host | `localhost` |
| `port` | Server port | `8086` |
| `swaggerUrl` | Swagger UI URL | `http://localhost:8086/swagger-ui.html` |
| `apiDocsUrl` | OpenAPI docs URL | `http://localhost:8086/v3/api-docs` |

## Usage Examples

### Creating a New Commission Rule

1. Navigate to **Commission Rules** → **Create Commission Rule**
2. Review/modify the request body:
```json
{
  "currency": "XOF",
  "transferType": "SAME_WALLET",
  "minTransaction": 5001,
  "maxTransaction": 50000,
  "kycLevel": "ANY",
  "percentage": 0.005,
  "fixedAmount": 100,
  "minAmount": 100,
  "maxAmount": 1000,
  "priority": 90,
  "description": "Standard fee for same wallet transfers"
}
```
3. Click **Send**
4. The `ruleId` will be automatically saved to collection variables

### Calculating a Transaction Fee

1. Navigate to **Commissions** → **Calculate Transaction Fee**
2. Modify the amount, currency, transferType, or kycLevel as needed
3. Click **Send**
4. Review the calculated commission in the response

### Testing Different Scenarios

Use the pre-configured scenarios in the **Test Scenarios** folder:
- Each scenario tests a different combination of amount, currency, transfer type, and KYC level
- Simply select a scenario and click **Send**

## Request Parameters Reference

### Transfer Types
- `SAME_WALLET` - Transfer within same provider (e.g., Orange Money → Orange Money)
- `CROSS_WALLET` - Transfer between providers (e.g., Orange Money → Wave)
- `INTERNATIONAL` - Cross-border/cross-currency transfers

### KYC Levels
- `ANY` - No specific KYC requirement
- `LEVEL_1` - Basic KYC verification
- `LEVEL_2` - Enhanced KYC verification
- `LEVEL_3` - Full KYC verification

### Currencies
- `XOF` - West African CFA Franc (Benin, Burkina Faso, Côte d'Ivoire, etc.)
- `XAF` - Central African CFA Franc (Cameroon, Chad, Gabon, etc.)

## Automated Tests

The collection includes automated tests that run after each request:

### Global Tests (All Requests)
- Response time < 2000ms
- Content-Type is application/json

### Endpoint-Specific Tests
- Status code validation (200, 201, etc.)
- Response structure validation
- Success flag verification
- Data field presence checks

### Running Tests

**Run entire collection:**
1. Click the **...** menu next to the collection
2. Select **Run collection**
3. Configure test settings
4. Click **Run Commission Service**

**Run folder/request:**
1. Hover over folder or request
2. Click **Run** button
3. Review test results

## Tips & Best Practices

### 1. Variable Usage
- Collection variables (`{{ruleId}}`) are shared across all requests
- Use `{{$randomUUID}}` for generating random UUIDs
- The `userId` variable uses `{{$randomUUID}}` by default

### 2. Request Chaining
The collection is designed to chain requests:
1. **Create Rule** → Saves `ruleId` to variables
2. **Get/Update/Activate/Deactivate** → Uses saved `ruleId`

### 3. Validation Testing
Test validation by sending invalid data:
- Negative amounts
- Invalid currency codes
- Percentage > 1.0
- Missing required fields

### 4. Pagination
When fetching all rules, use query parameters:
- `page=0` - First page (0-indexed)
- `size=20` - Number of items per page

### 5. Date Formats
For revenue reports, use ISO date format:
- `startDate=2025-10-01`
- `endDate=2025-10-31`

## BCEAO Fee Rules

The service implements BCEAO (Central Bank of West African States) standard rules:

| Amount Range | Fee Calculation |
|--------------|-----------------|
| ≤ 5,000 XOF | FREE (0 XOF) |
| > 5,000 XOF | 100 XOF + 0.5% (max 1,000 XOF) |

Test using the **Calculate BCEAO Fee** endpoint.

## Troubleshooting

### Connection Refused
**Problem:** `Error: connect ECONNREFUSED 127.0.0.1:8086`

**Solution:**
1. Verify service is running: `curl http://localhost:8086/actuator/health`
2. Check if port 8086 is in use: `lsof -i :8086`
3. Review application logs

### 404 Not Found
**Problem:** Endpoint returns 404

**Solution:**
1. Verify the API path matches the controller mapping
2. Check if `apiVersion` variable is set correctly (`v1`)
3. Ensure service has started successfully

### Database Connection Error
**Problem:** Service fails to start due to database issues

**Solution:**
1. Verify PostgreSQL is running
2. Check database credentials in `application.yml`
3. Run migrations: `./gradlew flywayMigrate`

### Empty Response Data
**Problem:** Response has `success: true` but empty/null data

**Solution:**
1. Check if database has seed data
2. Run migration V6 for example commission rules
3. Verify request parameters match existing data

## API Documentation

Alternative ways to explore the API:

### Swagger UI
Open in browser: `http://localhost:8086/swagger-ui.html`
- Interactive API documentation
- Try out endpoints directly
- View request/response schemas

### OpenAPI Spec
JSON format: `http://localhost:8086/v3/api-docs`
- Machine-readable API specification
- Can be imported into other tools

## Running Locally

This collection is designed for local development and testing:

1. **Start PostgreSQL** - Ensure your database is running
2. **Start Redis** - Required for caching (default: localhost:6379)
3. **Run Migrations** - Execute `./gradlew flywayMigrate` to set up database
4. **Start Service** - Run `./gradlew bootRun`
5. **Verify** - Service should be available at `http://localhost:8086`
6. **Test** - Use Postman collection to test endpoints

## Support

For issues or questions:
- Check application logs: `tail -f logs/commission-service.log`
- Review API documentation: `/swagger-ui.html`
- Inspect database: Connect to PostgreSQL and query `commission_rules` table
- Check migration status: `./gradlew flywayInfo`

---

**Version:** 1.1.0
**Last Updated:** 2025-10-20
**Service:** Payment Commission Service
**API Version:** v1
**Environment:** Local Development (localhost:8086)
