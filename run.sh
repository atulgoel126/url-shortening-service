#!/bin/bash

echo "==================================="
echo "LinkSplit URL Shortening Service"
echo "==================================="

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed. Please install Maven 3.6 or higher."
    exit 1
fi

# Build the application
echo "Building the application..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed. Please check the errors above."
    exit 1
fi

echo ""
echo "Build successful!"
echo ""
echo "To run the application, you need PostgreSQL running with the following:"
echo "  - Database: linksplit"
echo "  - Username: linksplit"
echo "  - Password: linksplit"
echo ""
echo "Run the following SQL to set up the database:"
echo "  CREATE DATABASE linksplit;"
echo "  CREATE USER linksplit WITH PASSWORD 'linksplit';"
echo "  GRANT ALL PRIVILEGES ON DATABASE linksplit TO linksplit;"
echo ""
echo "Then run the schema file:"
echo "  psql -U linksplit -d linksplit -f src/main/resources/db/migration/V1__initial_schema.sql"
echo ""
echo "Finally, start the application with:"
echo "  java -jar target/url-shortening-service-1.0.0-SNAPSHOT.jar"
echo ""
echo "The application will be available at http://localhost:8080"
echo "Production domain: https://cli.p"