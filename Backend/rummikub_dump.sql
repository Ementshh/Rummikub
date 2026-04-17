-- ============================================================
-- Rummikub Online - PostgreSQL Database Dump
-- DDL Schema + DML Seed Data
-- ============================================================

-- Bersihkan objek yang sudah ada (urutan terbalik dari dependensi)
DROP TABLE IF EXISTS game_tiles CASCADE;
DROP TABLE IF EXISTS table_sets CASCADE;
DROP TABLE IF EXISTS game_participants CASCADE;
DROP TABLE IF EXISTS games CASCADE;
DROP TABLE IF EXISTS tiles CASCADE;
DROP TABLE IF EXISTS users CASCADE;

DROP TYPE IF EXISTS tile_color CASCADE;
DROP TYPE IF EXISTS game_status CASCADE;
DROP TYPE IF EXISTS set_type CASCADE;
DROP TYPE IF EXISTS tile_location CASCADE;

-- ============================================================
-- EKSTENSI
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- ENUM TYPES (dibuat terlebih dahulu sebelum tabel)
-- ============================================================
CREATE TYPE tile_color    AS ENUM ('RED', 'BLUE', 'YELLOW', 'BLACK');
CREATE TYPE game_status   AS ENUM ('WAITING', 'IN_PROGRESS', 'FINISHED');
CREATE TYPE set_type      AS ENUM ('GROUP', 'RUN');
CREATE TYPE tile_location AS ENUM ('POOL', 'RACK', 'TABLE');

