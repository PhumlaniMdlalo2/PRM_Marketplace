package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private BulletinPost post;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Comment parent;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    protected Comment() {
    }

    private Comment(Builder builder) {
        this.id = builder.id;
        this.post = builder.post;
        this.author = builder.author;
        this.parent = builder.parent;
        this.body = builder.body;
        this.createdAt = builder.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public BulletinPost getPost() {
        return post;
    }

    public User getAuthor() {
        return author;
    }

    public Comment getParent() {
        return parent;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isReply() {
        return parent != null;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Comment)) return false;
        Comment comment = (Comment) o;
        return Objects.equals(id, comment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", post=" + (post == null ? null : post.getId()) +
                ", author=" + (author == null ? null : author.getId()) +
                ", parent=" + (parent == null ? null : parent.getId()) +
                ", body='" + body + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    public static class Builder {
        private UUID id;
        private BulletinPost post;
        private User author;
        private Comment parent;
        private String body;
        private LocalDateTime createdAt;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setPost(BulletinPost post) {
            this.post = post;
            return this;
        }

        public Builder setAuthor(User author) {
            this.author = author;
            return this;
        }

        public Builder setParent(Comment parent) {
            this.parent = parent;
            return this;
        }

        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(Comment comment) {
            this.id = comment.id;
            this.post = comment.post;
            this.author = comment.author;
            this.parent = comment.parent;
            this.body = comment.body;
            this.createdAt = comment.createdAt;
            return this;
        }

        public Comment build() {
            return new Comment(this);
        }
    }
}
