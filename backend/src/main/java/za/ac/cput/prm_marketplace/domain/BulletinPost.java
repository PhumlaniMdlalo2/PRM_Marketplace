package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "bulletin_posts")
public class BulletinPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 5000)
    private String body;

    private String category;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "comment_count")
    private int commentCount;

    @Column(name = "like_count")
    private int likeCount;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    protected BulletinPost() {
    }

    private BulletinPost(Builder builder) {
        this.id = builder.id;
        this.author = builder.author;
        this.title = builder.title;
        this.body = builder.body;
        this.category = builder.category;
        this.imageUrl = builder.imageUrl;
        this.commentCount = builder.commentCount;
        this.likeCount = builder.likeCount;
        this.createdAt = builder.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BulletinPost)) return false;
        BulletinPost that = (BulletinPost) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BulletinPost{" +
                "id=" + id +
                ", author=" + author +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", commentCount=" + commentCount +
                ", likeCount=" + likeCount +
                ", createdAt=" + createdAt +
                '}';
    }

    public static class Builder {
        private UUID id;
        private User author;
        private String title;
        private String body;
        private String category;
        private String imageUrl;
        private int commentCount;
        private int likeCount;
        private LocalDateTime createdAt;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setAuthor(User author) {
            this.author = author;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        public Builder setCategory(String category) {
            this.category = category;
            return this;
        }

        public Builder setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder setCommentCount(int commentCount) {
            this.commentCount = commentCount;
            return this;
        }

        public Builder setLikeCount(int likeCount) {
            this.likeCount = likeCount;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(BulletinPost bulletinPost) {
            this.id = bulletinPost.id;
            this.author = bulletinPost.author;
            this.title = bulletinPost.title;
            this.body = bulletinPost.body;
            this.category = bulletinPost.category;
            this.imageUrl = bulletinPost.imageUrl;
            this.commentCount = bulletinPost.commentCount;
            this.likeCount = bulletinPost.likeCount;
            this.createdAt = bulletinPost.createdAt;
            return this;
        }

        public BulletinPost build() {
            return new BulletinPost(this);
        }
    }
}
