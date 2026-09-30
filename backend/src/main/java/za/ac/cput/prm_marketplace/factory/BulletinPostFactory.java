package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.User;

public class BulletinPostFactory {

    public static BulletinPost createBulletinPost(User author, String title, String category) {
        return createBulletinPost(author, title, null, category, null);
    }

    public static BulletinPost createBulletinPost(User author, String title, String body, String category) {
        return createBulletinPost(author, title, body, category, null);
    }

    public static BulletinPost createBulletinPost(User author, String title, String body,
                                                  String category, String imageUrl) {
        if (author == null) {
            return null;
        }
        if (title == null || title.isBlank()) {
            return null;
        }

        return new BulletinPost.Builder()
                .setAuthor(author)
                .setTitle(title)
                .setBody(body == null ? "" : body)
                .setCategory(category)
                .setImageUrl(imageUrl)
                .build();
    }
}