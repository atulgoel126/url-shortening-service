#!/bin/bash

# Remote deployment script for LinkSplit (runs on EC2)
# Usage: ./remote-deploy.sh [timestamp_folder]
# Example: ./remote-deploy.sh 20241128-143022

set -e  # Exit on error

# Configuration
APP_DIR="/opt/linksplit"
SERVICE_NAME="linksplit"
JAR_NAME="url-shortening-service-1.0.0-SNAPSHOT.jar"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

echo "========================================="
echo "LinkSplit Deployment Script"
echo "========================================="

# Step 1: Determine source directory
if [ -z "$1" ]; then
    # Find the most recent deployment folder
    LATEST_DIR=$(ls -d ~/*/ 2>/dev/null | grep -E '[0-9]{8}-[0-9]{6}' | sort -r | head -n1)
    if [ -z "$LATEST_DIR" ]; then
        print_error "No deployment folder found. Please specify timestamp folder."
        echo "Usage: $0 [timestamp_folder]"
        exit 1
    fi
    SOURCE_DIR="${LATEST_DIR}url-shortening-service"
    print_warning "Using latest deployment: ${LATEST_DIR}"
else
    SOURCE_DIR="$HOME/$1/url-shortening-service"
fi

# Verify source directory exists
if [ ! -d "$SOURCE_DIR" ]; then
    print_error "Source directory not found: $SOURCE_DIR"
    exit 1
fi

print_status "Source directory: $SOURCE_DIR"

# Step 2: Check if service is running
echo ""
echo "Checking current service status..."
if systemctl is-active --quiet $SERVICE_NAME; then
    print_warning "LinkSplit service is currently running"
    NEED_RESTART=true
else
    print_status "LinkSplit service is not running"
    NEED_RESTART=false
fi

# Step 3: Backup current deployment (if exists)
if [ -d "$APP_DIR" ] && [ -f "$APP_DIR/target/$JAR_NAME" ]; then
    BACKUP_DIR="$APP_DIR.backup.$(date +%Y%m%d-%H%M%S)"
    print_status "Creating backup: $BACKUP_DIR"
    sudo cp -r $APP_DIR $BACKUP_DIR
fi

# Step 4: Stop service if running
if [ "$NEED_RESTART" = true ]; then
    print_status "Stopping LinkSplit service..."
    sudo systemctl stop $SERVICE_NAME
    sleep 2
fi

# Step 5: Create app directory if it doesn't exist
if [ ! -d "$APP_DIR" ]; then
    print_status "Creating application directory: $APP_DIR"
    sudo mkdir -p $APP_DIR
    sudo chown ec2-user:ec2-user $APP_DIR
fi

# Step 6: Copy new files
print_status "Copying application files..."
sudo cp -r $SOURCE_DIR/* $APP_DIR/
sudo cp -r $SOURCE_DIR/.[^.]* $APP_DIR/ 2>/dev/null || true

# Step 7: Ensure production config exists
if [ ! -f "$APP_DIR/src/main/resources/application-prod.yml" ]; then
    print_warning "Production config not found. Creating template..."
    sudo cat > $APP_DIR/src/main/resources/application-prod.yml << 'EOF'
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/linksplit
    username: linksplit
    password: your_secure_password
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

app:
  base-url: https://cli.p
  shortcode-length: 6
  ad-display-seconds: 5
  cpm-rate: 1.50
  revenue-share: 0.70
  
server:
  port: 8080
  forward-headers-strategy: framework

logging:
  level:
    root: INFO
    com.linksplit: INFO
EOF
    print_error "Please edit $APP_DIR/src/main/resources/application-prod.yml with correct database password!"
    exit 1
fi

# Step 8: Build the application
print_status "Building application with Maven..."
cd $APP_DIR

# Clean and build
sudo mvn clean package -DskipTests

# Check if build was successful
if [ ! -f "target/$JAR_NAME" ]; then
    print_error "Build failed! JAR file not found: target/$JAR_NAME"
    exit 1
fi

print_status "Build successful!"

# Step 9: Ensure systemd service file exists
if [ ! -f "/etc/systemd/system/$SERVICE_NAME.service" ]; then
    print_warning "Creating systemd service file..."
    sudo tee /etc/systemd/system/$SERVICE_NAME.service > /dev/null << EOF
[Unit]
Description=LinkSplit URL Shortening Service
After=network.target postgresql.service

[Service]
Type=simple
User=ec2-user
WorkingDirectory=$APP_DIR
ExecStart=/usr/bin/java -Dspring.profiles.active=prod -Xmx512m -jar $APP_DIR/target/$JAR_NAME
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF
    sudo systemctl daemon-reload
fi

# Step 10: Start or restart the service
print_status "Starting LinkSplit service..."
sudo systemctl daemon-reload
sudo systemctl enable $SERVICE_NAME
sudo systemctl start $SERVICE_NAME

# Step 11: Wait and check status
print_status "Waiting for service to start..."
sleep 5

# Check if service is running
if systemctl is-active --quiet $SERVICE_NAME; then
    print_status "LinkSplit service started successfully!"
    
    # Test the application
    echo ""
    print_status "Testing application..."
    sleep 3
    
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 | grep -q "200\|302"; then
        print_status "Application is responding on port 8080"
    else
        print_warning "Application may still be starting up. Check logs with: sudo journalctl -u $SERVICE_NAME -f"
    fi
else
    print_error "Service failed to start!"
    echo "Check logs with: sudo journalctl -u $SERVICE_NAME -n 50"
    exit 1
fi

# Step 12: Show useful information
echo ""
echo "========================================="
echo -e "${GREEN}Deployment Complete!${NC}"
echo "========================================="
echo "Service Status: $(systemctl is-active $SERVICE_NAME)"
echo "Application URL: http://$(curl -s ifconfig.me):8080"
echo "Domain URL: https://cli.p"
echo ""
echo "Useful commands:"
echo "  View logs:        sudo journalctl -u $SERVICE_NAME -f"
echo "  Restart service:  sudo systemctl restart $SERVICE_NAME"
echo "  Check status:     sudo systemctl status $SERVICE_NAME"
echo "  Test locally:     curl http://localhost:8080"
echo "========================================="

# Step 13: Cleanup old deployments (optional)
print_status "Cleaning up old deployment folders..."
find ~/ -maxdepth 1 -type d -name "[0-9]*-[0-9]*" -mtime +7 -exec rm -rf {} \; 2>/dev/null || true
print_status "Kept deployments from last 7 days"