ALTER TABLE payments
    MODIFY COLUMN method ENUM('CARD', 'EFT', 'CASH_ON_PICKUP', 'WALLET', 'SANDBOX') NOT NULL,
    ADD COLUMN seller_user_id BINARY(16) NULL;

ALTER TABLE vendor_profiles
    ADD COLUMN payout_account_holder VARCHAR(120) NULL,
    ADD COLUMN payout_bank_name VARCHAR(120) NULL,
    ADD COLUMN payout_account_number VARCHAR(20) NULL,
    ADD COLUMN payout_branch_code VARCHAR(10) NULL,
    ADD COLUMN payout_account_type VARCHAR(40) NULL;
