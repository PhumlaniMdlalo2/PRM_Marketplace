-- =============================================================================
-- V1: initial marketplace schema.
--
-- Generated from the JPA entity model via Hibernate script generation, so this
-- file stays faithful to the @Entity definitions. Regenerate with
--   src/test/java/za/ac/cput/prm_marketplace/tools/SchemaDumpTest.java
-- and review the diff before changing it.
--
-- Existing non-empty databases are adopted through
-- spring.flyway.baseline-on-migrate, so V1 only executes on a fresh database.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Accounts and identity
-- -----------------------------------------------------------------------------
create table users (
    verified       bit         not null,
    created_at     datetime(6),
    id             binary(16)  not null,
    avatar_url     varchar(255),
    email          varchar(255) not null,
    name           varchar(255) not null,
    password_hash  varchar(255) not null,
    phone          varchar(255),
    role           enum ('FACULTY', 'RESIDENT', 'STUDENT', 'VENDOR'),
    primary key (id)
) engine = InnoDB;

create table addresses (
    is_default    bit,
    created_at    datetime(6),
    id            binary(16) not null,
    user_id       binary(16),
    city          varchar(255) not null,
    country       varchar(255) not null,
    line1         varchar(255) not null,
    line2         varchar(255),
    postal_code   varchar(255),
    province      varchar(255) not null,
    suburb        varchar(255) not null,
    primary key (id)
) engine = InnoDB;

create table vendor_profiles (
    rating_avg      decimal(38, 2),
    verified        bit         not null,
    created_at      datetime(6),
    id              binary(16)  not null,
    user_id         binary(16)  not null,
    business_name   varchar(255) not null,
    registration_no varchar(255),
    primary key (id)
) engine = InnoDB;

create table verification_codes (
    used       bit        not null,
    created_at datetime(6),
    expires_at datetime(6) not null,
    id         binary(16) not null,
    user_id    binary(16) not null,
    code       varchar(10) not null,
    primary key (id)
) engine = InnoDB;

create table password_reset_tokens (
    used       bit         not null,
    created_at datetime(6),
    expires_at datetime(6) not null,
    id         binary(16)  not null,
    user_id    binary(16)  not null,
    token      varchar(100) not null,
    primary key (id)
) engine = InnoDB;

-- -----------------------------------------------------------------------------
-- Catalogue
-- -----------------------------------------------------------------------------
create table products (
    active            bit                not null,
    price             decimal(10, 2)     not null,
    stock_quantity    integer            not null,
    created_at        datetime(6),
    id                binary(16)         not null,
    vendor_id         binary(16)         not null,
    description       varchar(1000),
    category          varchar(255),
    city              varchar(255),
    image_url         varchar(255),
    name              varchar(255)       not null,
    province          varchar(255),
    -- `condition` is reserved in MySQL, so the column is named product_condition.
    product_condition enum ('FAIR', 'GOOD', 'LIKE_NEW', 'NEW', 'POOR'),
    primary key (id)
) engine = InnoDB;

create table product_images (
    is_primary  bit,
    sort_order  integer,
    created_at  datetime(6),
    id          binary(16)     not null,
    product_id  binary(16)     not null,
    image_url   varchar(500)   not null,
    primary key (id)
) engine = InnoDB;

create table cart_items (
    quantity    integer      not null,
    added_at    datetime(6),
    id          binary(16)   not null,
    product_id  binary(16)   not null,
    user_id     binary(16)   not null,
    primary key (id)
) engine = InnoDB;

create table saved_items (
    saved_at    datetime(6),
    id          binary(16) not null,
    product_id  binary(16) not null,
    user_id     binary(16) not null,
    primary key (id)
) engine = InnoDB;

-- -----------------------------------------------------------------------------
-- Orders and payments
-- -----------------------------------------------------------------------------
create table orders (
    total_amount         decimal(12, 2) not null,
    created_at           datetime(6),
    updated_at           datetime(6),
    buyer_id             binary(16)    not null,
    id                   binary(16)    not null,
    shipping_address_id  binary(16),
    status               enum ('CANCELLED', 'CONFIRMED', 'DELIVERED', 'PENDING', 'REFUNDED', 'SHIPPED') not null,
    primary key (id)
) engine = InnoDB;

create table order_items (
    price_at_purchase  decimal(10, 2) not null,
    quantity           integer        not null,
    created_at         datetime(6),
    id                 binary(16)     not null,
    order_id           binary(16)     not null,
    product_id         binary(16)     not null,
    primary key (id)
) engine = InnoDB;

create table payments (
    amount                decimal(10, 2) not null,
    created_at            datetime(6),
    paid_at               datetime(6),
    id                    binary(16)     not null,
    order_id              binary(16)     not null,
    user_id               binary(16)     not null,
    transaction_reference varchar(255)   not null,
    method                enum ('CARD', 'EFT', 'WALLET') not null,
    status                enum ('COMPLETED', 'FAILED', 'PENDING', 'REFUNDED') not null,
    primary key (id)
) engine = InnoDB;

-- -----------------------------------------------------------------------------
-- Bulletin board
-- -----------------------------------------------------------------------------
create table bulletin_posts (
    comment_count  integer,
    like_count     integer,
    created_at     datetime(6),
    author_id      binary(16)   not null,
    id             binary(16)   not null,
    body           varchar(5000) not null,
    category       varchar(255),
    image_url      varchar(255),
    title          varchar(255) not null,
    primary key (id)
) engine = InnoDB;

create table comments (
    created_at  datetime(6),
    author_id   binary(16)   not null,
    id          binary(16)   not null,
    parent_id   binary(16),
    post_id     binary(16)   not null,
    body        varchar(2000) not null,
    primary key (id)
) engine = InnoDB;

