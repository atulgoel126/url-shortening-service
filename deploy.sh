#!/bin/bash

# Deployment script for LinkSplit to EC2

# Configuration
EC2_HOST="18.60.52.214"
EC2_USER="ec2-user"
KEY_PATH="$HOME/Downloads/linksplit-key-pair.pem"
PROJECT_DIR="/Users/atulgoel/Desktop/cloven_apps/url-shortening-service"

# Generate timestamp
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
ARCHIVE_NAME="linksplit-${TIMESTAMP}.tar.gz"
REMOTE_DIR="~/${TIMESTAMP}"

echo "========================================="
echo "Deploying LinkSplit to EC2"
echo "Timestamp: ${TIMESTAMP}"
echo "========================================="

# Change to project parent directory
cd /Users/atulgoel/Desktop/cloven_apps

# Step 1: Create archive
echo "📦 Creating archive: ${ARCHIVE_NAME}"
tar -czf "${ARCHIVE_NAME}" \
    --exclude='url-shortening-service/target' \
    --exclude='url-shortening-service/.git' \
    --exclude='url-shortening-service/*.log' \
    --exclude='url-shortening-service/.idea' \
    --exclude='url-shortening-service/.vscode' \
    --exclude='url-shortening-service/node_modules' \
    url-shortening-service/

# Check if archive was created successfully
if [ ! -f "${ARCHIVE_NAME}" ]; then
    echo "❌ Failed to create archive"
    exit 1
fi

# Get file size
FILE_SIZE=$(du -h "${ARCHIVE_NAME}" | cut -f1)
echo "✅ Archive created: ${ARCHIVE_NAME} (${FILE_SIZE})"

# Step 2: Create remote directory and transfer file
echo "📤 Transferring to EC2..."
echo "   Remote directory: ${REMOTE_DIR}"

# Create remote directory and upload in one command
ssh -i "${KEY_PATH}" ${EC2_USER}@${EC2_HOST} "mkdir -p ${TIMESTAMP}" && \
scp -i "${KEY_PATH}" "${ARCHIVE_NAME}" ${EC2_USER}@${EC2_HOST}:~/${TIMESTAMP}/

# Check if transfer was successful
if [ $? -eq 0 ]; then
    echo "✅ Transfer complete!"
else
    echo "❌ Transfer failed"
    rm "${ARCHIVE_NAME}"
    exit 1
fi

# Step 3: Extract on remote server
echo "📂 Extracting on remote server..."
ssh -i "${KEY_PATH}" ${EC2_USER}@${EC2_HOST} << EOF
    cd ${TIMESTAMP}
    tar -xzf ${ARCHIVE_NAME}
    echo "Files extracted to: ~/${TIMESTAMP}/url-shortening-service/"
    ls -la url-shortening-service/
EOF

# Step 4: Cleanup local archive
echo "🧹 Cleaning up local archive..."
rm "${ARCHIVE_NAME}"

echo "========================================="
echo "✅ Deployment package ready!"
echo "📝 Remote directory: ${EC2_USER}@${EC2_HOST}:~/${TIMESTAMP}/url-shortening-service/"

# SSH into server and run deployment
echo "🔗 Connecting to server and running deployment..."
ssh -i "${KEY_PATH}" "${EC2_USER}@${EC2_HOST}" << EOF
    echo "📂 Changing to deployment directory..."
    cd ${TIMESTAMP}/url-shortening-service
    
    echo "🚀 Running remote deployment..."
    ./remote-deploy.sh
EOF

echo "========================================="