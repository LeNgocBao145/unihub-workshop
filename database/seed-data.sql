
INSERT INTO "departments" ("id", "department_name") VALUES
                                                        ('d1111111-1111-1111-1111-111111111111', 'Công nghệ thông tin'),
                                                        ('d2222222-2222-2222-2222-222222222222', 'Kinh tế và Quản trị Kinh doanh');

INSERT INTO "majors" ("id", "major_name") VALUES
                                              ('m1111111-1111-1111-1111-111111111111', 'Kỹ thuật phần mềm'),
                                              ('m2222222-2222-2222-2222-222222222222', 'Digital Marketing');

INSERT INTO "classes" ("id", "class_name") VALUES
                                               ('c1111111-1111-1111-1111-111111111111', 'SE1501'),
                                               ('c2222222-2222-2222-2222-222222222222', 'SS1502');

-- ==============================================================================
-- 3. DỮ LIỆU NGƯỜI DÙNG & TÀI KHOẢN (Users, Student Profiles, Accounts)
-- ==============================================================================
-- Tạo 4 User với các Role khác nhau
INSERT INTO "users" ("id", "name", "role", "created_at") VALUES
                                                             ('u1111111-1111-1111-1111-111111111111', 'Host Nguyễn', 'HOST', now()),
                                                             ('u2222222-2222-2222-2222-222222222222', 'Staff Trần', 'STAFF', now()),
                                                             ('u3333333-3333-3333-3333-333333333333', 'Attendee Lê Văn A', 'ATTENDEE', now()),
                                                             ('u4444444-4444-4444-4444-444444444444', 'Attendee Phạm Thị B', 'ATTENDEE', now());


INSERT INTO "student_profiles" ("id", "student_code", "department", "major", "class_name", "name") VALUES
                                                                                                       ('u3333333-3333-3333-3333-333333333333', 'SE123456', 'Công nghệ thông tin', 'Kỹ thuật phần mềm', 'SE1501', 'Attendee Lê Văn A'),
                                                                                                       ('u4444444-4444-4444-4444-444444444444', 'SS654321', 'Kinh tế và Quản trị Kinh doanh', 'Digital Marketing', 'SS1502', 'Attendee Phạm Thị B');


INSERT INTO "accounts" ("id", "user_id", "provider", "provider_id", "email", "hashed_password", "created_at") VALUES
                                                                                                                  (gen_random_uuid(), 'u1111111-1111-1111-1111-111111111111', 'CREDENTIALS', NULL, 'host@hcmus.edu.vn', '$2a$10$Uj2aBIEpu/oMHgOzax/13.dTy3askZCbKRqBS.E8TVdIrVqh2CVAG', now()),
                                                                                                                  (gen_random_uuid(), 'u2222222-2222-2222-2222-222222222222', 'CREDENTIALS', NULL, 'staff@hcmus.edu.vn', '$2a$10$Uj2aBIEpu/oMHgOzax/13.dTy3askZCbKRqBS.E8TVdIrVqh2CVAG', now()),
                                                                                                                  (gen_random_uuid(), 'u3333333-3333-3333-3333-333333333333', 'CREDENTIALS', NULL, 'alevan@hcmus.edu.vn', '$2a$10$Uj2aBIEpu/oMHgOzax/13.dTy3askZCbKRqBS.E8TVdIrVqh2CVAG', now()),
                                                                                                                  (gen_random_uuid(), 'u4444444-4444-4444-4444-444444444444', 'CREDENTIALS', NULL, 'bphamthi@hcmus.edu.vn', '$2a$10$Uj2aBIEpu/oMHgOzax/13.dTy3askZCbKRqBS.E8TVdIrVqh2CVAG', now());

-- ==============================================================================
-- 4. DỮ LIỆU WORKSHOPS & SPEAKERS
-- ==============================================================================
-- Thêm Speakers
INSERT INTO "speakers" ("id", "name", "title", "organization", "bio", "email", "phone_number", "created_at") VALUES
                                                                                                                 ('s1111111-1111-1111-1111-111111111111', 'Tiến sĩ AI', 'Senior AI Engineer', 'Google', 'Chuyên gia Trí tuệ nhân tạo...', 'ai.expert@example.com', '0987654321', now()),
                                                                                                                 ('s2222222-2222-2222-2222-222222222222', 'Chuyên gia Marketing', 'CMO', 'Vinamilk', 'Chuyên gia chiến lược Marketing...', 'marketing.pro@example.com', '0123456789', now());

-- Thêm Workshops (1 Free, 1 Paid) do Host tổ chức
INSERT INTO "workshops" ("id", "name", "host_id", "room", "room_map", "total_slots", "available_slots", "description", "price", "type", "start_at", "end_at", "speaker", "pdf_url") VALUES
                                                                                                                                                                                        ('w1111111-1111-1111-1111-111111111111', 'Tương lai của AI trong lập trình', 'u1111111-1111-1111-1111-111111111111', 'Hội trường A', 'https://map.url/roomA', 100, 98, 'Tìm hiểu cách AI thay đổi ngành IT', 0, 'FREE', '2026-06-10 08:00:00', '2026-06-10 11:30:00', 'Tiến sĩ AI', 'https://pdf.url/ai.pdf'),
                                                                                                                                                                                        ('w2222222-2222-2222-2222-222222222222', 'Chiến lược Digital Marketing 2026', 'u1111111-1111-1111-1111-111111111111', 'Phòng 101', 'https://map.url/room101', 50, 49, 'Workshop chuyên sâu về Marketing', 150000.00, 'PAID', '2026-06-15 13:00:00', '2026-06-15 17:00:00', 'Chuyên gia Marketing', 'https://pdf.url/mkt.pdf');

-- Gắn Speaker vào Workshop
INSERT INTO "workshop_speakers" ("workshop_id", "speaker_id") VALUES
                                                                  ('w1111111-1111-1111-1111-111111111111', 's1111111-1111-1111-1111-111111111111'),
                                                                  ('w2222222-2222-2222-2222-222222222222', 's2222222-2222-2222-2222-222222222222');


-- Đăng ký Workshop FREE (Workshop w1111...)
INSERT INTO "registrations" ("id", "workshop_id", "user_id", "status", "is_present", "created_at") VALUES
                                                                                                       ('r1111111-1111-1111-1111-111111111111', 'w1111111-1111-1111-1111-111111111111', 'u3333333-3333-3333-3333-333333333333', 'CONFIRMED', false, now()),
                                                                                                       ('r2222222-2222-2222-2222-222222222222', 'w1111111-1111-1111-1111-111111111111', 'u4444444-4444-4444-4444-444444444444', 'CONFIRMED', false, now());

-- Đăng ký Workshop PAID (Workshop w2222...)
INSERT INTO "registrations" ("id", "workshop_id", "user_id", "status", "is_present", "created_at") VALUES
    ('r3333333-3333-3333-3333-333333333333', 'w2222222-2222-2222-2222-222222222222', 'u3333333-3333-3333-3333-333333333333', 'CONFIRMED', false, now());

-- Thanh toán cho đăng ký PAID (r3333...)
INSERT INTO "payments" ("id", "registration_id", "amount", "provider", "gateway", "provider_transaction_id", "bank_reference_code", "actual_content", "status", "created_at") VALUES
    (gen_random_uuid(), 'r3333333-3333-3333-3333-333333333333', 150000.00, 'SEPAY', 'MBBANK', 'TXN_987654321', 'REF_123456', 'SEPAY u3333 thanh toan mkt', 'SUCCESS', now());