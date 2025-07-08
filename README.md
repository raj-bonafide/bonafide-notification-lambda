# Bonafide Notification Lambda

A serverless notification service for the Bonafide platform, enabling real-time, targeted notifications to users, roles, or teams via AWS Lambda, API Gateway (WebSocket & HTTP), and DynamoDB.

---

## Features
- Real-time notifications to users, roles, or teams
- WebSocket and HTTP API Gateway integration
- Connection and subscription management in DynamoDB
- REST API for sending notifications and querying metrics
- WebSocket protocol for subscribing to topics and heartbeats
- Direct Lambda invocation support
- Metrics and health endpoints
- Scalable, serverless, and easy to extend

---

## Request Types & Flow

The Lambda handler supports multiple event types:
- **HTTP API**: Handles REST endpoints for sending notifications, metrics, health, and connections.
- **WebSocket Connect/Disconnect**: Manages user connections and stores metadata in DynamoDB.
- **WebSocket Default**: Handles client messages (e.g., subscribe, heartbeat) for real-time updates.
- **Direct Invoke**: Allows programmatic notification sending via Lambda invocation.

### Notification Flow
1. **Client connects via WebSocket** → `$connect` event stores connection info.
2. **Client subscribes to topics** via WebSocket message (`{"action": "subscribe", "topics": ["PROCESS_COMPLETE"]}`) → updates subscriptions in DynamoDB.
3. **Notification is sent** (via REST API or Lambda invoke) → eligible connections are determined and notified in real-time.
4. **Client sends heartbeat** (`{"action": "heartbeat"}`) → updates last seen timestamp.
5. **Disconnect** → `$disconnect` event removes connection.

---

## REST API Endpoints

### Send Notification
Send a notification to users, roles, or teams.
```sh
curl -X POST https://<api-url>/api/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "type": "SYSTEM_ALERT",
    "title": "System Maintenance",
    "message": "The system will be down at midnight.",
    "priority": "HIGH",
    "targetUsers": ["user1", "user2"],
    "timestamp": 1717238400
  }'
```

### Get Metrics
Get active connection and subscription metrics.
```sh
curl https://<api-url>/api/notifications/metrics
```

### Health Check
Check service health.
```sh
curl https://<api-url>/api/notifications/health
```

### Get Active Connections
```sh
curl https://<api-url>/api/notifications/connections
```

---

## WebSocket Protocol

### Connect
Clients connect to the WebSocket endpoint:
```
wss://<api-id>.execute-api.<region>.amazonaws.com/<stage>?userId=<user>&roles=USER,ADMIN&teams=TEAM1,TEAM2&department=SALES
```
- On connect, the service stores the connection with user info and default subscriptions.

### Subscribe to Topics
Send a message after connecting to subscribe to topics:
```json
{
  "action": "subscribe",
  "topics": ["PROCESS_COMPLETE", "SYSTEM_ALERTS"]
}
```

### Heartbeat
Send a heartbeat to keep the connection alive:
```json
{
  "action": "heartbeat"
}
```

---

## Notification Payload Structure

### NotificationRequest
```json
{
  "type": "CUSTOM_NOTIFICATION",
  "title": "Title",
  "message": "Message body",
  "moduleName": "MODULE",
  "priority": "HIGH",
  "targetUsers": ["user1"],
  "requiredRoles": ["ADMIN"],
  "targetTeams": ["TEAM1"],
  "processOwnerId": "ownerId",
  "data": {"key": "value"},
  "actions": [{"type": "OPEN_URL", "url": "https://..."}],
  "timestamp": 1717238400
}
```

### NotificationResult
```json
{
  "status": "SENT|FAILED|NO_RECIPIENTS",
  "sent": 1,
  "failed": 0,
  "totalRecipients": 1,
  "message": "Sent to 1/1 connections"
}
```

---

## How to Subscribe to Notifications
1. **Connect** to the WebSocket endpoint with your user info as query params.
2. **Send a subscribe message** to specify which topics you want to receive:
   ```json
   { "action": "subscribe", "topics": ["PROCESS_COMPLETE", "SYSTEM_ALERTS"] }
   ```
3. **Send periodic heartbeats** to keep your connection alive:
   ```json
   { "action": "heartbeat" }
   ```
4. **Receive notifications** in real-time as messages from the server.

---

## Configuration
- Environment variables:
  - `CONNECTIONS_TABLE`: DynamoDB table for connections
  - `WEBSOCKET_API_ENDPOINT`: WebSocket API endpoint (must be https:// for Lambda)
  - `AWS_REGION`: AWS region
- See `src/main/resources/notificationlambda/notification-service.properties` for more options.

---

## Example Use Cases
- Notify users of process completion
- Send system alerts to all admins
- Targeted notifications to users, roles, or teams
- Real-time UI updates for business events

---

## License
Proprietary - Bonafide Platform
