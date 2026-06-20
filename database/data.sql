-- ===================================================================
-- DATABASE DUMMY DATA FOR TESTING (CLEARED / CORE ONLY)
-- ===================================================================

-- 1. Insert Users (Passwords are BCrypt hashed version of 'admin123', 'customer123', 'agent123')
INSERT INTO users (id, username, password, role, is_active) VALUES
(1, 'admin', '$2a$10$hKDVYxLefOc/V9GdnxyI9.2P8M2jPtbXz3W8/H3N2yQ4qR7jXp0/a', 'ADMIN', TRUE);
