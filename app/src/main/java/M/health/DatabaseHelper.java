package M.health;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "health_app.db";
    private static final int DATABASE_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Roles table
        db.execSQL("CREATE TABLE roles (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE NOT NULL," +
                "description TEXT," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        // Users table (extended)
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "full_name TEXT NOT NULL," +
                "email TEXT UNIQUE NOT NULL," +
                "password_hash TEXT NOT NULL," +
                "role TEXT CHECK (role IN ('patient','doctor','admin','secretary')) NOT NULL," +
                "role_id INTEGER," +
                "is_active INTEGER DEFAULT 1," +
                "last_login DATETIME," +
                "phone TEXT," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (role_id) REFERENCES roles(id))");

        // Patients table
        db.execSQL("CREATE TABLE patients (" +
                "user_id INTEGER PRIMARY KEY," +
                "date_of_birth DATE," +
                "gender TEXT," +
                "blood_type TEXT," +
                "emergency_contact TEXT," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");

        // Doctors table
        db.execSQL("CREATE TABLE doctors (" +
                "user_id INTEGER PRIMARY KEY," +
                "specialization TEXT," +
                "license_number TEXT UNIQUE," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");

        // Appointments table
        db.execSQL("CREATE TABLE appointments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "patient_id INTEGER NOT NULL," +
                "doctor_id INTEGER NOT NULL," +
                "appointment_datetime DATETIME NOT NULL," +
                "status TEXT CHECK (status IN ('scheduled','cancelled','completed')) DEFAULT 'scheduled'," +
                "notes TEXT," +
                "created_by TEXT CHECK (created_by IN ('patient','secretary','doctor'))," +
                "FOREIGN KEY (patient_id) REFERENCES users(id)," +
                "FOREIGN KEY (doctor_id) REFERENCES users(id))");

        // Medical records table
        db.execSQL("CREATE TABLE medical_records (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "patient_id INTEGER NOT NULL," +
                "doctor_id INTEGER NOT NULL," +
                "diagnosis TEXT," +
                "treatment TEXT," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (patient_id) REFERENCES users(id)," +
                "FOREIGN KEY (doctor_id) REFERENCES users(id))");

        // Test results table
        db.execSQL("CREATE TABLE test_results (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "patient_id INTEGER NOT NULL," +
                "doctor_id INTEGER NOT NULL," +
                "test_name TEXT," +
                "result TEXT," +
                "test_date DATE," +
                "FOREIGN KEY (patient_id) REFERENCES users(id)," +
                "FOREIGN KEY (doctor_id) REFERENCES users(id))");

        // Prescriptions table
        db.execSQL("CREATE TABLE prescriptions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "patient_id INTEGER NOT NULL," +
                "doctor_id INTEGER NOT NULL," +
                "medication TEXT NOT NULL," +
                "dosage TEXT," +
                "instructions TEXT," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (patient_id) REFERENCES users(id)," +
                "FOREIGN KEY (doctor_id) REFERENCES users(id))");

        // Prescription refill requests table
        db.execSQL("CREATE TABLE prescription_refill_requests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "prescription_id INTEGER NOT NULL," +
                "status TEXT CHECK (status IN ('pending','approved','rejected')) DEFAULT 'pending'," +
                "requested_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (prescription_id) REFERENCES prescriptions(id))");

        // Messages table
        db.execSQL("CREATE TABLE messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sender_id INTEGER NOT NULL," +
                "receiver_id INTEGER NOT NULL," +
                "message TEXT NOT NULL," +
                "is_urgent INTEGER DEFAULT 0," +
                "sent_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (sender_id) REFERENCES users(id)," +
                "FOREIGN KEY (receiver_id) REFERENCES users(id))");

        // Notifications table
        db.execSQL("CREATE TABLE notifications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "content TEXT NOT NULL," +
                "is_read INTEGER DEFAULT 0," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES users(id))");

        // Permissions table (extended)
        db.execSQL("CREATE TABLE permissions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "role TEXT NOT NULL," +
                "permission TEXT NOT NULL," +
                "permission_key TEXT UNIQUE," +
                "category TEXT," +
                "role_required INTEGER DEFAULT 0)");

        // Insert default roles
        db.execSQL("INSERT INTO roles (name, description) VALUES " +
                "('admin', 'Administrator with full access')," +
                "('doctor', 'Medical doctor with patient access')," +
                "('patient', 'Patient with limited access')," +
                "('secretary', 'Secretary with administrative access')");

        // Insert default permissions
        db.execSQL("INSERT INTO permissions (role, permission, permission_key, category, role_required) VALUES " +
                "('admin', 'Manage Users', 'admin_manage_users', 'administration', 1)," +
                "('admin', 'Manage Patients', 'admin_manage_patients', 'administration', 1)," +
                "('admin', 'View All Data', 'admin_view_all_data', 'administration', 1)," +
                "('doctor', 'View Patients', 'doctor_view_patients', 'medical', 1)," +
                "('doctor', 'Manage Appointments', 'doctor_manage_appointments', 'medical', 1)," +
                "('doctor', 'Access Medical Records', 'doctor_access_medical_records', 'medical', 1)," +
                "('doctor', 'Prescribe Medication', 'doctor_prescribe_medication', 'medical', 1)," +
                "('patient', 'Book Appointments', 'patient_book_appointments', 'patient', 1)," +
                "('patient', 'View Own Records', 'patient_view_own_records', 'patient', 1)," +
                "('patient', 'Message Doctor', 'patient_message_doctor', 'patient', 1)," +
                "('secretary', 'Manage Appointments', 'secretary_manage_appointments', 'administrative', 1)," +
                "('secretary', 'View Patient List', 'secretary_view_patient_list', 'administrative', 1)");

        // Insert default admin user
        db.execSQL("INSERT INTO users (full_name, email, password_hash, role, role_id, is_active) VALUES " +
                "('Admin', 'admin@health.com', 'admin123', 'admin', 1, 1)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add new columns to existing tables
            db.execSQL("ALTER TABLE users ADD COLUMN role_id INTEGER");
            db.execSQL("ALTER TABLE users ADD COLUMN is_active INTEGER DEFAULT 1");
            db.execSQL("ALTER TABLE users ADD COLUMN last_login DATETIME");
            
            db.execSQL("ALTER TABLE permissions ADD COLUMN permission_key TEXT");
            db.execSQL("ALTER TABLE permissions ADD COLUMN category TEXT");
            db.execSQL("ALTER TABLE permissions ADD COLUMN role_required INTEGER DEFAULT 0");
            
            // Create roles table
            db.execSQL("CREATE TABLE roles (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT UNIQUE NOT NULL," +
                    "description TEXT," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            
            // Insert default roles
            db.execSQL("INSERT INTO roles (name, description) VALUES " +
                    "('admin', 'Administrator with full access')," +
                    "('doctor', 'Medical doctor with patient access')," +
                    "('patient', 'Patient with limited access')," +
                    "('secretary', 'Secretary with administrative access')");
            
            // Update existing users with role_id
            db.execSQL("UPDATE users SET role_id = 1 WHERE role = 'admin'");
            db.execSQL("UPDATE users SET role_id = 2 WHERE role = 'doctor'");
            db.execSQL("UPDATE users SET role_id = 3 WHERE role = 'patient'");
            db.execSQL("UPDATE users SET role_id = 4 WHERE role = 'secretary'");
            
            // Clear and insert new permissions
            db.execSQL("DELETE FROM permissions");
            db.execSQL("INSERT INTO permissions (role, permission, permission_key, category, role_required) VALUES " +
                    "('admin', 'Manage Users', 'admin_manage_users', 'administration', 1)," +
                    "('admin', 'Manage Patients', 'admin_manage_patients', 'administration', 1)," +
                    "('admin', 'View All Data', 'admin_view_all_data', 'administration', 1)," +
                    "('doctor', 'View Patients', 'doctor_view_patients', 'medical', 1)," +
                    "('doctor', 'Manage Appointments', 'doctor_manage_appointments', 'medical', 1)," +
                    "('doctor', 'Access Medical Records', 'doctor_access_medical_records', 'medical', 1)," +
                    "('doctor', 'Prescribe Medication', 'doctor_prescribe_medication', 'medical', 1)," +
                    "('patient', 'Book Appointments', 'patient_book_appointments', 'patient', 1)," +
                    "('patient', 'View Own Records', 'patient_view_own_records', 'patient', 1)," +
                    "('patient', 'Message Doctor', 'patient_message_doctor', 'patient', 1)," +
                    "('secretary', 'Manage Appointments', 'secretary_manage_appointments', 'administrative', 1)," +
                    "('secretary', 'View Patient List', 'secretary_view_patient_list', 'administrative', 1)");
        }
    }
}