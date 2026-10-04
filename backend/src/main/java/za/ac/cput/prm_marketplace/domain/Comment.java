package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.annotation.JsonSerialize;
import za.ac.cput.prm_marketplace.dto.AuthorSummary;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class Comment {

    /**
 * Stays writable on purpose. A reply names its parent as a nested object, so Jackson has to be
 * able to read the parent's id out of the request. Mass assignment is handled in the service,
 * which rebuilds the entity instead of copying the body, and it proves that in CommentServiceImplTest.
 */
@Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The client names the post and, for a reply, the parent comment. Both are accepted on input
     * and hidden from output so a comment listing does not drag in a whole post and a nested reply
     * tree. The service re-reads both and rejects anything that does not exist or sits on another
     * post, so trusting the incoming id here does not trust the client.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private BulletinPost post;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private User author;

    /**
     * Accepted on input, hidden from output: the parent is a comment, and serialising it would nest
     * a whole reply tree inside every comment in a listing. A client still has to know which comment
     * a reply belongs to, so {@link #getParentId()} puts just that id back on the wire.
     */
    @ManyToOne
    @JoinColumn(name = "parent_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Comment parent;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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

    /** Serialised through {@link AuthorSummary}: a reader gets a name, not a contact list. */
    @Schema(implementation = AuthorSummary.class)

    @JsonSerialize(using = AuthorSummary.Serializer.class)
    public User getAuthor() {
        return author;
    }

    public Comment getParent() {
        return parent;
    }

    /**
     * The id of the comment this one replies to, or null when it is a top-level comment.
     *
     * <p>{@code parent} itself stays write-only, so this is the only trace of the relationship on the
     * way out. It is what lets a client rebuild the thread it is about to render.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public UUID getParentId() {
        return parent == null ? null : parent.getId();
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
        return id != null && id.equals(comment.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
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
