package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;

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

    private String category;

    protected BulletinPost() {

    }

    private BulletinPost(Builder builder) {
        this.id = builder.id;
        this.author = builder.author;
        this.title = builder.title;
        this.category = builder.category;
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

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "BulletinPost{" +
                "id=" + id +
                ", author=" + author +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                '}';
    }

    public static class Builder {
        private UUID id;
        private User author;
        private String title;
        private String category;

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

        public Builder setCategory(String category) {
            this.category = category;
            return this;
        }

        public Builder copy(BulletinPost bulletinPost) {
            this.id = bulletinPost.id;
            this.author = bulletinPost.author;
            this.title = bulletinPost.title;
            this.category = bulletinPost.category;
            return this;
        }

        public BulletinPost build() {
            return new BulletinPost(this);
        }
    }
}