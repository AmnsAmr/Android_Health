package M.health;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateAppointmentActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    private Spinner spinnerPatient;
    private Spinner spinnerDoctor;
    private EditText etDate;
    private EditText etTime;
    private EditText etNotes;
    private Button btnCheckAvailability;
    private Button btnCreateAppointment;
    private TextView tvAvailabilityStatus;
    private ImageView btnBack;

    private List<Patient> patients;
    private List<Doctor> doctors;

    private int selectedPatientId = -1;
    private int selectedDoctorId = -1;
    private String selectedDate = "";
    private String selectedTime = "";
    private boolean isAvailabilityChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_appointment);

        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("HealthAppPrefs", MODE_PRIVATE);

        initializeViews();
        loadPatients();
        loadDoctors();
        setupListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        spinnerPatient = findViewById(R.id.spinnerPatient);
        spinnerDoctor = findViewById(R.id.spinnerDoctor);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etNotes = findViewById(R.id.etNotes);
        btnCheckAvailability = findViewById(R.id.btnCheckAvailability);
        btnCreateAppointment = findViewById(R.id.btnCreateAppointment);
        tvAvailabilityStatus = findViewById(R.id.tvAvailabilityStatus);
    }

    private void loadPatients() {
        patients = new ArrayList<>();
        patients.add(new Patient(-1, "Sélectionner un patient", "")); // Default

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, full_name, phone FROM users WHERE role = 'patient' AND is_active = 1 ORDER BY full_name",
                null
        );

        while (cursor.moveToNext()) {
            patients.add(new Patient(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2)
            ));
        }
        cursor.close();

        List<String> patientNames = new ArrayList<>();
        for (Patient patient : patients) {
            String displayName = patient.getName();
            if (patient.getId() != -1 && patient.getPhone() != null && !patient.getPhone().isEmpty()) {
                displayName += " (" + patient.getPhone() + ")";
            }
            patientNames.add(displayName);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, patientNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPatient.setAdapter(adapter);
    }

    private void loadDoctors() {
        doctors = new ArrayList<>();
        doctors.add(new Doctor(-1, "Sélectionner un médecin", "")); // Default

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT u.id, u.full_name, d.specialization " +
                "FROM users u " +
                "JOIN doctors d ON u.id = d.user_id " +
                "WHERE u.role = 'doctor' AND u.is_active = 1 " +
                "ORDER BY u.full_name";

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            doctors.add(new Doctor(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2)
            ));
        }
        cursor.close();

        List<String> doctorNames = new ArrayList<>();
        for (Doctor doctor : doctors) {
            String displayName = doctor.getName();
            if (doctor.getId() != -1 && doctor.getSpecialization() != null) {
                displayName += " - " + doctor.getSpecialization();
            }
            doctorNames.add(displayName);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, doctorNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDoctor.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());

        btnCheckAvailability.setOnClickListener(v -> checkAvailability());
        btnCreateAppointment.setOnClickListener(v -> createAppointment());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    selectedDate = sdf.format(calendar.getTime());

                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
                    etDate.setText(displayFormat.format(calendar.getTime()));

                    isAvailabilityChecked = false;
                    btnCreateAppointment.setEnabled(false);
                    tvAvailabilityStatus.setVisibility(View.GONE);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Don't allow past dates
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d:00", hourOfDay, minute);
                    etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));

                    isAvailabilityChecked = false;
                    btnCreateAppointment.setEnabled(false);
                    tvAvailabilityStatus.setVisibility(View.GONE);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true // 24-hour format
        );
        timePickerDialog.show();
    }

    private void checkAvailability() {
        // Validate inputs
        selectedPatientId = patients.get(spinnerPatient.getSelectedItemPosition()).getId();
        selectedDoctorId = doctors.get(spinnerDoctor.getSelectedItemPosition()).getId();

        if (selectedPatientId == -1) {
            Toast.makeText(this, "Veuillez sélectionner un patient", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDoctorId == -1) {
            Toast.makeText(this, "Veuillez sélectionner un médecin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner une date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner une heure", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for conflicts
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String appointmentDateTime = selectedDate + " " + selectedTime;

        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM appointments " +
                        "WHERE doctor_id = ? AND appointment_datetime = ? AND status = 'scheduled'",
                new String[]{String.valueOf(selectedDoctorId), appointmentDateTime}
        );

        boolean isAvailable = true;
        if (cursor.moveToFirst() && cursor.getInt(0) > 0) {
            isAvailable = false;
        }
        cursor.close();

        // Display result
        tvAvailabilityStatus.setVisibility(View.VISIBLE);
        if (isAvailable) {
            tvAvailabilityStatus.setText("✓ Créneau disponible");
            tvAvailabilityStatus.setTextColor(Color.parseColor("#4CAF50"));
            btnCreateAppointment.setEnabled(true);
            isAvailabilityChecked = true;
        } else {
            tvAvailabilityStatus.setText("✗ Créneau déjà réservé");
            tvAvailabilityStatus.setTextColor(Color.parseColor("#D32F2F"));
            btnCreateAppointment.setEnabled(false);
            isAvailabilityChecked = false;
        }
    }

    private void createAppointment() {
        if (!isAvailabilityChecked) {
            Toast.makeText(this, "Veuillez vérifier la disponibilité", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("patient_id", selectedPatientId);
        values.put("doctor_id", selectedDoctorId);
        values.put("appointment_datetime", selectedDate + " " + selectedTime);
        values.put("status", "scheduled");
        values.put("notes", etNotes.getText().toString().trim());
        values.put("created_by", "secretary");

        long result = db.insert("appointments", null, values);

        if (result != -1) {
            Toast.makeText(this, "Rendez-vous créé avec succès", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Erreur lors de la création", Toast.LENGTH_SHORT).show();
        }
    }

    // Inner classes
    static class Patient {
        private int id;
        private String name;
        private String phone;

        Patient(int id, String name, String phone) {
            this.id = id;
            this.name = name;
            this.phone = phone;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getPhone() { return phone; }
    }

    static class Doctor {
        private int id;
        private String name;
        private String specialization;

        Doctor(int id, String name, String specialization) {
            this.id = id;
            this.name = name;
            this.specialization = specialization;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getSpecialization() { return specialization; }
    }
}