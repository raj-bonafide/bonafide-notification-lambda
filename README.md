# Bonafide Notification Lambda

This project provides a Notification Lambda Service for the Bonafide platform, enabling real-time notifications to users via AWS Lambda, DynamoDB, and API Gateway WebSocket integration.

## Features
- Send notifications to users, roles, or teams
- AWS Lambda and WebSocket API Gateway integration
- DynamoDB for connection management
- Spring Boot configuration and auto-configuration
- Metrics and logging support
- Lambda/serverless friendly design

## Project Structure
- `src/main/java/com/bonafide/notificationlambda/` - Main Java source code
- `src/main/resources/notificationlambda/` - Resource and configuration files

## Getting Started
1. Clone the repository
2. Build with Maven: `mvn clean install`
3. Configure AWS credentials and environment variables as needed
4. Deploy the Lambda and configure API Gateway

## Configuration
See `src/main/resources/notificationlambda/notification-service.properties` for configuration options.

Example (application.yml):
```yaml
notification:
  enabled: true
  use-lambda-direct: true
  aws:
    region: us-east-1
    lambda:
      function-name: notification-service-lambda
    dynamo-db:
      connections-table: websocket-connections
```

## Lambda Deployment Notes
- The library is designed to be used in AWS Lambda environments.
- You only need two minimal Lambdas for WebSocket API Gateway:
  - **Connection Handler Lambda**: Handles `$connect` and `$disconnect` events, manages DynamoDB connection table.
  - **Default Handler Lambda**: (Optional) Handles messages from clients.
- The Java library can invoke Lambda directly or send notifications via WebSocket API Gateway.

## REST API Endpoints

### Send Notification
`POST /api/notifications/send`
- **Body:** `NotificationRequest` JSON
- **Response:** `NotificationResponse` JSON

Example:
```json
POST /api/notifications/send
{
  "type": "SYSTEM_ALERT",
  "title": "System Maintenance",
  "message": "The system will be down at midnight.",
  "priority": "HIGH",
  "targetUsers": ["user1", "user2"],
  "timestamp": "2024-06-01T12:00:00Z"
}
```

### Send to Users
`POST /api/notifications/send/users?type=TYPE&message=MESSAGE`
- **Body:** List of user IDs (JSON array)
- **Response:** `NotificationResponse` JSON

Example:
```json
POST /api/notifications/send/users?type=ALERT&message=Hello
["user1", "user2"]
```

### Send to Roles
`POST /api/notifications/send/roles?type=TYPE&message=MESSAGE`
- **Body:** List of roles (JSON array)
- **Response:** `NotificationResponse` JSON

Example:
```json
POST /api/notifications/send/roles?type=ALERT&message=Hello
["ADMIN", "MANAGER"]
```

### Send Process Complete
`POST /api/notifications/send/process-complete?processId=PID&status=STATUS&ownerId=OWNER&moduleName=MODULE`
- **Response:** `NotificationResponse` JSON

### Get Metrics
`GET /api/notifications/metrics`
- **Response:** Map of metric names to values

### Health Check
`GET /api/notifications/health`
- **Response:** Service status and timestamp

## User Cases
- **Notify users of process completion:**
  - Use `sendProcessComplete` to alert process owners and admins when a process finishes.
- **Send system alerts to all admins:**
  - Use `sendToRoles` with role `ADMIN`.
- **Targeted notifications:**
  - Use `sendToUsers` for direct user notifications.

## License
Proprietary - Bonafide Platform
