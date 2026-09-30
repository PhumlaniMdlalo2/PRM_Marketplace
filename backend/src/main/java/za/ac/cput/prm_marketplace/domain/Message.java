package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 2000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status = MessageStatus.SENT;

    @Column(updatable = false)
    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    protected Message() {
    }

    private Message(Builder builder) {
        this.id = builder.id;
        this.conversation = builder.conversation;
        this.sender = builder.sender;
        this.body = builder.body;
        this.status = builder.status;
        this.sentAt = builder.sentAt;
        this.readAt = builder.readAt;
    }

    public UUID getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public User getSender() {
        return sender;
    }

    public String getBody() {
        return body;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void markRead() {
        this.status = MessageStatus.READ;
        this.readAt = LocalDateTime.now();
    }

    public void markDelivered() {
        if (this.status == MessageStatus.SENT) {
            this.status = MessageStatus.DELIVERED;
        }
    }

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", conversation=" + (conversation == null ? null : conversation.getId()) +
                ", sender=" + (sender == null ? null : sender.getId()) +
                ", body='" + body + '\'' +
                ", status=" + status +
                ", sentAt=" + sentAt +
                ", readAt=" + readAt +
                '}';
    }

    public static class Builder {
        private UUID id;
        private Conversation conversation;
        private User sender;
        private String body;
        private MessageStatus status = MessageStatus.SENT;
        private LocalDateTime sentAt;
        private LocalDateTime readAt;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setConversation(Conversation conversation) {
            this.conversation = conversation;
            return this;
        }

        public Builder setSender(User sender) {
            this.sender = sender;
            return this;
        }

        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        public Builder setStatus(MessageStatus status) {
            this.status = status;
            return this;
        }

        public Builder setSentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public Builder setReadAt(LocalDateTime readAt) {
            this.readAt = readAt;
            return this;
        }

        public Builder copy(Message message) {
            this.id = message.id;
            this.conversation = message.conversation;
            this.sender = message.sender;
            this.body = message.body;
            this.status = message.status;
            this.sentAt = message.sentAt;
            this.readAt = message.readAt;
            return this;
        }

        public Message build() {
            return new Message(this);
        }
    }
}
