package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.PostLike;

import java.util.List;
import java.util.UUID;

public interface IPostLikeService {

    PostLike create(PostLike postLike);

    PostLike read(UUID id);

    PostLike update(PostLike postLike);

    boolean delete(UUID id);

    List<PostLike> getAll();

    PostLike toggle(UUID postId, UUID userId);

    boolean hasLiked(UUID postId, UUID userId);

    long countByPost(UUID postId);

    List<PostLike> getByUser(UUID userId);
}