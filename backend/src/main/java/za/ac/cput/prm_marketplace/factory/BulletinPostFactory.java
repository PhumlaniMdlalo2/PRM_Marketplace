package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.User;

public class BulletinPostFactory {

    public static BulletinPost createBulletinPost(User author, String title, String category) {
        if (author == null) {
            return null;
        }
        if (title == null || title.isBlank()) {
            return null;
        }

        return new BulletinPost.Builder()
                .setAuthor(author)
                .setTitle(title)
                .setCategory(category)
                .build();
    }
}