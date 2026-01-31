package M.health;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class ViewPatientsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private ListView lvPatients;
    private List<PatientInfo> patients;
    private PatientListAdapter adapter;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_patients);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        lvPatients = findViewById(R.id.lvPatientsList);
        etSearch = findViewById(R.id.etSearchPatient);
        Button btnSearch = findViewById(R.id.btnSearchPatient);

        patients = new ArrayList<>();
        adapter = new PatientListAdapter();
        lvPatients.setAdapter(adapter);

        loadPatients("");

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            loadPatients(query);
        });
    }

    private void loadPatients(String searchQuery) {
        patients.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT u.id, u.full_name, u.email, u.phone, p.date_of_birth, p.blood_type, p.emergency_contact " +
                "FROM users u LEFT JOIN patients p ON u.id = p.user_id " +
                "WHERE u.role = 'patient' AND u.is_active = 1 ";

        if (!searchQuery.isEmpty()) {
            query += "AND (u.full_name LIKE ? OR u.email LIKE ?) ";
        }
        query += "ORDER BY u.full_name ASC";

        Cursor cursor = searchQuery.isEmpty() ? 
            db.rawQuery(query, null) : 
            db.rawQuery(query, new String[]{"%" + searchQuery + "%", "%" + searchQuery + "%"});

        while (cursor.moveToNext()) {
            patients.add(new PatientInfo(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5),
                cursor.getString(6)
            ));
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    private void showPatientDetails(PatientInfo patient) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_patient_details, null);

        TextView tvName = dialogView.findViewById(R.id.tvPatientDetailName);
        TextView tvEmail = dialogView.findViewById(R.id.tvPatientDetailEmail);
        TextView tvPhone = dialogView.findViewById(R.id.tvPatientDetailPhone);
        TextView tvBirthDate = dialogView.findViewById(R.id.tvPatientDetailBirthDate);
        TextView tvBloodType = dialogView.findViewById(R.id.tvPatientDetailBloodType);
        TextView tvEmergency = dialogView.findViewById(R.id.tvPatientDetailEmergency);

        tvName.setText(patient.name);
        tvEmail.setText(patient.email);
        tvPhone.setText(patient.phone != null ? patient.phone : "N/A");
        tvBirthDate.setText(patient.birthDate != null ? patient.birthDate : "N/A");
        tvBloodType.setText(patient.bloodType != null ? patient.bloodType : "N/A");
        tvEmergency.setText(patient.emergencyContact != null ? patient.emergencyContact : "N/A");

        new AlertDialog.Builder(this)
            .setTitle("Détails Patient")
            .setView(dialogView)
            .setPositiveButton("Fermer", null)
            .show();
    }

    private class PatientListAdapter extends BaseAdapter {
        @Override
        public int getCount() { return patients.size(); }

        @Override
        public Object getItem(int position) { return patients.get(position); }

        @Override
        public long getItemId(int position) { return patients.get(position).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ViewPatientsActivity.this)
                        .inflate(R.layout.item_patient_view, parent, false);
            }

            PatientInfo patient = patients.get(position);

            TextView tvName = convertView.findViewById(R.id.tvPatientViewName);
            TextView tvEmail = convertView.findViewById(R.id.tvPatientViewEmail);
            TextView tvPhone = convertView.findViewById(R.id.tvPatientViewPhone);
            Button btnDetails = convertView.findViewById(R.id.btnViewPatientDetails);

            tvName.setText(patient.name);
            tvEmail.setText(patient.email);
            tvPhone.setText(patient.phone != null ? patient.phone : "N/A");

            btnDetails.setOnClickListener(v -> showPatientDetails(patient));

            return convertView;
        }
    }

    public static class PatientInfo {
        int id;
        String name, email, phone, birthDate, bloodType, emergencyContact;

        PatientInfo(int id, String name, String email, String phone, String birthDate, String bloodType, String emergencyContact) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.birthDate = birthDate;
            this.bloodType = bloodType;
            this.emergencyContact = emergencyContact;
        }

        public int getId() { return id; }
        public String getFullName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getBirthDate() { return birthDate; }
        public String getBloodType() { return bloodType; }
        public String getEmergencyContact() { return emergencyContact; }
    }
}
