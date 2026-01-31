-- Check messages table structure
PRAGMA table_info(messages);

-- Check all messages
SELECT * FROM messages;

-- Check all users
SELECT id, full_name, role FROM users;

-- Check messages with user details
SELECT 
    m.id,
    m.sender_id,
    sender.full_name as sender_name,
    sender.role as sender_role,
    m.receiver_id,
    receiver.full_name as receiver_name,
    receiver.role as receiver_role,
    m.message,
    m.is_urgent,
    m.sent_at
FROM messages m
JOIN users sender ON m.sender_id = sender.id
JOIN users receiver ON m.receiver_id = receiver.id;

-- Example: Insert test message from patient (id=3) to doctor (id=2)
-- INSERT INTO messages (sender_id, receiver_id, message, is_urgent) VALUES (3, 2, 'Test message from patient', 0);

-- Example: Insert test urgent message from secretary (id=4) to doctor (id=2)
-- INSERT INTO messages (sender_id, receiver_id, message, is_urgent) VALUES (4, 2, 'Urgent message from secretary', 1);
