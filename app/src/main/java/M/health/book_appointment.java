package M.health;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
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

    // ✅ 2 spinners : Specialité puis Médecin
    private Spinner spinnerSpecialties, spinnerDoctors;

    private EditText etDate, etReason;
    private Button btnConfirm;

    // Spécialités
    private final List<String> specialties = new ArrayList<>();
    private ArrayAdapter<String> specialtyAdapter;

    // Médecins (filtrés)
    private final List<Integer> doctorIds = new ArrayList<>();
    private final List<String> doctorNames = new ArrayList<>();
    private ArrayAdapter<String> doctorAdapter;

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
        setupSpecialtySpinner(); // adapters + listener
        loadSpecialties();       // charge les catégories depuis DB (puis charge médecins du 1er item)
        setupDatePicker();
        setupConfirmButton();
    }

    private void initializeViews() {
        spinnerSpecialties = findViewById(R.id.spinnerSpecialties); // ✅ dans XML
        spinnerDoctors = findViewById(R.id.spinnerDoctors);
        etDate = findViewById(R.id.etDate);
        etReason = findViewById(R.id.etReason);
        btnConfirm = findViewById(R.id.btnConfirm);
    }

    // =======================
    // 1) Spinner Specialités
    // =======================
    private void setupSpecialtySpinner() {
        specialtyAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                specialties
        );
        specialtyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpecialties.setAdapter(specialtyAdapter);

        // Adapter médecins (vide au départ)
        doctorAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                doctorNames
        );
        doctorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDoctors.setAdapter(doctorAdapter);

        spinnerSpecialties.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSpecialty = specialties.get(position);

                // Si aucune spécialité
                if ("Aucune spécialité".equals(selectedSpecialty)) {
                    loadDoctorsBySpecialty(null);
                } else {
                    loadDoctorsBySpecialty(selectedSpecialty);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void loadSpecialties() {
        specialties.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            String query =
                    "SELECT DISTINCT d.specialization " +
                            "FROM doctors d " +
                            "WHERE d.specialization IS NOT NULL AND TRIM(d.specialization) <> '' " +
                            "ORDER BY d.specialization ASC";

            cursor = db.rawQuery(query, null);

            while (cursor.moveToNext()) {
                specialties.add(cursor.getString(0));
            }

            if (specialties.isEmpty()) {
                specialties.add("Aucune spécialité");
            }

            specialtyAdapter.notifyDataSetChanged();

            // Charger automatiquement les médecins de la 1ère spécialité
            String first = specialties.get(0);
            loadDoctorsBySpecialty("Aucune spécialité".equals(first) ? null : first);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur de chargement des spécialités", Toast.LENGTH_SHORT).show();

            specialties.clear();
            specialties.add("Aucune spécialité");
            specialtyAdapter.notifyDataSetChanged();

            loadDoctorsBySpecialty(null);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    // ============================
    // 2) Charger médecins filtrés
    // ============================
    private void loadDoctorsBySpecialty(String specialty) {
        doctorIds.clear();
        doctorNames.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            String query =
                    "SELECT u.id, u.full_name, d.specialization " +
                            "FROM users u " +
                            "JOIN doctors d ON u.id = d.user_id " +
                            "WHERE u.role = 'doctor' " +
                            (specialty != null ? " AND d.specialization = ? " : "") +
                            "ORDER BY u.full_name ASC";

            String[] args = (specialty != null) ? new String[]{specialty} : null;
            cursor = db.rawQuery(query, args);

            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String spec = cursor.getString(2);

                doctorIds.add(id);
                doctorNames.add("Dr. " + name + " - " + spec);
            }

            if (doctorNames.isEmpty()) {
                doctorNames.add("Aucun médecin dans cette spécialité");
                doctorIds.add(-1);
            }

            doctorAdapter.notifyDataSetChanged();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur de chargement des médecins", Toast.LENGTH_SHORT).show();

            doctorNames.clear();
            doctorIds.clear();
            doctorNames.add("Aucun médecin disponible");
            doctorIds.add(-1);

            doctorAdapter.notifyDataSetChanged();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    // =======================
    // DatePicker
    // =======================
    private void setupDatePicker() {
        etDate.setFocusable(false);
        etDate.setClickable(true);

        etDate.setOnClickListener(v -> {
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
        });
    }

    private void setupConfirmButton() {
        btnConfirm.setOnClickListener(v -> confirmBooking());
    }

    // =======================
    // Confirm booking
    // =======================
    private void confirmBooking() {
        // si aucun médecin dispo dans la spécialité sélectionnée
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

        String appointmentDatetime = convertToDatetime(date);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put("patient_id", patientId);
            values.put("doctor_id", doctorId);
            values.put("appointment_datetime", appointmentDatetime);
            values.put("notes", reason);          // ✅ colonne correcte
            values.put("status", "scheduled");
            values.put("created_by", "patient");  // ✅ contrainte CHECK

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
        } finally {
            db.close();
        }
    }

    private String convertToDatetime(String date) {
        try {
            String[] parts = date.split("/");
            if (parts.length == 3) {
                return parts[2] + "-" + parts[1] + "-" + parts[0] + " 14:00:00";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date + " 14:00:00";
    }
}
