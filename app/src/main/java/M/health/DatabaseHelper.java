package M.health;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "health_app.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Users table
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "full_name TEXT NOT NULL," +
                "email TEXT UNIQUE NOT NULL," +
                "password_hash TEXT NOT NULL," +
                "role TEXT CHECK (role IN ('patient','doctor','admin','secretary')) NOT NULL," +
                "phone TEXT," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

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

        // Permissions table
        db.execSQL("CREATE TABLE permissions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "role TEXT NOT NULL," +
                "permission TEXT NOT NULL)");

        // Insert default admin user
        db.execSQL("INSERT INTO users (full_name, email, password_hash, role) VALUES " +
                "('Admin', 'admin@health.com', 'admin123', 'admin')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS permissions");
        db.execSQL("DROP TABLE IF EXISTS notifications");
        db.execSQL("DROP TABLE IF EXISTS messages");
        db.execSQL("DROP TABLE IF EXISTS prescription_refill_requests");
        db.execSQL("DROP TABLE IF EXISTS prescriptions");
        db.execSQL("DROP TABLE IF EXISTS test_results");
        db.execSQL("DROP TABLE IF EXISTS medical_records");
        db.execSQL("DROP TABLE IF EXISTS appointments");
        db.execSQL("DROP TABLE IF EXISTS doctors");
        db.execSQL("DROP TABLE IF EXISTS patients");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }
}