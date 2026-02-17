# BlackCar Chat Integration Guide for Front-end

This document provides technical details on how to integrate the chat feature into the front-end application (Web or Mobile).

## 1. WebSocket / STOMP Integration

The real-time messaging is powered by WebSockets using the STOMP protocol.

### Connection Details
- **Base URL**: `ws://[host]:[port]/ws`
- **Protocol**: STOMP over WebSocket
- **Handshake Endpoint**: `/ws`

### Authentication
You must provide the JWT token in the STOMP connection headers.
- **Header Key**: `Authorization`
- **Header Value**: `Bearer <your_jwt_token>`

### Messaging Workflow

#### Sending a Message
To send a message, publish a STOMP frame to the following destination:
- **Destination**: `/app/chat.send`
- **Payload**: JSON matching the `WebChatMessage` structure.

**Example Payload:**
```json
{
  "conversationId": 1,
  "senderId": 5,
  "content": "Hello, I'm waiting at the corner!"
}
```

#### Receiving Messages
To receive messages in real-time, the client should subscribe to a specific conversation topic:
- **Topic**: `/topic/chat/{conversationId}`

When any participant sends a message to the conversation, the server will broadcast the saved `WebChatMessage` to all subscribers of this topic.

---

## 2. REST API Reference

Complementary REST endpoints are used for fetching history and managing conversations.

### Base Path: `/api/v1/chat`

#### A. Get User Conversations
Retrieves all conversations for a specific user.
- **Endpoint**: `GET /conversations/{userId}`
- **Authentication**: Required (JWT Bearer Token in HTTP Header)
- **Response**: `List<WebConversation>`

#### B. Get Conversation History
Retrieves all messages for a specific conversation.
- **Endpoint**: `GET /history/{conversationId}`
- **Authentication**: Required (JWT Bearer Token in HTTP Header)
- **Response**: `List<WebChatMessage>`

#### C. Get or Create Conversation
Finds an existing conversation between participants or creates a new one if it doesn't exist.
- **Endpoint**: `POST /conversations`
- **Authentication**: Required (JWT Bearer Token in HTTP Header)
- **Request Body**: `List<Long>` (List of participant IDs)
- **Example Body**: `[1, 5]`
- **Response**: `WebConversation`

---

## 3. Data Structures (DTOs)

### WebChatMessage
| Field | Type | Description |
|---|---|---|
| `id` | Long | Unique message ID (server-generated) |
| `conversationId` | Long | ID of the conversation |
| `senderId` | Long | ID of the user who sent the message |
| `content` | String | Message text |
| `createdAt` | ISO8601 String | Timestamp when message was created |

### WebConversation
| Field | Type | Description |
|---|---|---|
| `id` | Long | Unique conversation ID |
| `createdAt` | ISO8601 String | When the conversation was started |
| `participantIds` | Set<Long> | IDs of all participants |
| `lastMessage` | WebChatMessage | The most recent message in this conversation |

---

## 4. Implementation Tips

1. **Connect on Login**: Establish the WebSocket connection once the user is authenticated.
2. **Subscribe Dynamically**: When a user opens a chat window, subscribe to `/topic/chat/{conversationId}`. Unsubscribe when the chat is closed to save resources.
3. **Optimistic UI**: You can show the message locally before the server confirms it, but wait for the WebSocket broadcast to guarantee delivery and get the official `id` and `createdAt`.
4. **Error Handling**: Monitor STOMP error frames. Connection might fail if the JWT token is expired.
