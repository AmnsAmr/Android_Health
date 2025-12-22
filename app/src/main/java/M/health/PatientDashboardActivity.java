package M.health;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PatientDashboardActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    // UI Components
    private TextView tvPatientName, tvPatientId;
    private TextView tvNextApptDoctor, tvNextApptSpecialty, tvNextApptDate;
    private LinearLayout cardNextAppointment;
    private Button btnEditRdv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("user_session", MODE_PRIVATE);

        // Bind Views
        tvPatientName = findViewById(R.id.tvPatientName);
        tvPatientId = findViewById(R.id.tvPatientId);
        tvNextApptDoctor = findViewById(R.id.tvNextApptDoctor);
        tvNextApptSpecialty = findViewById(R.id.tvNextApptSpecialty);
        tvNextApptDate = findViewById(R.id.tvNextApptDate);
        cardNextAppointment = findViewById(R.id.cardRdvDoctor);
        btnEditRdv = findViewById(R.id.btnEditRdv);

        // Retrieve current User ID from session
        int userId = prefs.getInt("user_id", -1);

        if (userId == -1) {
            Toast.makeText(this, "Erreur: Session expirée", Toast.LENGTH_SHORT).show();
            finish(); // Close activity to prevent crash
            return;
        }

        // Load Data
        loadPatientInfo(userId);
        loadNextAppointment(userId);
    }

    private void loadPatientInfo(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT full_name FROM users WHERE id = ?", new String[]{String.valueOf(userId)});
            if (cursor.moveToFirst()) {
                String name = cursor.getString(0);
                tvPatientName.setText(name);
                tvPatientId.setText("ID: " + userId);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur chargement profil", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadNextAppointment(int patientId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            // JOIN query: Get Appointment Date + Doctor's Name (from users) + Specialization (from doctors)
            // Filtering by status 'scheduled' and limiting to 1 result
            String query = "SELECT u.full_name, d.specialization, a.appointment_datetime " +
                    "FROM appointments a " +
                    "JOIN users u ON a.doctor_id = u.id " +
                    "JOIN doctors d ON a.doctor_id = d.user_id " +
                    "WHERE a.patient_id = ? AND a.status = 'scheduled' " +
                    "ORDER BY a.appointment_datetime ASC LIMIT 1";

            cursor = db.rawQuery(query, new String[]{String.valueOf(patientId)});

            if (cursor.moveToFirst()) {
                // If data exists, show it
                String doctorName = cursor.getString(0);
                String specialization = cursor.getString(1);
                String date = cursor.getString(2);

                tvNextApptDoctor.setText("Dr. " + doctorName);
                tvNextApptSpecialty.setText(specialization);
                tvNextApptDate.setText(date);

                // Ensure card is visible
                cardNextAppointment.setVisibility(View.VISIBLE);
            } else {
                // No appointments found: Hide the card or show "No Appointments"
                tvNextApptDoctor.setText("Aucun rendez-vous");
                tvNextApptSpecialty.setText("");
                tvNextApptDate.setText("");
                btnEditRdv.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur chargement RDV", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }
    }
}