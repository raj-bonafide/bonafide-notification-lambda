# AWS Console Setup Guide - Notification Service

## 🎯 Quick Setup Overview

**What we're building:**
```
Your Java App → Single Lambda → WebSocket API → UI Clients
```

**Required AWS Services:**
- ✅ Lambda Function (handles everything)
- ✅ WebSocket API Gateway (real-time connections)
- ✅ DynamoDB Table (connection storage)
- ✅ IAM Role (permissions)

---

## 🚀 Setup Steps

### **Step 1: Create S3 Bucket for Lambda Code**

**AWS Console:**
1. Go to **S3** service
2. Click **Create bucket**
3. Bucket name: `notification-service-deployments-YOUR-NAME`
4. Region: `us-east-1` (or your preferred region)
5. Click **Create bucket**
6. **Upload** your compiled JAR file: `notification-service.jar`

**AWS CLI Alternative:**
```bash
aws s3 mb s3://notification-service-deployments-YOUR-NAME --region us-east-1
aws s3 cp target/notification-service.jar s3://notification-service-deployments-YOUR-NAME/
```

---

### **Step 2: Create DynamoDB Table**

**AWS Console:**
1. Go to **DynamoDB** service
2. Click **Create table**
3. **Table name**: `websocket-connections-dev`
4. **Partition key**: `connectionId` (String)
5. **Table settings**: On-demand
6. Click **Create table**
7. Wait for table to become **Active**

**AWS CLI Alternative:**
```bash
aws dynamodb create-table \
    --table-name websocket-connections-dev \
    --attribute-definitions AttributeName=connectionId,AttributeType=S \
    --key-schema AttributeName=connectionId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --region us-east-1
```

---

### **Step 3: Create IAM Role for Lambda**

**AWS Console:**
1. Go to **IAM** service
2. Click **Roles** → **Create role**
3. **Trusted entity**: AWS service
4. **Use case**: Lambda
5. Click **Next**
6. **Attach policies**:
    - ✅ `AWSLambdaBasicExecutionRole`
7. Click **Next**
8. **Role name**: `notification-lambda-role-dev`
9. Click **Create role**

**Add Custom Policy:**
1. Go to created role → **Add permissions** → **Create inline policy**
2. **JSON** tab, paste:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem",
        "dynamodb:Scan",
        "dynamodb:Query"
      ],
      "Resource": "arn:aws:dynamodb:us-east-1:*:table/websocket-connections-dev"
    },
    {
      "Effect": "Allow",
      "Action": [
        "execute-api:ManageConnections",
        "execute-api:Invoke"
      ],
      "Resource": "arn:aws:execute-api:us-east-1:*:*"
    }
  ]
}
```
3. **Name**: `notification-lambda-custom-policy`
4. Click **Create policy**

---

### **Step 4: Create Lambda Function**

**AWS Console:**
1. Go to **Lambda** service
2. Click **Create function**
3. **Author from scratch**
4. **Function name**: `notification-service-dev`
5. **Runtime**: Java 21
6. **Execution role**: Use existing role
7. **Existing role**: `notification-lambda-role-dev`
8. Click **Create function**

**Configure Lambda:**
1. **Code** tab → **Upload from** → **Amazon S3**
2. **S3 bucket**: `notification-service-deployments-YOUR-NAME`
3. **S3 object key**: `notification-service.jar`
4. Click **Save**

**Environment Variables:**
1. **Configuration** tab → **Environment variables** → **Edit**
2. Add variables:
    - `ENVIRONMENT` = `dev`
    - `CONNECTIONS_TABLE` = `notification_socket_connections`
    - `AWS_REGION` = `ap-south-1`
    - `LOG_LEVEL` = `INFO`
3. Click **Save**

**Function Settings:**
1. **Configuration** tab → **General configuration** → **Edit**
2. **Memory**: 1024 MB
3. **Timeout**: 30 seconds
4. Click **Save**

---

### **Step 5: Create WebSocket API Gateway**

**AWS Console:**
1. Go to **API Gateway** service
2. Click **Create API**
3. **WebSocket API** → **Build**
4. **API name**: `notification-websocket-api-dev`
5. **Route selection expression**: `$request.body.action`
6. Click **Create API**

**Create Routes:**
1. **Routes** → **Create Route**
2. **Route key**: `$connect`
3. **Integration**: Lambda
4. **Lambda function**: `notification-service-dev`
5. Click **Create**

6. **Create Route**
7. **Route key**: `$disconnect`
8. **Integration**: Lambda
9. **Lambda function**: `notification-service-dev`
10. Click **Create**

11. **Create Route**
12. **Route key**: `$default`
13. **Integration**: Lambda
14. **Lambda function**: `notification-service-dev`
15. Click **Create**

**Deploy API:**
1. **Actions** → **Deploy API**
2. **Stage**: `dev`
3. Click **Deploy**

**Note WebSocket URL:**
- Format: `wss://YOUR-API-ID.execute-api.us-east-1.amazonaws.com/dev`
- Copy this URL for later use

