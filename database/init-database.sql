CREATE SCHEMA "public";
CREATE SCHEMA "cron";
CREATE TYPE "check_in_status" AS ENUM('SUCCESS', 'FAILED');
CREATE TYPE "user_role" AS ENUM('ATTENDEE', 'HOST', 'STAFF');
CREATE TYPE "provider" AS ENUM('GOOGLE', 'CREDENTIALS');
CREATE TYPE "workshop_type" AS ENUM('FREE', 'PAID');
CREATE TYPE "registration_status" AS ENUM('RESERVED', 'CONFIRMED', 'CANCELLED');
CREATE TYPE "payment_provider" AS ENUM('SEPAY');
CREATE TYPE "payment_status" AS ENUM('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED');
CREATE TABLE "accounts" (
                            "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                            "user_id" uuid NOT NULL CONSTRAINT "accounts_user_id_key" UNIQUE,
                            "provider" provider NOT NULL,
                            "provider_id" varchar,
                            "email" varchar NOT NULL CONSTRAINT "accounts_email_key" UNIQUE,
                            "hashed_password" varchar,
                            "created_at" timestamp DEFAULT now() NOT NULL
);
CREATE TABLE "check_ins" (
                             "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                             "registration_id" uuid NOT NULL CONSTRAINT "check_ins_registration_id_key" UNIQUE,
                             "scanned_by_user_id" uuid,
                             "status" check_in_status NOT NULL,
                             "scanned_at" timestamp NOT NULL
);
CREATE TABLE "classes" (
                           "id" uuid PRIMARY KEY,
                           "class_name" varchar(100) NOT NULL CONSTRAINT "classes_class_name_key" UNIQUE
);
CREATE TABLE "data_imports" (
                                "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                "filename" varchar(512),
                                "status" varchar(255) NOT NULL,
                                "processed_rows" integer DEFAULT 0 NOT NULL,
                                "error_log" text,
                                "imported_at" timestamp DEFAULT now() NOT NULL,
                                CONSTRAINT "data_imports_status_check" CHECK (((status)::text = ANY (ARRAY[('SUCCESS'::character varying)::text, ('FAILED'::character varying)::text])))
);
CREATE TABLE "departments" (
                               "id" uuid PRIMARY KEY,
                               "department_name" varchar(100) NOT NULL CONSTRAINT "departments_department_name_key" UNIQUE
);
CREATE TABLE "majors" (
                          "id" uuid PRIMARY KEY,
                          "major_name" varchar(100) NOT NULL CONSTRAINT "majors_major_name_key" UNIQUE
);
CREATE TABLE "payments" (
                            "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                            "registration_id" uuid NOT NULL,
                            "amount" numeric(10, 2) NOT NULL,
                            "provider" varchar(255) NOT NULL,
                            "gateway" varchar(20),
                            "provider_transaction_id" varchar(255) CONSTRAINT "payments_provider_transaction_id_key" UNIQUE,
                            "bank_reference_code" varchar(255) CONSTRAINT "payments_bank_reference_code_key" UNIQUE,
                            "actual_content" text,
                            "status" varchar(255) DEFAULT 'PENDING',
                            "expired_at" timestamp,
                            "created_at" timestamp DEFAULT CURRENT_TIMESTAMP,
                            "updated_at" timestamp DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE "refresh_tokens" (
                                  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                  "token" varchar(255) NOT NULL CONSTRAINT "refresh_tokens_token_key" UNIQUE,
                                  "expiry_date" timestamp with time zone NOT NULL,
                                  "user_id" uuid NOT NULL
);
CREATE TABLE "registrations" (
                                 "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                 "workshop_id" uuid NOT NULL UNIQUE,
                                 "user_id" uuid NOT NULL UNIQUE,
                                 "status" varchar(255) DEFAULT 'RESERVED' NOT NULL,
                                 "is_present" boolean DEFAULT false NOT NULL,
                                 "expires_at" timestamp,
                                 "created_at" timestamp DEFAULT now() NOT NULL,
                                 "updated_at" timestamp DEFAULT now() NOT NULL,
                                 CONSTRAINT "idx_workshop_user" UNIQUE("workshop_id","user_id"),
                                 CONSTRAINT "uq_registrations_workshop_user" UNIQUE("workshop_id","user_id")
);
CREATE TABLE "speakers" (
                            "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                            "name" varchar(255) NOT NULL,
                            "title" varchar(255),
                            "organization" varchar(255),
                            "bio" text,
                            "email" varchar(255) NOT NULL,
                            "phone_number" varchar(255) NOT NULL,
                            "created_at" timestamp DEFAULT now() NOT NULL
);
CREATE TABLE "student_profiles" (
                                    "id" uuid PRIMARY KEY,
                                    "student_code" varchar(255) NOT NULL CONSTRAINT "student_profiles_student_code_key" UNIQUE,
                                    "department" varchar(255) NOT NULL,
                                    "major" varchar(255),
                                    "class_name" varchar(255),
                                    "name" varchar(255)
);
CREATE TABLE "users" (
                         "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                         "name" varchar(255) NOT NULL,
                         "role" user_role DEFAULT 'ATTENDEE' NOT NULL,
                         "created_at" timestamp DEFAULT now() NOT NULL
);
CREATE TABLE "workshop_speakers" (
                                     "workshop_id" uuid,
                                     "speaker_id" uuid,
                                     CONSTRAINT "workshop_speakers_pkey" PRIMARY KEY("workshop_id","speaker_id")
);
CREATE TABLE "workshops" (
                             "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                             "name" varchar(255) NOT NULL,
                             "host_id" uuid,
                             "room" varchar(100) NOT NULL,
                             "room_map" varchar(500),
                             "total_slots" integer NOT NULL,
                             "available_slots" integer NOT NULL,
                             "description" text,
                             "price" numeric(12, 2) DEFAULT '0' NOT NULL,
                             "type" varchar(255) DEFAULT 'FREE' NOT NULL,
                             "start_at" timestamp,
                             "end_at" timestamp,
                             "created_at" timestamp DEFAULT now() NOT NULL,
                             "updated_at" timestamp DEFAULT now() NOT NULL,
                             "speaker" varchar(100),
                             "pdf_url" varchar(500),
                             CONSTRAINT "chk_workshops_dates" CHECK (((end_at IS NULL) OR (start_at IS NULL) OR (end_at > start_at))),
                             CONSTRAINT "chk_workshops_slots" CHECK ((available_slots <= total_slots)),
                             CONSTRAINT "workshops_available_slots_check" CHECK ((available_slots >= 0)),
                             CONSTRAINT "workshops_total_slots_check" CHECK ((total_slots > 0))
);
CREATE TABLE "cron"."job" (
                              "jobid" bigserial PRIMARY KEY,
                              "schedule" text NOT NULL,
                              "command" text NOT NULL,
                              "nodename" text DEFAULT 'localhost' NOT NULL,
                              "nodeport" integer DEFAULT inet_server_port() NOT NULL,
                              "database" text DEFAULT current_database() NOT NULL,
                              "username" text DEFAULT CURRENT_USER NOT NULL UNIQUE,
                              "active" boolean DEFAULT true NOT NULL,
                              "jobname" text UNIQUE,
                              CONSTRAINT "jobname_username_uniq" UNIQUE("jobname","username")
);
ALTER TABLE "cron"."job" ENABLE ROW LEVEL SECURITY;
CREATE TABLE "cron"."job_run_details" (
                                          "jobid" bigint,
                                          "runid" bigserial PRIMARY KEY,
                                          "job_pid" integer,
                                          "database" text,
                                          "username" text,
                                          "command" text,
                                          "status" text,
                                          "return_message" text,
                                          "start_time" timestamp with time zone,
                                          "end_time" timestamp with time zone
);
ALTER TABLE "cron"."job_run_details" ENABLE ROW LEVEL SECURITY;
CREATE UNIQUE INDEX "accounts_email_key" ON "accounts" ("email");
CREATE UNIQUE INDEX "accounts_pkey" ON "accounts" ("id");
CREATE UNIQUE INDEX "accounts_user_id_key" ON "accounts" ("user_id");
CREATE UNIQUE INDEX "check_ins_pkey" ON "check_ins" ("id");
CREATE UNIQUE INDEX "check_ins_registration_id_key" ON "check_ins" ("registration_id");
CREATE UNIQUE INDEX "classes_class_name_key" ON "classes" ("class_name");
CREATE UNIQUE INDEX "classes_pkey" ON "classes" ("id");
CREATE UNIQUE INDEX "data_imports_pkey" ON "data_imports" ("id");
CREATE UNIQUE INDEX "departments_department_name_key" ON "departments" ("department_name");
CREATE UNIQUE INDEX "departments_pkey" ON "departments" ("id");
CREATE UNIQUE INDEX "majors_major_name_key" ON "majors" ("major_name");
CREATE UNIQUE INDEX "majors_pkey" ON "majors" ("id");
CREATE INDEX "idx_bank_reference_code" ON "payments" ("bank_reference_code");
CREATE UNIQUE INDEX "idx_payments_bank_reference" ON "payments" ("bank_reference_code");
CREATE INDEX "idx_payments_bank_reference_code" ON "payments" ("bank_reference_code");
CREATE INDEX "idx_payments_registration_id" ON "payments" ("registration_id");
CREATE INDEX "idx_registration_id" ON "payments" ("registration_id");
CREATE UNIQUE INDEX "payments_bank_reference_code_key" ON "payments" ("bank_reference_code");
CREATE UNIQUE INDEX "payments_pkey" ON "payments" ("id");
CREATE UNIQUE INDEX "payments_provider_transaction_id_key" ON "payments" ("provider_transaction_id");
CREATE UNIQUE INDEX "refresh_tokens_pkey" ON "refresh_tokens" ("id");
CREATE UNIQUE INDEX "refresh_tokens_token_key" ON "refresh_tokens" ("token");
CREATE INDEX "idx_registrations_status_expires" ON "registrations" ("status","expires_at");
CREATE UNIQUE INDEX "idx_registrations_workshop_user" ON "registrations" ("workshop_id","user_id");
CREATE INDEX "idx_status_expires" ON "registrations" ("status","expires_at");
CREATE UNIQUE INDEX "idx_workshop_user" ON "registrations" ("workshop_id","user_id");
CREATE UNIQUE INDEX "registrations_pkey" ON "registrations" ("id");
CREATE UNIQUE INDEX "uq_registrations_workshop_user" ON "registrations" ("workshop_id","user_id");
CREATE UNIQUE INDEX "speakers_pkey" ON "speakers" ("id");
CREATE UNIQUE INDEX "student_profiles_pkey" ON "student_profiles" ("id");
CREATE UNIQUE INDEX "student_profiles_student_code_key" ON "student_profiles" ("student_code");
CREATE UNIQUE INDEX "users_pkey" ON "users" ("id");
CREATE UNIQUE INDEX "workshop_speakers_pkey" ON "workshop_speakers" ("workshop_id","speaker_id");
CREATE UNIQUE INDEX "workshops_pkey" ON "workshops" ("id");
CREATE UNIQUE INDEX "job_pkey" ON "cron"."job" ("jobid");
CREATE UNIQUE INDEX "jobname_username_uniq" ON "cron"."job" ("jobname","username");
CREATE UNIQUE INDEX "job_run_details_pkey" ON "cron"."job_run_details" ("runid");
ALTER TABLE "accounts" ADD CONSTRAINT "fk_accounts_user" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE;
ALTER TABLE "check_ins" ADD CONSTRAINT "fk_checkins_registration" FOREIGN KEY ("registration_id") REFERENCES "registrations"("id") ON DELETE CASCADE;
ALTER TABLE "check_ins" ADD CONSTRAINT "fk_checkins_scanned_by" FOREIGN KEY ("scanned_by_user_id") REFERENCES "users"("id") ON DELETE SET NULL;
ALTER TABLE "refresh_tokens" ADD CONSTRAINT "fk_refresh_token_user" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE;
ALTER TABLE "registrations" ADD CONSTRAINT "fk_registrations_user" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE;
ALTER TABLE "registrations" ADD CONSTRAINT "fk_registrations_workshop" FOREIGN KEY ("workshop_id") REFERENCES "workshops"("id") ON DELETE CASCADE;
ALTER TABLE "workshop_speakers" ADD CONSTRAINT "fk_ws_speaker" FOREIGN KEY ("speaker_id") REFERENCES "speakers"("id") ON DELETE CASCADE;
ALTER TABLE "workshop_speakers" ADD CONSTRAINT "fk_ws_workshop" FOREIGN KEY ("workshop_id") REFERENCES "workshops"("id") ON DELETE CASCADE;
ALTER TABLE "workshops" ADD CONSTRAINT "fk_workshops_host" FOREIGN KEY ("host_id") REFERENCES "users"("id") ON DELETE SET NULL;

CREATE EXTENSION IF NOT EXISTS pg_cron;

CREATE OR REPLACE FUNCTION handle_expired_items()
RETURNS void AS $$
DECLARE
    vn_now timestamp := timezone('Asia/Ho_Chi_Minh', NOW());
BEGIN
    -- Xử lý Payments hết hạn
    UPDATE payments 
    SET status = 'FAILED', 
        updated_at = vn_now
    WHERE status = 'PENDING' 
      AND expired_at < vn_now;

    -- Hoàn trả slot cho Workshop
    WITH expired_regs AS (
        UPDATE registrations
        SET status = 'CANCELLED',
            updated_at = vn_now
        WHERE status = 'RESERVED'
          AND expires_at < vn_now
        RETURNING workshop_id
    ),
    summary AS (
        SELECT workshop_id, COUNT(*) as count_cancelled
        FROM expired_regs
        GROUP BY workshop_id
    )
    UPDATE workshops
    SET available_slots = workshops.available_slots + summary.count_cancelled
    FROM summary
    WHERE workshops.id = summary.workshop_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION handle_cleanup_expired_tokens()
RETURNS void AS $$
BEGIN
    DELETE FROM refresh_tokens 
    WHERE expiry_date < timezone('Asia/Ho_Chi_Minh', NOW());
END;
$$ LANGUAGE plpgsql;

SELECT cron.schedule('handle_expired_items_job', '* * * * *', 'SELECT handle_expired_items();');

SELECT cron.schedule('cleanup_expired_tokens_job', '0 * * * *', 'SELECT handle_cleanup_expired_tokens();');
