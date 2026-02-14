export interface WebChatMessage {
  id?: number;
  conversationId: number;
  senderEmail: string;
  senderRole?: string;
  content: string;
  createdAt?: string;
}

export interface WebConversation {
  id: number;
  createdAt: string;
  participantEmails: string[];
  lastMessage?: WebChatMessage;
}
