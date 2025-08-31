# LinkSplit - URL Shortening Service

A URL shortening service with monetization through interstitial advertisements, built with Spring Boot and PostgreSQL.

## Features

- URL shortening with Base62 encoding
- User registration and authentication
- Dashboard for tracking links and earnings
- Interstitial ad page with countdown timer
- View tracking and analytics
- Fraud prevention (rate limiting)
- Earnings calculation based on CPM model
- In-memory caching for performance

## Technology Stack

- **Backend**: Java 17, Spring Boot 3.2.0
- **Database**: PostgreSQL 15+
- **Caching**: Caffeine
- **Frontend**: Thymeleaf, Bootstrap 5
- **Security**: Spring Security with BCrypt
- **Testing**: JUnit 5, Mockito, Testcontainers

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 15+

## Setup Instructions

### 1. Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE linksplit;
CREATE USER linksplit WITH PASSWORD 'linksplit';
GRANT ALL PRIVILEGES ON DATABASE linksplit TO linksplit;
```

### 2. Configure Application

Update the database connection in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/linksplit
    username: linksplit
    password: linksplit
```

Or set environment variables:
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`

### 3. Run Database Migrations

The application uses the SQL schema defined in `src/main/resources/db/migration/V1__initial_schema.sql`. 

Run the SQL script manually in your PostgreSQL database:

```bash
psql -U linksplit -d linksplit -f src/main/resources/db/migration/V1__initial_schema.sql
```

### 4. Build the Application

```bash
mvn clean package
```

### 5. Run the Application

```bash
java -jar target/url-shortening-service-1.0.0-SNAPSHOT.jar
```

Or using Maven:

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Configuration

Key configuration properties in `application.yml`:

- `app.base-url`: Base URL for generated short links
- `app.shortcode-length`: Length of generated short codes (default: 6)
- `app.ad-display-seconds`: Countdown timer duration (default: 5)
- `app.cpm-rate`: Cost per thousand impressions (default: 1.50)
- `app.revenue-share`: Creator's revenue share percentage (default: 0.70)
- `app.view-fraud-prevention.enabled`: Enable/disable fraud prevention
- `app.view-fraud-prevention.rate-limit-hours`: Hours for rate limiting

## API Endpoints

### Public Endpoints

- `GET /` - Home page
- `POST /api/url` - Create short URL
- `GET /{shortCode}` - Redirect to ad page
- `GET /ad-page?id={shortCode}` - Display interstitial ad
- `POST /register` - User registration
- `POST /login` - User login

### Authenticated Endpoints

- `GET /dashboard` - User dashboard
- `POST /logout` - Logout

## Testing

Run all tests:

```bash
mvn test
```

Run integration tests:

```bash
mvn test -Dtest=*IntegrationTest
```

## Production Deployment

1. Update `application.yml` with production database credentials
2. Set `spring.jpa.hibernate.ddl-auto` to `validate`
3. Configure HTTPS (required for production)
4. Set up proper logging
5. Configure monitoring and health checks
6. Set up database backups

## Security Considerations

- All passwords are hashed using BCrypt
- CSRF protection enabled for web endpoints
- Prepared statements used to prevent SQL injection
- URL validation to prevent malicious redirects
- Rate limiting to prevent abuse

## Future Enhancements

- Real payment system integration
- Admin dashboard for user management
- Custom domain support for creators
- Ad mediation for increased revenue
- Advanced analytics and reporting
- API rate limiting
- Webhook support for events

## License

This is a POC (Proof of Concept) implementation for educational purposes.