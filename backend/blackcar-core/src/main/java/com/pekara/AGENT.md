## Package featuring all active applicatoin features
1. repository for db interaciton
2. service layer
3. mapper for dto conversion
4. models for db entities
5. config for application properties


TODO:
1. Implement package that will be responsible for chat, which will lay on top of websockets
2. Create models for chat related entites. Proposition:


*Conversation*
Column Name,Type,Description
conversation_id,PK,Unique identifier for the chat.
created_at,Timestamp,When the chat started.

*Participant* - Join table
Column Name,Type,Description
conversation_id,FK,Foreign key to conversation table.
participant_id,PK,Unique identifier for the participant.


*Message*
Column Name,Type,Description
message_id,PK,Unique identifier.
conversation_id,FK,Which chat does this belong to?
sender_id,FK,Who sent it? (Links to Users).
content,Text,The message body.
created_at,Timestamp,When it was sent.



