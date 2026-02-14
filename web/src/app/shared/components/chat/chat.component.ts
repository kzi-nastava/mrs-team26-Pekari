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
  private adminSubscription?: Subscription;
  private conversationsSubscription?: Subscription;

  @ViewChild('scrollMe') private myScrollContainer!: ElementRef;


  constructor() {
    effect(() => {
      const user = this.currentUser();
      if (user) {
        this.loadConversations();
        if (user.role === 'admin') {
          this.subscribeToAdminsTopic();
        }
      } else {
        this.closeChat();
        this.adminSubscription?.unsubscribe();
      }
    });
  }

  ngOnInit(): void {}

  ngOnDestroy(): void {
    this.unsubscribeFromActiveChat();
    this.adminSubscription?.unsubscribe();
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

    // Direct support chat to admins
    this.chatService.getOrCreateConversation([user.email]).subscribe(conv => {
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
      this.messages.update(msgs => {
        if (msgs.find(m => m.id === msg.id)) return msgs;
        return [...msgs, msg];
      });
    });
  }

  private subscribeToAdminsTopic() {
    this.adminSubscription?.unsubscribe();
    this.adminSubscription = this.chatService.subscribeToAdminsTopic().subscribe(msg => {
      this.loadConversations();
      const active = this.activeConversation();
      if (active && active.id === msg.conversationId) {
        this.messages.update(msgs => {
          if (msgs.find(m => m.id === msg.id)) return msgs;
          return [...msgs, msg];
        });
      }
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

  getChatPartner(conv: WebConversation): string {
    const user = this.currentUser();
    if (!user) return 'Unknown';
    // Find the first email that is NOT the current user's email
    const partner = conv.participantEmails.find(e => e !== user.email);
    // If it's a support chat created by a user with only themselves as participant,
    // and we are an admin, partner will be that user.
    // If we are that user, partner will be undefined.
    return partner || 'Self (Support Chat)';
  }
}
