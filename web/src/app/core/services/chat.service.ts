import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EnvironmentService } from './environment.service';
import { WebSocketService } from './websocket.service';
import { WebChatMessage, WebConversation } from '../models/chat.model';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private http = inject(HttpClient);
  private env = inject(EnvironmentService);
  private ws = inject(WebSocketService);

  private readonly API_URL = `${this.env.getApiUrl()}/chat`;

  getUserConversations(email: string): Observable<WebConversation[]> {
    return this.http.get<WebConversation[]>(`${this.API_URL}/conversations/${email}`);
  }

  getConversationHistory(conversationId: number): Observable<WebChatMessage[]> {
    return this.http.get<WebChatMessage[]>(`${this.API_URL}/history/${conversationId}`);
  }

  getOrCreateConversation(participantEmails: string[]): Observable<WebConversation> {
    return this.http.post<WebConversation>(`${this.API_URL}/conversations`, participantEmails);
  }

  sendMessage(message: WebChatMessage): void {
    this.ws.publish('/app/chat.send', message);
  }

  subscribeToConversation(conversationId: number): Observable<WebChatMessage> {
    return this.ws.subscribeToChat(conversationId);
  }

  subscribeToAdminsTopic(): Observable<WebChatMessage> {
    return this.ws.subscribeToAdminsChat();
  }

  unsubscribeFromConversation(conversationId: number): void {
    this.ws.unsubscribeFromChat(conversationId);
  }
}
