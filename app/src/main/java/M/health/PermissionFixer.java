package M.health;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class PermissionFixer {
    private static final String TAG = "PermissionFixer";
    
    public static void fixSecretaryPermissions(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        try {
            // Add missing secretary permissions
            db.execSQL("INSERT OR REPLACE INTO permissions (role, permission, permission_key, category, role_required) VALUES " +
                "('secretary', 'Manage Patients', 'secretary_manage_patients', 'administrative', 1)");
            
            db.execSQL("INSERT OR REPLACE INTO permissions (role, permission, permission_key, category, role_required) VALUES " +
                "('secretary', 'View Doctor Schedules', 'secretary_view_doctor_schedules', 'administrative', 1)");
            
            db.execSQL("INSERT OR REPLACE INTO permissions (role, permission, permission_key, category, role_required) VALUES " +
                "('secretary', 'Send Urgent Messages', 'secretary_send_urgent_messages', 'administrative', 1)");
            
            db.execSQL("INSERT OR REPLACE INTO permissions (role, permission, permission_key, category, role_required) VALUES " +
                "('secretary', 'Update Patient Profiles', 'secretary_update_patient_profiles', 'administrative', 1)");
            
            db.execSQL("INSERT OR REPLACE INTO permissions (role, permission, permission_key, category, role_required) VALUES " +
                "('secretary', 'Access Patient List', 'secretary_access_patient_list', 'administrative', 1)");
            
            // Create test secretary user if not exists
            db.execSQL("INSERT OR IGNORE INTO users (full_name, email, password_hash, role, role_id, is_active) VALUES " +
                "('Secretary Test', 'secretary@test.com', 'secretary123', 'secretary', 4, 1)");
            
            Log.d(TAG, "Secretary permissions fixed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error fixing secretary permissions: " + e.getMessage());
        } finally {
            db.close();
        }
    }
}