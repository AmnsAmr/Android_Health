package M.health;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class EditAppointmentActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private int appointmentId;

    private TextView tvPatientName;
    private EditText etDate, etTime;
    private Spinner spinnerStatus;
    private Button btnSaveChanges, btnCancelAppt;

    private static final String[] STATUS_OPTIONS = {"scheduled", "completed", "cancelled"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_appointment);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        // Get ID passed from SecretaryDashboard
        appointmentId = getIntent().getIntExtra("appointment_id", -1);
        if (appointmentId == -1 || !authManager.isLoggedIn()) {
            finish();
            return;
        }

        initializeViews();
        loadAppointmentDetails();
    }

    private void initializeViews() {
        tvPatientName = findViewById(R.id.tvPatientName);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnCancelAppt = findViewById(R.id.btnCancelAppt);

        // Setup Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, STATUS_OPTIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);

        btnSaveChanges.setOnClickListener(v -> saveChanges());
        btnCancelAppt.setOnClickListener(v -> confirmCancellation());
    }

    private void loadAppointmentDetails() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT u.full_name, a.appointment_datetime, a.status " +
                        "FROM appointments a " +
                        "JOIN users u ON a.patient_id = u.id " +
                        "WHERE a.id = ?",
                new String[]{String.valueOf(appointmentId)});

        if (cursor.moveToFirst()) {
            tvPatientName.setText(cursor.getString(0));

            String fullDateTime = cursor.getString(1); // Format: YYYY-MM-DD HH:MM:SS
            if (fullDateTime.length() >= 16) {
                etDate.setText(fullDateTime.substring(0, 10));
                etTime.setText(fullDateTime.substring(11, 16));
            }

            String currentStatus = cursor.getString(2);
            for (int i = 0; i < STATUS_OPTIONS.length; i++) {
                if (STATUS_OPTIONS[i].equals(currentStatus)) {
                    spinnerStatus.setSelection(i);
                    break;
                }
            }
        }
        cursor.close();
    }

    private void saveChanges() {
        String newDate = etDate.getText().toString();
        String newTime = etTime.getText().toString();
        String newStatus = spinnerStatus.getSelectedItem().toString();

        if (newDate.isEmpty() || newTime.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir la date et l'heure", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reconstruct DateTime string
        String newDateTime = newDate + " " + newTime + ":00";

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("appointment_datetime", newDateTime);
        values.put("status", newStatus);

        int rows = db.update("appointments", values, "id = ?",
                new String[]{String.valueOf(appointmentId)});

        if (rows > 0) {
            Toast.makeText(this, "Modifications enregistrées", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmCancellation() {
        new AlertDialog.Builder(this)
                .setTitle("Annuler le rendez-vous")
                .setMessage("Êtes-vous sûr de vouloir annuler ce rendez-vous ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put("status", "cancelled");

                    db.update("appointments", values, "id = ?",
                            new String[]{String.valueOf(appointmentId)});

                    Toast.makeText(this, "Rendez-vous annulé", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Non", null)
                .show();
    }
}