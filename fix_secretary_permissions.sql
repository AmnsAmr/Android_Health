-- Fix Secretary Permissions in Database
-- Run this SQL to fix secretary access issues

-- Add missing secretary permissions
INSERT OR REPLACE INTO permissions (role, permission, permission_key, category, role_required) VALUES 
('secretary', 'Manage Patients', 'secretary_manage_patients', 'administrative', 1),
('secretary', 'View Doctor Schedules', 'secretary_view_doctor_schedules', 'administrative', 1),
('secretary', 'Send Urgent Messages', 'secretary_send_urgent_messages', 'administrative', 1),
('secretary', 'Update Patient Profiles', 'secretary_update_patient_profiles', 'administrative', 1),
('secretary', 'Access Patient List', 'secretary_access_patient_list', 'administrative', 1);

-- Verify current secretary permissions
SELECT * FROM permissions WHERE role = 'secretary';

-- Create test secretary user if not exists
INSERT OR IGNORE INTO users (full_name, email, password_hash, role, role_id, is_active) VALUES 
('Secretary Test', 'secretary@test.com', 'secretary123', 'secretary', 4, 1);