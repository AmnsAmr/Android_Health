package M.health;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
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
    private SharedPreferences prefs;

    private Spinner spinnerDoctors;
    private EditText etDate, etReason;
    private Button btnConfirm;

    private List<Integer> doctorIds;
    private List<String> doctorNames;
    private Calendar calendar;
    private int patientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        patientId = prefs.getInt("user_id", -1);
        calendar = Calendar.getInstance();

        if (patientId == -1) {
            Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadDoctors();
        setupDatePicker();
        setupConfirmButton();
    }

    private void initializeViews() {
        spinnerDoctors = findViewById(R.id.spinnerDoctors);
        etDate = findViewById(R.id.etDate);
        etReason = findViewById(R.id.etReason);
        btnConfirm = findViewById(R.id.btnConfirm);
    }

    private void loadDoctors() {
        doctorIds = new ArrayList<>();
        doctorNames = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            String query = "SELECT u.id, u.full_name, d.specialization " +
                    "FROM users u " +
                    "JOIN doctors d ON u.id = d.user_id " +
                    "WHERE u.role = 'doctor'";

            cursor = db.rawQuery(query, null);

            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String specialization = cursor.getString(2);

                doctorIds.add(id);
                doctorNames.add("Dr. " + name + " - " + specialization);
            }

            if (doctorNames.isEmpty()) {
                doctorNames.add("Aucun médecin disponible");
                doctorIds.add(-1);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    doctorNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDoctors.setAdapter(adapter);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur de chargement des médecins", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void setupDatePicker() {
        etDate.setFocusable(false);
        etDate.setClickable(true);

        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        book_appointment.this,
                        (view, year, month, dayOfMonth) -> {
                            calendar.set(year, month, dayOfMonth);
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
                            etDate.setText(sdf.format(calendar.getTime()));
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

                datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                datePickerDialog.show();
            }
        });
    }

    private void setupConfirmButton() {
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmBooking();
            }
        });
    }

    private void confirmBooking() {
        if (doctorIds.isEmpty() || doctorIds.get(0) == -1) {
            Toast.makeText(this, "Aucun médecin disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = etDate.getText().toString().trim();
        String reason = etReason.getText().toString().trim();

        if (date.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner une date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (reason.isEmpty()) {
            Toast.makeText(this, "Veuillez décrire vos symptômes", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPosition = spinnerDoctors.getSelectedItemPosition();
        int doctorId = doctorIds.get(selectedPosition);

        // Convertir la date DD/MM/YYYY vers YYYY-MM-DD HH:mm:ss
        String appointmentDatetime = convertToDatetime(date);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put("patient_id", patientId);
            values.put("doctor_id", doctorId);
            values.put("appointment_datetime", appointmentDatetime);
            values.put("reason", reason);
            values.put("status", "scheduled");

            long result = db.insert("appointments", null, values);

            if (result != -1) {
                Toast.makeText(this, "Rendez-vous confirmé avec succès!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Erreur lors de la réservation", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String convertToDatetime(String date) {
        try {
            String[] parts = date.split("/");
            if (parts.length == 3) {
                // Format: YYYY-MM-DD 14:00:00 (heure par défaut)
                return parts[2] + "-" + parts[1] + "-" + parts[0] + " 14:00:00";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date + " 14:00:00";
    }
}
