package M.health;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ManageAppointmentsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView rvAppointments;
    private Spinner spinnerFilterDoctor;
    private Spinner spinnerFilterStatus;
    private Button btnCreateAppointment;
    private ImageView btnBack;

    private ManageAppointmentAdapter appointmentAdapter;
    private List<AppointmentDetail> appointments;
    private List<Doctor> doctors;

    private int selectedDoctorId = -1; // -1 means all doctors
    private String selectedStatus = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_appointments);

        dbHelper = new DatabaseHelper(this);

        initializeViews();
        loadDoctors();
        setupSpinners();
        loadAppointments();
        setupListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        spinnerFilterDoctor = findViewById(R.id.spinnerFilterDoctor);
        spinnerFilterStatus = findViewById(R.id.spinnerFilterStatus);
        btnCreateAppointment = findViewById(R.id.btnCreateAppointment);
        rvAppointments = findViewById(R.id.rvAppointments);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        appointments = new ArrayList<>();
        appointmentAdapter = new ManageAppointmentAdapter(appointments, this::onAppointmentAction);
        rvAppointments.setAdapter(appointmentAdapter);
    }

    private void loadDoctors() {
        doctors = new ArrayList<>();
        doctors.add(new Doctor(-1, "Tous les médecins", "")); // Default option

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT u.id, u.full_name, d.specialization " +
                "FROM users u " +
                "JOIN doctors d ON u.id = d.user_id " +
                "WHERE u.role = 'doctor' AND u.is_active = 1";

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            doctors.add(new Doctor(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2)
            ));
        }
        cursor.close();
    }

    private void setupSpinners() {
        // Doctor spinner
        List<String> doctorNames = new ArrayList<>();
        for (Doctor doctor : doctors) {
            String displayName = doctor.getName();
            if (doctor.getId() != -1 && doctor.getSpecialization() != null) {
                displayName += " (" + doctor.getSpecialization() + ")";
            }
            doctorNames.add(displayName);
        }

        ArrayAdapter<String> doctorAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, doctorNames);
        doctorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterDoctor.setAdapter(doctorAdapter);

        // Status spinner
        String[] statuses = {"Tous", "Planifié", "Annulé", "Terminé"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterStatus.setAdapter(statusAdapter);

        // Spinner listeners
        spinnerFilterDoctor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDoctorId = doctors.get(position).getId();
                loadAppointments();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerFilterStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] statusValues = {"all", "scheduled", "cancelled", "completed"};
                selectedStatus = statusValues[position];
                loadAppointments();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadAppointments() {
        appointments.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Build dynamic query based on filters
        StringBuilder queryBuilder = new StringBuilder(
                "SELECT a.id, a.appointment_datetime, a.status, a.notes, " +
                        "p.full_name as patient_name, p.phone as patient_phone, " +
                        "d.full_name as doctor_name, doc.specialization " +
                        "FROM appointments a " +
                        "JOIN users p ON a.patient_id = p.id " +
                        "JOIN users d ON a.doctor_id = d.id " +
                        "LEFT JOIN doctors doc ON d.id = doc.user_id " +
                        "WHERE 1=1 "
        );

        List<String> selectionArgs = new ArrayList<>();

        if (selectedDoctorId != -1) {
            queryBuilder.append("AND a.doctor_id = ? ");
            selectionArgs.add(String.valueOf(selectedDoctorId));
        }

        if (!selectedStatus.equals("all")) {
            queryBuilder.append("AND a.status = ? ");
            selectionArgs.add(selectedStatus);
        }

        queryBuilder.append("ORDER BY a.appointment_datetime DESC");

        Cursor cursor = db.rawQuery(queryBuilder.toString(),
                selectionArgs.toArray(new String[0]));

        while (cursor.moveToNext()) {
            AppointmentDetail appointment = new AppointmentDetail();
            appointment.setId(cursor.getInt(0));
            appointment.setDateTime(cursor.getString(1));
            appointment.setStatus(cursor.getString(2));
            appointment.setNotes(cursor.getString(3));
            appointment.setPatientName(cursor.getString(4));
            appointment.setPatientPhone(cursor.getString(5));
            appointment.setDoctorName(cursor.getString(6));
            appointment.setSpecialization(cursor.getString(7));

            appointments.add(appointment);
        }
        cursor.close();

        appointmentAdapter.notifyDataSetChanged();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnCreateAppointment.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateAppointmentActivity.class);
            startActivity(intent);
        });
    }

    private void onAppointmentAction(AppointmentDetail appointment, String action) {
        Intent intent;
        switch (action) {
            case "edit":
                intent = new Intent(this, EditAppointmentActivity.class);
                intent.putExtra("appointment_id", appointment.getId());
                startActivity(intent);
                break;
            case "cancel":
                cancelAppointment(appointment.getId());
                break;
        }
    }

    private void cancelAppointment(int appointmentId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE appointments SET status = 'cancelled' WHERE id = ?",
                new String[]{String.valueOf(appointmentId)});
        loadAppointments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments();
    }

    // Inner classes
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

    static class AppointmentDetail {
        private int id;
        private String dateTime;
        private String status;
        private String notes;
        private String patientName;
        private String patientPhone;
        private String doctorName;
        private String specialization;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getDateTime() { return dateTime; }
        public void setDateTime(String dateTime) { this.dateTime = dateTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getPatientName() { return patientName; }
        public void setPatientName(String patientName) { this.patientName = patientName; }
        public String getPatientPhone() { return patientPhone; }
        public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
        public String getDoctorName() { return doctorName; }
        public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
        public String getSpecialization() { return specialization; }
        public void setSpecialization(String specialization) { this.specialization = specialization; }
    }
}