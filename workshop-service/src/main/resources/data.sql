CREATE TABLE workshops (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    host_id UUID,
    room VARCHAR(255) NOT NULL,
    room_map VARCHAR(512),
    total_slots INT NOT NULL,
    available_slots INT NOT NULL,
    description TEXT,
    price DECIMAL DEFAULT 0,
    type VARCHAR(10) DEFAULT 'FREE',
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_slots CHECK (available_slots <= total_slots)
);

INSERT INTO workshops (
    id, name, host_id, room, room_map,
    total_slots, available_slots,
    description, price, type,
    start_at, end_at, created_at, updated_at
) VALUES

      (
          RANDOM_UUID(),
          'Intro to Spring Boot',
          '11111111-1111-1111-1111-111111111111',
          'Room A1',
          'https://example.com/maps/a1.png',
          50,
          20,
          'Learn basics of Spring Boot',
          0,
          'FREE',
          TIMESTAMP '2026-05-10 09:00:00',
          TIMESTAMP '2026-05-10 12:00:00',
          CURRENT_TIMESTAMP,
          CURRENT_TIMESTAMP
      ),

      (
          RANDOM_UUID(),
          'Advanced System Design',
          '22222222-2222-2222-2222-222222222222',
          'Room B2',
          'https://example.com/maps/b2.png',
          30,
          5,
          'Deep dive into scalable systems',
          499000,
          'PAID',
          TIMESTAMP '2026-05-12 13:00:00',
          TIMESTAMP '2026-05-12 17:00:00',
          CURRENT_TIMESTAMP,
          CURRENT_TIMESTAMP
      ),

      (
          RANDOM_UUID(),
          'Docker for Beginners',
          '11111111-1111-1111-1111-111111111111',
          'Room C3',
          'https://example.com/maps/c3.png',
          40,
          40,
          'Hands-on Docker workshop',
          0,
          'FREE',
          TIMESTAMP '2026-05-15 08:30:00',
          TIMESTAMP '2026-05-15 11:30:00',
          CURRENT_TIMESTAMP,
          CURRENT_TIMESTAMP
      ),

      (
          RANDOM_UUID(),
          'Kubernetes in Practice',
          '33333333-3333-3333-3333-333333333333',
          'Room D4',
          'https://example.com/maps/d4.png',
          25,
          1,
          'Deploy and manage K8s cluster',
          799000,
          'PAID',
          TIMESTAMP '2026-05-18 14:00:00',
          TIMESTAMP '2026-05-18 18:00:00',
          CURRENT_TIMESTAMP,
          CURRENT_TIMESTAMP
      );