---

### **Step 6: Create HTTP API Gateway (Optional)**

**AWS Console:**
1. Go to **API Gateway** service
2. Click **Create API**
3. **HTTP API** → **Build**
4. **API name**: `notification-http-api-dev`
5. Click **Create API**

**Create Routes:**
1. **Routes** → **Create Route**
2. **Method**: POST, **Path**: `/api/notifications/send`
3. **Integration**: Lambda
4. **Lambda function**: `notification-service-dev`
5. Click **Create**

Repeat for these routes:
- `POST /api/notifications/send/users`
- `POST /api/notifications/send/roles`
- `POST /api/notifications/send/process-complete`
- `GET /api/notifications/metrics`
- `GET /api/notifications/health`

**Deploy API:**
1. **Deploy** → **Stage**: `dev`
2. Note the **Invoke URL**

---

### **Step 7: Update Lambda Environment Variables**

**AWS Console:**
1. Go back to **Lambda** → `notification-service-dev`
2. **Configuration** → **Environment variables** → **Edit**
3. Add:
    - `WEBSOCKET_API_ENDPOINT` = `https://YOUR-WEBSOCKET-API-ID.execute-api.us-east-1.amazonaws.com/dev`
    - `HTTP_API_ENDPOINT` = `https://YOUR-HTTP-API-ID.execute-api.us-east-1.amazonaws.com/dev`
4. Click **Save**

---

## 🧪 Testing Your Setup

### **Test HTTP API:**
```bash
# Replace with your actual HTTP API URL
HTTP_API_URL="https://your-http-api-id.execute-api.us-east-1.amazonaws.com/dev"

# Test health check
curl -X GET "${HTTP_API_URL}/api/notifications/health"

# Test metrics
curl -X GET "${HTTP_API_URL}/api/notifications/metrics"

# Test send notification
curl -X POST "${HTTP_API_URL}/api/notifications/send" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "TEST_NOTIFICATION",
    "title": "Setup Test",
    "message": "Testing notification service setup",
    "moduleName": "SETUP_TEST",
    "targetUsers": ["test-user"],
    "priority": "MEDIUM"
  }'
```

### **Test WebSocket (Node.js):**
```javascript
const WebSocket = require('ws');

const wsUrl = 'wss://your-websocket-api-id.execute-api.us-east-1.amazonaws.com/dev';
const params = new URLSearchParams({
    userId: 'test-user-123',
    roles: 'USER,ADMIN',
    teams: 'Backend,DevOps'
});

const ws = new WebSocket(`${wsUrl}?${params}`);

ws.on('open', () => {
    console.log('✅ WebSocket connected');
    ws.send(JSON.stringify({ action: 'heartbeat' }));
});

ws.on('message', (data) => {
    console.log('📨 Received:', JSON.parse(data));
});
```

---

## 🎯 Java Application Configuration

**application.yml:**
```yaml
notification:
  enabled: true
  use-lambda-direct: true
  aws:
    region: us-east-1
    lambda:
      function-name: notification-service-dev
    dynamo-db:
      connections-table: websocket-connections-dev
```

**Environment Variables (Optional):**
```bash
export AWS_REGION=us-east-1
export NOTIFICATION_LAMBDA_FUNCTION=notification-service-dev
export NOTIFICATION_DYNAMODB_TABLE=websocket-connections-dev
export NOTIFICATION_WEBSOCKET_URL=wss://your-api-id.execute-api.us-east-1.amazonaws.com/dev
```

---

## 🎉 You're Done!

Your notification service is now live and ready to use:

✅ **Lambda Function**: `notification-service-dev`  
✅ **WebSocket API**: `wss://your-api-id.execute-api.us-east-1.amazonaws.com/dev`  
✅ **HTTP API**: `https://your-api-id.execute-api.us-east-1.amazonaws.com/dev`  
✅ **DynamoDB Table**: `websocket-connections-dev`

**Next Steps:**
1. Update your Java application configuration
2. Test sending notifications from your Java services
3. Connect your UI to the WebSocket endpoint
4. Monitor CloudWatch logs for troubleshooting

**Usage in Java:**
```java
// This will now work!
notificationService.sendProcessComplete(
    "process-123", 
    "SUCCESS", 
    "user@company.com", 
    "DATA_PROCESSING"
);
```

---

## 🔧 Troubleshooting

**Common Issues:**

1. **Lambda timeout**: Increase timeout in Lambda configuration
2. **Permission denied**: Check IAM role has all required policies
3. **WebSocket connection failed**: Verify API Gateway deployment
4. **DynamoDB errors**: Check table name and region match
5. **CORS errors**: Add CORS configuration to HTTP API

**CloudWatch Logs:**
- Lambda logs: `/aws/lambda/notification-service-dev`
- API Gateway logs: Enable in API Gateway settings

**Test Individual Components:**
- Lambda: Use Lambda test console
- DynamoDB: Check items in DynamoDB console
- API Gateway: Use API Gateway test feature