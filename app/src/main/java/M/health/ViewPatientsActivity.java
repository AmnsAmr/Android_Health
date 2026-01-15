package M.health;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ViewPatientsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView rvPatients;
    private EditText etSearch;
    private ImageView btnBack;

    private PatientListAdapter patientAdapter;
    private List<PatientInfo> patients;
    private List<PatientInfo> filteredPatients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_patients);

        dbHelper = new DatabaseHelper(this);

        initializeViews();
        loadPatients();
        setupListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        etSearch = findViewById(R.id.etSearch);
        rvPatients = findViewById(R.id.rvPatients);

        rvPatients.setLayoutManager(new LinearLayoutManager(this));
        patients = new ArrayList<>();
        filteredPatients = new ArrayList<>();
        patientAdapter = new PatientListAdapter(filteredPatients, this::onPatientClick);
        rvPatients.setAdapter(patientAdapter);
    }

    private void loadPatients() {
        patients.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Secretary can only view administrative data, not medical records
        String query = "SELECT u.id, u.full_name, u.email, u.phone, " +
                "p.date_of_birth, p.gender, p.emergency_contact " +
                "FROM users u " +
                "LEFT JOIN patients p ON u.id = p.user_id " +
                "WHERE u.role = 'patient' AND u.is_active = 1 " +
                "ORDER BY u.full_name ASC";

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            PatientInfo patient = new PatientInfo();
            patient.setId(cursor.getInt(0));
            patient.setFullName(cursor.getString(1));
            patient.setEmail(cursor.getString(2));
            patient.setPhone(cursor.getString(3));
            patient.setDateOfBirth(cursor.getString(4));
            patient.setGender(cursor.getString(5));
            patient.setEmergencyContact(cursor.getString(6));

            patients.add(patient);
        }
        cursor.close();

        filteredPatients.clear();
        filteredPatients.addAll(patients);
        patientAdapter.notifyDataSetChanged();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPatients(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterPatients(String query) {
        filteredPatients.clear();

        if (query.isEmpty()) {
            filteredPatients.addAll(patients);
        } else {
            String lowerQuery = query.toLowerCase();
            for (PatientInfo patient : patients) {
                if (patient.getFullName().toLowerCase().contains(lowerQuery) ||
                        (patient.getPhone() != null && patient.getPhone().contains(query)) ||
                        (patient.getEmail() != null && patient.getEmail().toLowerCase().contains(lowerQuery))) {
                    filteredPatients.add(patient);
                }
            }
        }

        patientAdapter.notifyDataSetChanged();
    }

    private void onPatientClick(PatientInfo patient) {
        // Open patient administrative details (non-medical)
        Intent intent = new Intent(this, PatientAdminDetailsActivity.class);
        intent.putExtra("patient_id", patient.getId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPatients();
    }

    // Patient Info Model
    public static class PatientInfo {
        private int id;
        private String fullName;
        private String email;
        private String phone;
        private String dateOfBirth;
        private String gender;
        private String emergencyContact;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getEmergencyContact() { return emergencyContact; }
        public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    }
}