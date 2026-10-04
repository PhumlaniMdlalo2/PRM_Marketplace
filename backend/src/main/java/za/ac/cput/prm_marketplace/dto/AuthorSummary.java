package za.ac.cput.prm_marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import za.ac.cput.prm_marketplace.domain.User;

/**
 * The only shape of a user that other people get to see.
 *
 * Anything a reader can attribute a piece of content to needs a name and an avatar, and nothing
 * else. Email, phone, role and verification state stay off the wire: a bulletin post or a comment is
 * readable by every signed-in user, so shipping the whole {@link User} there hands each reader every
 * user's contact details. {@code /api/users/me} still returns the full record to its owner.
 *
 * <p>Records in this project are immutable and hold no logic, so the serializer that applies this
 * shape lives here too rather than in a separate class for three lines of field writing.
 */
public record AuthorSummary(String id, String name, String avatarUrl) {

    public static AuthorSummary of(User user) {
        if (user == null) {
            return null;
        }
        return new AuthorSummary(
                user.getId() == null ? null : user.getId().toString(),
                user.getName(),
                user.getAvatarUrl());
    }

    /**
     * Applied with {@code @JsonSerialize(using = AuthorSummary.Serializer.class)} on the {@code User}
     * relation of a content entity, so the property name in the response does not change and no
     * controller or service has to learn about this.
     *
     * <p>The Jackson 3 types here are not interchangeable with the Jackson 2 ones: an annotation from
     * the old package compiles fine against a transitive dependency and is then silently ignored, so
     * the shape of the response stays wide open with no error to explain why.
     */
    public static class Serializer extends ValueSerializer<User> {

        @Override
        public void serialize(User user, JsonGenerator gen, SerializationContext context) {
            if (user == null) {
                gen.writeNull();
                return;
            }
            gen.writeStartObject();
            gen.writeStringProperty("id", user.getId() == null ? null : user.getId().toString());
            gen.writeStringProperty("name", user.getName());
            gen.writeStringProperty("avatarUrl", user.getAvatarUrl());
            gen.writeEndObject();
        }
    }
}