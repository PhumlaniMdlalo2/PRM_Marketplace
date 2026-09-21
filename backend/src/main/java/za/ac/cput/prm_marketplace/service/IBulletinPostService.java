package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.BulletinPost;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IBulletinPostService {

    BulletinPost create(BulletinPost bulletinPost);

    BulletinPost read(UUID id);

    BulletinPost update(BulletinPost bulletinPost);

    boolean delete(UUID id);

    List<BulletinPost> getAll();

    Optional<BulletinPost> findByAuthorId(UUID authorId);
}