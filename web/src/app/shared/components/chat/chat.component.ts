import { Component, inject, OnInit, OnDestroy, signal, effect, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { ChatService } from '../../../core/services/chat.service';
import { AuthService } from '../../../core/services/auth.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { WebChatMessage, WebConversation } from '../../../core/models/chat.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.css']
})
export class ChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  private chatService = inject(ChatService);
  private authService = inject(AuthService);
  private ws = inject(WebSocketService);

  currentUser = this.authService.currentUser;
  isOpen = signal(false);
  activeConversation = signal<WebConversation | null>(null);
  messages = signal<WebChatMessage[]>([]);
  conversations = signal<WebConversation[]>([]);
  newMessageContent = '';
  isConnected = toSignal(this.ws.isConnected$, { initialValue: false });

  private chatSubscription?: Subscription;
  private conversationsSubscription?: Subscription;

  @ViewChild('scrollMe') private myScrollContainer!: ElementRef;


  constructor() {
    effect(() => {
      const user = this.currentUser();
      if (user) {
        this.loadConversations();
      } else {
        this.closeChat();
      }
    });
  }

  ngOnInit(): void {}

  ngOnDestroy(): void {
    this.unsubscribeFromActiveChat();
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  toggleChat() {
    this.isOpen.update(v => !v);
    if (this.isOpen() && this.currentUser()?.role !== 'admin' && !this.activeConversation()) {
      this.startSupportChat();
    }
  }

  closeChat() {
    this.isOpen.set(false);
    this.unsubscribeFromActiveChat();
    this.activeConversation.set(null);
  }

  loadConversations() {
    const user = this.currentUser();
    if (!user) return;

    this.chatService.getUserConversations(user.email).subscribe(convs => {
      this.conversations.set(convs);
    });
  }

  startSupportChat() {
    const user = this.currentUser();
    if (!user || user.role === 'admin') return;

    // Direct support chat to admin/tech support
    this.chatService.getOrCreateConversation([user.email, 'support@test.com']).subscribe(conv => {
      this.selectConversation(conv);
    });
  }

  selectConversation(conv: WebConversation) {
    this.unsubscribeFromActiveChat();
    this.activeConversation.set(conv);
    this.loadHistory(conv.id);
    this.subscribeToChat(conv.id);
  }

  private loadHistory(convId: number) {
    this.chatService.getConversationHistory(convId).subscribe(msgs => {
      this.messages.set(msgs);
    });
  }

  private subscribeToChat(convId: number) {
    this.chatSubscription = this.chatService.subscribeToConversation(convId).subscribe(msg => {
      this.messages.update(msgs => [...msgs, msg]);
    });
  }

  private unsubscribeFromActiveChat() {
    const active = this.activeConversation();
    if (active) {
      this.chatService.unsubscribeFromConversation(active.id);
    }
    this.chatSubscription?.unsubscribe();
  }

  sendMessage() {
    if (!this.newMessageContent.trim() || !this.activeConversation()) return;

    const user = this.currentUser();
    if (!user) return;

    const msg: WebChatMessage = {
      conversationId: this.activeConversation()!.id,
      senderEmail: user.email,
      content: this.newMessageContent.trim()
    };

    this.chatService.sendMessage(msg);
    this.newMessageContent = '';
  }

  private scrollToBottom(): void {
    try {
      this.myScrollContainer.nativeElement.scrollTop = this.myScrollContainer.nativeElement.scrollHeight;
    } catch (err) {}
  }

  backToList() {
    this.unsubscribeFromActiveChat();
    this.activeConversation.set(null);
    this.messages.set([]);
    this.loadConversations();
  }
}
