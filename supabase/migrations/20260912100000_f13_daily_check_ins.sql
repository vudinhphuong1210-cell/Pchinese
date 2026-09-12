-- F13 daily streak: one authoritative check-in and XP award per learner-local date.
CREATE TABLE IF NOT EXISTS daily_check_ins (
    daily_check_in_id uuid NOT NULL DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    check_in_date date NOT NULL,
    awarded_xp integer NOT NULL DEFAULT 10,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_daily_check_ins PRIMARY KEY (daily_check_in_id),
    CONSTRAINT fk_daily_check_ins_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT uq_daily_check_ins_user_date UNIQUE (user_id, check_in_date),
    CONSTRAINT ck_daily_check_ins_awarded_xp CHECK (awarded_xp >= 0)
);

CREATE INDEX IF NOT EXISTS ix_daily_check_ins_user_date
    ON daily_check_ins (user_id, check_in_date DESC);