create table post_likes (
    created_at  datetime(6),
    id          binary(16) not null,
    post_id     binary(16) not null,
    user_id     binary(16) not null,
    primary key (id)
) engine = InnoDB;

create table reviews (
    rating       integer        not null,
    created_at   datetime(6),
    id           binary(16)     not null,
    product_id   binary(16)     not null,
    reviewer_id  binary(16)     not null,
    comment      varchar(255)   not null,
    primary key (id)
) engine = InnoDB;

-- -----------------------------------------------------------------------------
-- Messaging and notifications
-- -----------------------------------------------------------------------------
create table conversations (
    created_at       datetime(6),
    last_message_at  datetime(6),
    buyer_id         binary(16) not null,
    id               binary(16) not null,
    product_id       binary(16),
    seller_id        binary(16) not null,
    primary key (id)
) engine = InnoDB;

create table messages (
    read_at          datetime(6),
    sent_at          datetime(6),
    conversation_id  binary(16)   not null,
    id               binary(16)   not null,
    sender_id        binary(16)   not null,
    body             varchar(2000) not null,
    status           enum ('DELIVERED', 'READ', 'SENT') not null,
    primary key (id)
) engine = InnoDB;

create table notifications (
    is_read    bit               not null,
    created_at  datetime(6),
    id          binary(16)        not null,
    user_id     binary(16)        not null,
    message     varchar(1000)     not null,
    title       varchar(255)      not null,
    type        enum ('MESSAGE', 'ORDER', 'PAYMENT', 'REVIEW', 'SYSTEM') not null,
    primary key (id)
) engine = InnoDB;

create table reports (
    created_at        datetime(6),
    resolved_at       datetime(6),
    id                binary(16)     not null,
    reporter_id       binary(16)     not null,
    target_id         binary(16),
    reason            varchar(1000)  not null,
    resolution_notes  varchar(1000),
    status            enum ('DISMISSED', 'OPEN', 'RESOLVED', 'UNDER_REVIEW') not null,
    target_type       enum ('BULLETIN_POST', 'COMMENT', 'MESSAGE', 'PRODUCT', 'REVIEW', 'USER') not null,
    primary key (id)
) engine = InnoDB;

-- -----------------------------------------------------------------------------
-- Unique constraints
-- -----------------------------------------------------------------------------
alter table users
    add constraint uk_users_email unique (email);
alter table vendor_profiles
    add constraint uk_vendor_profiles_user unique (user_id);
alter table conversations
    add constraint uk_conversation_participants unique (buyer_id, seller_id, product_id);
alter table payments
    add constraint uk_payments_transaction_reference unique (transaction_reference);
alter table post_likes
    add constraint uk_post_like_user unique (post_id, user_id);
alter table saved_items
    add constraint uk_saved_user_product unique (user_id, product_id);

-- -----------------------------------------------------------------------------
-- Foreign keys
-- -----------------------------------------------------------------------------
alter table addresses
    add constraint fk_addresses_user foreign key (user_id) references users (id);
alter table vendor_profiles
    add constraint fk_vendor_profiles_user foreign key (user_id) references users (id);
alter table verification_codes
    add constraint fk_verification_codes_user foreign key (user_id) references users (id);
alter table password_reset_tokens
    add constraint fk_password_reset_tokens_user foreign key (user_id) references users (id);

alter table products
    add constraint fk_products_vendor foreign key (vendor_id) references vendor_profiles (id);
alter table product_images
    add constraint fk_product_images_product foreign key (product_id) references products (id);
alter table cart_items
    add constraint fk_cart_items_product foreign key (product_id) references products (id);
alter table cart_items
    add constraint fk_cart_items_user foreign key (user_id) references users (id);
alter table saved_items
    add constraint fk_saved_items_product foreign key (product_id) references products (id);
alter table saved_items
    add constraint fk_saved_items_user foreign key (user_id) references users (id);

alter table orders
    add constraint fk_orders_buyer foreign key (buyer_id) references users (id);
alter table orders
    add constraint fk_orders_shipping_address foreign key (shipping_address_id) references addresses (id);
alter table order_items
    add constraint fk_order_items_order foreign key (order_id) references orders (id);
alter table order_items
    add constraint fk_order_items_product foreign key (product_id) references products (id);
alter table payments
    add constraint fk_payments_order foreign key (order_id) references orders (id);
alter table payments
    add constraint fk_payments_user foreign key (user_id) references users (id);

alter table bulletin_posts
    add constraint fk_bulletin_posts_author foreign key (author_id) references users (id);
alter table comments
    add constraint fk_comments_author foreign key (author_id) references users (id);
alter table comments
    add constraint fk_comments_parent foreign key (parent_id) references comments (id);
alter table comments
    add constraint fk_comments_post foreign key (post_id) references bulletin_posts (id);
alter table post_likes
    add constraint fk_post_likes_post foreign key (post_id) references bulletin_posts (id);
alter table post_likes
    add constraint fk_post_likes_user foreign key (user_id) references users (id);
alter table reviews
    add constraint fk_reviews_reviewer foreign key (reviewer_id) references users (id);

alter table conversations
    add constraint fk_conversations_buyer foreign key (buyer_id) references users (id);
alter table conversations
    add constraint fk_conversations_seller foreign key (seller_id) references users (id);
alter table conversations
    add constraint fk_conversations_product foreign key (product_id) references products (id);
alter table messages
    add constraint fk_messages_conversation foreign key (conversation_id) references conversations (id);
alter table messages
    add constraint fk_messages_sender foreign key (sender_id) references users (id);

alter table notifications
    add constraint fk_notifications_user foreign key (user_id) references users (id);
alter table reports
    add constraint fk_reports_reporter foreign key (reporter_id) references users (id);
