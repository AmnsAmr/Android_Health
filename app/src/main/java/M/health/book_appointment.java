package M.health;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class book_appointment extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private AuthManager authManager;

    private Spinner spinnerDoctors;
    private EditText etDate, etTime, etReason;
    private Button btnConfirm;

    private List<Integer> doctorIds;
    private List<String> doctorNames;
    private Calendar calendar;
    private int patientId;

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

        if (!authManager.hasPermission("patient_book_appointments")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_book_appointment);

        patientId = authManager.getUserId();
        calendar = Calendar.getInstance();

        // Setup reusable user profile header
        View userProfileHeader = findViewById(R.id.userProfileHeader);
        UIHelper.setupUserProfileHeader(this, userProfileHeader, authManager);

        initializeViews();
        loadDoctors();
        setupDatePicker();
        setupTimePicker();
        setupConfirmButton();
    }

    private void initializeViews() {
        spinnerDoctors = findViewById(R.id.spinnerDoctors);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etReason = findViewById(R.id.etReason);
        btnConfirm = findViewById(R.id.btnConfirm);
    }

    private void loadDoctors() {
        doctorIds = new ArrayList<>();
        doctorNames = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT u.id, u.full_name, d.specialization " +
                "FROM users u " +
                "JOIN doctors d ON u.id = d.user_id " +
                "WHERE u.role = 'doctor' AND u.is_active = 1 " +
                "ORDER BY u.full_name";

        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            doctorIds.add(cursor.getInt(0));
            doctorNames.add("Dr. " + cursor.getString(1) + " - " + cursor.getString(2));
        }
        cursor.close();

        if (doctorNames.isEmpty()) {
            doctorNames.add("Aucun médecin disponible");
            doctorIds.add(-1);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, doctorNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDoctors.setAdapter(adapter);
    }

    private void setupDatePicker() {
        etDate.setFocusable(false);
        etDate.setClickable(true);

        etDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
                        etDate.setText(sdf.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            // Don't allow past dates
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void setupTimePicker() {
        etTime.setFocusable(false);
        etTime.setClickable(true);

        etTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                    },
                    14, // Default to 14:00
                    0,
                    true // 24-hour format
            );
            timePickerDialog.show();
        });
    }

    private void setupConfirmButton() {
        btnConfirm.setOnClickListener(v -> confirmBooking());
    }

    private void confirmBooking() {
        if (doctorIds.isEmpty() || doctorIds.get(0) == -1) {
            Toast.makeText(this, "Aucun médecin disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String reason = etReason.getText().toString().trim();

        if (date.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner une date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (time.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner une heure", Toast.LENGTH_SHORT).show();
            return;
        }

        if (reason.isEmpty()) {
            Toast.makeText(this, "Veuillez décrire vos symptômes", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPosition = spinnerDoctors.getSelectedItemPosition();
        int doctorId = doctorIds.get(selectedPosition);

        // Convert date format from DD/MM/YYYY to YYYY-MM-DD HH:mm:ss
        String appointmentDatetime = convertToDatetime(date, time);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("patient_id", patientId);
        values.put("doctor_id", doctorId);
        values.put("appointment_datetime", appointmentDatetime);
        values.put("notes", reason);
        values.put("status", "scheduled");
        values.put("created_by", "patient");

        long result = db.insert("appointments", null, values);

        if (result != -1) {
            Toast.makeText(this, "Rendez-vous confirmé avec succès!", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Erreur lors de la réservation", Toast.LENGTH_SHORT).show();
        }
    }

    private String convertToDatetime(String date, String time) {
        try {
            String[] dateParts = date.split("/");
            if (dateParts.length == 3) {
                // Format: YYYY-MM-DD HH:mm:ss
                return dateParts[2] + "-" + dateParts[1] + "-" + dateParts[0] + " " + time + ":00";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date + " " + time + ":00";
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
        }
    }
}