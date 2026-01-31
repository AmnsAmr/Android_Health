package M.health;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DoctorProfileFixer {
    private static final String TAG = "DoctorProfileFixer";
    
    public static void fixMissingDoctorProfiles(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        try {
            // Find doctors in users table without doctor profiles
            Cursor cursor = db.rawQuery(
                "SELECT u.id, u.full_name FROM users u " +
                "LEFT JOIN doctors d ON u.id = d.user_id " +
                "WHERE u.role = 'doctor' AND d.user_id IS NULL", null);
            
            while (cursor.moveToNext()) {
                int userId = cursor.getInt(0);
                String fullName = cursor.getString(1);
                
                // Create doctor profile
                ContentValues values = new ContentValues();
                values.put("user_id", userId);
                values.put("specialization", "Généraliste");
                values.put("license_number", "LIC" + userId);
                
                long result = db.insert("doctors", null, values);
                
                if (result != -1) {
                    Log.d(TAG, "Created doctor profile for: " + fullName);
                }
            }
            cursor.close();
            
            // Add sample doctors if none exist
            Cursor doctorCount = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = 'doctor'", null);
            if (doctorCount.moveToFirst() && doctorCount.getInt(0) == 0) {
                addSampleDoctors(db);
            }
            doctorCount.close();
            
        } catch (Exception e) {
            Log.e(TAG, "Error fixing doctor profiles: " + e.getMessage());
        } finally {
            db.close();
        }
    }
    
    private static void addSampleDoctors(SQLiteDatabase db) {
        // Add sample doctors
        String[] doctors = {
            "Dr. Ahmed Hassan,Cardiologue,ahmed@health.com",
            "Dr. Sarah Martin,Pédiatre,sarah@health.com", 
            "Dr. Jean Dupont,Généraliste,jean@health.com"
        };
        
        for (String doctorData : doctors) {
            String[] parts = doctorData.split(",");
            
            // Insert user
            ContentValues userValues = new ContentValues();
            userValues.put("full_name", parts[0]);
            userValues.put("email", parts[2]);
            userValues.put("password_hash", "doctor123");
            userValues.put("role", "doctor");
            userValues.put("role_id", 2);
            userValues.put("is_active", 1);
            
            long userId = db.insert("users", null, userValues);
            
            if (userId != -1) {
                // Insert doctor profile
                ContentValues doctorValues = new ContentValues();
                doctorValues.put("user_id", userId);
                doctorValues.put("specialization", parts[1]);
                doctorValues.put("license_number", "LIC" + userId);
                
                db.insert("doctors", null, doctorValues);
            }
        }
    }
}