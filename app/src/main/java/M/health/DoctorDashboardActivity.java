package M.health;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DoctorDashboardActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private TextView doctorNameText, doctorSpecialtyText, statsText;
    private int doctorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        // Validate session and permissions
        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            redirectToLogin();
            return;
        }

        if (!authManager.hasPermission("doctor_view_patients")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_doctor_dashboard);

        doctorId = authManager.getUserId();

        // Setup header
        ((TextView) findViewById(R.id.tvUserName)).setText(authManager.getCurrentUser().fullName);
        ((TextView) findViewById(R.id.tvUserRole)).setText("Médecin");
        ((TextView) findViewById(R.id.tvUserInfo)).setText("ID: " + doctorId);
        findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            authManager.logout();
            redirectToLogin();
        });

        doctorNameText = findViewById(R.id.tvDoctorName);
        doctorSpecialtyText = findViewById(R.id.tvDoctorSpecialty);
        statsText = findViewById(R.id.statsText);

        LinearLayout cardPatients = findViewById(R.id.cardPatients);
        LinearLayout cardAppointments = findViewById(R.id.cardAppointments);
        LinearLayout cardMedicalRecords = findViewById(R.id.cardMedicalRecords);
        LinearLayout cardPrescriptions = findViewById(R.id.cardPrescriptions);

        cardPatients.setOnClickListener(v -> {
            if (authManager.hasPermission("doctor_view_patients")) {
                startActivity(new Intent(this, DoctorPatientsActivity.class));
            } else {
                Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            }
        });

        cardAppointments.setOnClickListener(v -> {
            if (authManager.hasPermission("doctor_manage_appointments")) {
                startActivity(new Intent(this, DoctorAppointmentsActivity.class));
            } else {
                Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            }
        });

        cardMedicalRecords.setOnClickListener(v -> {
            if (authManager.hasPermission("doctor_access_medical_records")) {
                startActivity(new Intent(this, DoctorMedicalRecordsActivity.class));
            } else {
                Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            }
        });

        cardPrescriptions.setOnClickListener(v -> {
            if (authManager.hasPermission("doctor_prescribe_medication")) {
                startActivity(new Intent(this, DoctorPrescriptionsActivity.class));
            } else {
                Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout cardMessages = findViewById(R.id.cardMessages);
        cardMessages.setOnClickListener(v -> startActivity(new Intent(this, DoctorMessagesActivity.class)));
        
        // DEBUG: Add long click to open diagnostic
        cardMessages.setOnLongClickListener(v -> {
            startActivity(new Intent(this, DatabaseDiagnosticActivity.class));
            return true;
        });

        loadDoctorInfo();
        loadStats();
    }

    private void loadDoctorInfo() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT u.full_name, d.specialization FROM users u " +
                        "JOIN doctors d ON u.id = d.user_id WHERE u.id = ?",
                new String[]{String.valueOf(doctorId)});

        if (cursor.moveToFirst()) {
            doctorNameText.setText(cursor.getString(0));
            doctorSpecialtyText.setText(cursor.getString(1));
        }
        cursor.close();
    }

    private void loadStats() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder stats = new StringBuilder();

        // Today's appointments
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND date(appointment_datetime) = date('now')",
                new String[]{String.valueOf(doctorId)});
        if (cursor.moveToFirst()) {
            stats.append("Rendez-vous aujourd'hui: ").append(cursor.getInt(0)).append("\n");
        }
        cursor.close();

        // Total patients
        cursor = db.rawQuery(
                "SELECT COUNT(DISTINCT patient_id) FROM appointments WHERE doctor_id = ?",
                new String[]{String.valueOf(doctorId)});
        if (cursor.moveToFirst()) {
            stats.append("Patients total: ").append(cursor.getInt(0)).append("\n");
        }
        cursor.close();

        // Pending refill requests
        cursor = db.rawQuery(
                "SELECT COUNT(*) FROM prescription_refill_requests pr " +
                        "JOIN prescriptions p ON pr.prescription_id = p.id " +
                        "WHERE p.doctor_id = ? AND pr.status = 'pending'",
                new String[]{String.valueOf(doctorId)});
        if (cursor.moveToFirst()) {
            stats.append("Demandes renouvellement: ").append(cursor.getInt(0));
        }
        cursor.close();

        statsText.setText(stats.toString());
    }

    private void redirectToLogin() {
        Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            redirectToLogin();
            return;
        }
        loadStats(); // Refresh stats when returning to dashboard
    }
}