-- ============================================================
-- TABEL 1: users
-- ============================================================
CREATE TABLE users (
    id            UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    username      VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABEL 2: tiles (Master Statis - 106 ubin Rummikub)
-- ============================================================
CREATE TABLE tiles (
    id       INT        PRIMARY KEY,
    color    tile_color,               -- NULL untuk Joker
    number   INT,                      -- NULL untuk Joker
    is_joker BOOLEAN    NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_tile_number CHECK (
        (is_joker = TRUE  AND number IS NULL AND color IS NULL)
        OR
        (is_joker = FALSE AND number BETWEEN 1 AND 13 AND color IS NOT NULL)
    )
);

-- ============================================================
-- TABEL 3: games
-- ============================================================
CREATE TABLE games (
    id                          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    status                      game_status NOT NULL DEFAULT 'WAITING',
    current_turn_participant_id UUID,       -- FK ditambahkan setelah game_participants dibuat
    turn_started_at             TIMESTAMP,
    created_at                  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABEL 4: game_participants (Relasi Many-to-Many: users <-> games)
-- ============================================================
CREATE TABLE game_participants (
    id                   UUID     PRIMARY KEY DEFAULT uuid_generate_v4(),
    game_id              UUID     NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    user_id              UUID     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    turn_order           SMALLINT NOT NULL,
    score                INT      NOT NULL DEFAULT 0,
    has_done_initial_meld BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_turn_order CHECK (turn_order BETWEEN 1 AND 4),
    CONSTRAINT uq_game_user   UNIQUE (game_id, user_id),
    CONSTRAINT uq_game_turn   UNIQUE (game_id, turn_order)
);

-- Tambahkan FK dari games.current_turn_participant_id -> game_participants.id
-- (Deferred karena relasi sirkular)
ALTER TABLE games
    ADD CONSTRAINT fk_games_current_turn
    FOREIGN KEY (current_turn_participant_id)
    REFERENCES game_participants (id)
    ON DELETE SET NULL;

-- ============================================================
-- TABEL 5: table_sets (Grup/Run valid di meja)
-- ============================================================
CREATE TABLE table_sets (
    id       UUID     PRIMARY KEY DEFAULT uuid_generate_v4(),
    game_id  UUID     NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    set_type set_type NOT NULL
);

-- ============================================================
-- TABEL 6: game_tiles (Posisi 106 ubin dalam sebuah game)
-- ============================================================
CREATE TABLE game_tiles (
    id             UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    game_id        UUID          NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    tile_id        INT           NOT NULL REFERENCES tiles (id),
    location       tile_location NOT NULL DEFAULT 'POOL',
    participant_id UUID          REFERENCES game_participants (id) ON DELETE SET NULL,
    table_set_id   UUID          REFERENCES table_sets (id) ON DELETE SET NULL,

    CONSTRAINT uq_game_tile UNIQUE (game_id, tile_id),

    CONSTRAINT chk_tile_location_rack CHECK (
        location <> 'RACK' OR participant_id IS NOT NULL
    ),
    CONSTRAINT chk_tile_location_table CHECK (
        location <> 'TABLE' OR table_set_id IS NOT NULL
    ),
    CONSTRAINT chk_tile_location_pool CHECK (
        location <> 'POOL' OR (participant_id IS NULL AND table_set_id IS NULL)
    )
);

-- ============================================================
-- INDEXES pada kolom Foreign Key untuk optimasi query
-- ============================================================
-- game_participants
CREATE INDEX idx_gp_game_id  ON game_participants (game_id);
CREATE INDEX idx_gp_user_id  ON game_participants (user_id);

-- games
CREATE INDEX idx_games_current_turn ON games (current_turn_participant_id);

-- table_sets
CREATE INDEX idx_ts_game_id ON table_sets (game_id);

-- game_tiles
CREATE INDEX idx_gt_game_id        ON game_tiles (game_id);
CREATE INDEX idx_gt_tile_id        ON game_tiles (tile_id);
CREATE INDEX idx_gt_participant_id ON game_tiles (participant_id);
CREATE INDEX idx_gt_table_set_id   ON game_tiles (table_set_id);
CREATE INDEX idx_gt_location       ON game_tiles (location);

-- ============================================================
-- DML: Seed Data untuk tabel tiles (106 ubin)
-- 4 warna × 13 angka × 2 set = 104 ubin + 2 Joker
-- ============================================================
INSERT INTO tiles (id, color, number, is_joker) VALUES
-- ===== SET 1: RED 1-13 =====
( 1, 'RED',    1, FALSE),
( 2, 'RED',    2, FALSE),
( 3, 'RED',    3, FALSE),
( 4, 'RED',    4, FALSE),
( 5, 'RED',    5, FALSE),
( 6, 'RED',    6, FALSE),
( 7, 'RED',    7, FALSE),
( 8, 'RED',    8, FALSE),
( 9, 'RED',    9, FALSE),
(10, 'RED',   10, FALSE),
(11, 'RED',   11, FALSE),
(12, 'RED',   12, FALSE),
(13, 'RED',   13, FALSE),
-- ===== SET 1: BLUE 1-13 =====
(14, 'BLUE',   1, FALSE),
(15, 'BLUE',   2, FALSE),
(16, 'BLUE',   3, FALSE),
(17, 'BLUE',   4, FALSE),
(18, 'BLUE',   5, FALSE),
(19, 'BLUE',   6, FALSE),
(20, 'BLUE',   7, FALSE),
(21, 'BLUE',   8, FALSE),
(22, 'BLUE',   9, FALSE),
(23, 'BLUE',  10, FALSE),
(24, 'BLUE',  11, FALSE),
(25, 'BLUE',  12, FALSE),
(26, 'BLUE',  13, FALSE),
-- ===== SET 1: YELLOW 1-13 =====
(27, 'YELLOW',  1, FALSE),
(28, 'YELLOW',  2, FALSE),
(29, 'YELLOW',  3, FALSE),
(30, 'YELLOW',  4, FALSE),
(31, 'YELLOW',  5, FALSE),
(32, 'YELLOW',  6, FALSE),
(33, 'YELLOW',  7, FALSE),
(34, 'YELLOW',  8, FALSE),
(35, 'YELLOW',  9, FALSE),
(36, 'YELLOW', 10, FALSE),
(37, 'YELLOW', 11, FALSE),
(38, 'YELLOW', 12, FALSE),
(39, 'YELLOW', 13, FALSE),
-- ===== SET 1: BLACK 1-13 =====
(40, 'BLACK',   1, FALSE),
(41, 'BLACK',   2, FALSE),
(42, 'BLACK',   3, FALSE),
(43, 'BLACK',   4, FALSE),
(44, 'BLACK',   5, FALSE),
(45, 'BLACK',   6, FALSE),
(46, 'BLACK',   7, FALSE),
(47, 'BLACK',   8, FALSE),
(48, 'BLACK',   9, FALSE),
(49, 'BLACK',  10, FALSE),
(50, 'BLACK',  11, FALSE),
(51, 'BLACK',  12, FALSE),
(52, 'BLACK',  13, FALSE),
-- ===== SET 2: RED 1-13 =====
(53, 'RED',    1, FALSE),
(54, 'RED',    2, FALSE),
(55, 'RED',    3, FALSE),
(56, 'RED',    4, FALSE),
(57, 'RED',    5, FALSE),
(58, 'RED',    6, FALSE),
(59, 'RED',    7, FALSE),
(60, 'RED',    8, FALSE),
(61, 'RED',    9, FALSE),
(62, 'RED',   10, FALSE),
(63, 'RED',   11, FALSE),
(64, 'RED',   12, FALSE),
(65, 'RED',   13, FALSE),
-- ===== SET 2: BLUE 1-13 =====
(66, 'BLUE',   1, FALSE),
(67, 'BLUE',   2, FALSE),
(68, 'BLUE',   3, FALSE),
(69, 'BLUE',   4, FALSE),
(70, 'BLUE',   5, FALSE),
(71, 'BLUE',   6, FALSE),
(72, 'BLUE',   7, FALSE),
(73, 'BLUE',   8, FALSE),
(74, 'BLUE',   9, FALSE),
(75, 'BLUE',  10, FALSE),
(76, 'BLUE',  11, FALSE),
(77, 'BLUE',  12, FALSE),
(78, 'BLUE',  13, FALSE),
-- ===== SET 2: YELLOW 1-13 =====
(79, 'YELLOW',  1, FALSE),
(80, 'YELLOW',  2, FALSE),
(81, 'YELLOW',  3, FALSE),
(82, 'YELLOW',  4, FALSE),
(83, 'YELLOW',  5, FALSE),
(84, 'YELLOW',  6, FALSE),
(85, 'YELLOW',  7, FALSE),
(86, 'YELLOW',  8, FALSE),
(87, 'YELLOW',  9, FALSE),
(88, 'YELLOW', 10, FALSE),
(89, 'YELLOW', 11, FALSE),
(90, 'YELLOW', 12, FALSE),
(91, 'YELLOW', 13, FALSE),
-- ===== SET 2: BLACK 1-13 =====
(92,  'BLACK',   1, FALSE),
(93,  'BLACK',   2, FALSE),
(94,  'BLACK',   3, FALSE),
(95,  'BLACK',   4, FALSE),
(96,  'BLACK',   5, FALSE),
(97,  'BLACK',   6, FALSE),
(98,  'BLACK',   7, FALSE),
(99,  'BLACK',   8, FALSE),
(100, 'BLACK',   9, FALSE),
(101, 'BLACK',  10, FALSE),
(102, 'BLACK',  11, FALSE),
(103, 'BLACK',  12, FALSE),
(104, 'BLACK',  13, FALSE),
-- ===== JOKER ×2 =====
(105, NULL, NULL, TRUE),
(106, NULL, NULL, TRUE);
