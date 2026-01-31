package M.health;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class SecretaryPatientManagementActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private ListView lvPatients;
    private List<Patient> patients;
    private PatientAdapter adapter;
    private Button btnAddPatient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secretary_patient_management);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        
        if (!authManager.hasPermission("secretary_manage_patients")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        View headerView = findViewById(R.id.headerLayout);
        UIHelper.setupHeaderWithSignOut(this, headerView, "Gestion Patients", authManager);
        
        lvPatients = findViewById(R.id.lvPatients);
        btnAddPatient = findViewById(R.id.btnAddPatient);
        
        patients = new ArrayList<>();
        adapter = new PatientAdapter();
        lvPatients.setAdapter(adapter);
        
        loadPatients();
        
        btnAddPatient.setOnClickListener(v -> showAddPatientDialog());
    }

    private void loadPatients() {
        patients.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT u.id, u.full_name, u.email, u.phone, p.date_of_birth, p.blood_type " +
            "FROM users u LEFT JOIN patients p ON u.id = p.user_id " +
            "WHERE u.role = 'patient' AND u.is_active = 1 " +
            "ORDER BY u.full_name ASC", null);

        while (cursor.moveToNext()) {
            patients.add(new Patient(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5)
            ));
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    private void showAddPatientDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_patient, null);
        
        EditText etName = dialogView.findViewById(R.id.etPatientName);
        EditText etEmail = dialogView.findViewById(R.id.etPatientEmail);
        EditText etPhone = dialogView.findViewById(R.id.etPatientPhone);
        EditText etBirthDate = dialogView.findViewById(R.id.etPatientBirthDate);
        EditText etBloodType = dialogView.findViewById(R.id.etPatientBloodType);
        
        new AlertDialog.Builder(this)
            .setTitle("Ajouter Patient")
            .setView(dialogView)
            .setPositiveButton("Ajouter", (dialog, which) -> {
                String name = etName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                String birthDate = etBirthDate.getText().toString().trim();
                String bloodType = etBloodType.getText().toString().trim();
                
                if (!name.isEmpty() && !email.isEmpty()) {
                    addPatient(name, email, phone, birthDate, bloodType);
                } else {
                    Toast.makeText(this, "Nom et email obligatoires", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void addPatient(String name, String email, String phone, String birthDate, String bloodType) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Insert user
        ContentValues userValues = new ContentValues();
        userValues.put("full_name", name);
        userValues.put("email", email);
        userValues.put("password_hash", "patient123"); // Default password
        userValues.put("role", "patient");
        userValues.put("role_id", 3);
        userValues.put("phone", phone);
        userValues.put("is_active", 1);
        
        long userId = db.insert("users", null, userValues);
        
        if (userId != -1) {
            // Insert patient details
            ContentValues patientValues = new ContentValues();
            patientValues.put("user_id", userId);
            patientValues.put("date_of_birth", birthDate);
            patientValues.put("blood_type", bloodType);
            
            db.insert("patients", null, patientValues);
            
            Toast.makeText(this, "Patient ajouté avec succès", Toast.LENGTH_SHORT).show();
            loadPatients();
        } else {
            Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditPatientDialog(Patient patient) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_patient, null);
        
        EditText etName = dialogView.findViewById(R.id.etPatientName);
        EditText etEmail = dialogView.findViewById(R.id.etPatientEmail);
        EditText etPhone = dialogView.findViewById(R.id.etPatientPhone);
        EditText etBirthDate = dialogView.findViewById(R.id.etPatientBirthDate);
        EditText etBloodType = dialogView.findViewById(R.id.etPatientBloodType);
        
        // Pre-fill with existing data
        etName.setText(patient.name);
        etEmail.setText(patient.email);
        etPhone.setText(patient.phone != null ? patient.phone : "");
        etBirthDate.setText(patient.birthDate != null ? patient.birthDate : "");
        etBloodType.setText(patient.bloodType != null ? patient.bloodType : "");
        
        new AlertDialog.Builder(this)
            .setTitle("Modifier Patient")
            .setView(dialogView)
            .setPositiveButton("Modifier", (dialog, which) -> {
                updatePatient(patient.id, 
                    etName.getText().toString().trim(),
                    etEmail.getText().toString().trim(),
                    etPhone.getText().toString().trim(),
                    etBirthDate.getText().toString().trim(),
                    etBloodType.getText().toString().trim());
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void updatePatient(int patientId, String name, String email, String phone, String birthDate, String bloodType) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Update user
        ContentValues userValues = new ContentValues();
        userValues.put("full_name", name);
        userValues.put("email", email);
        userValues.put("phone", phone);
        
        int userResult = db.update("users", userValues, "id = ?", new String[]{String.valueOf(patientId)});
        
        // Update patient details
        ContentValues patientValues = new ContentValues();
        patientValues.put("date_of_birth", birthDate);
        patientValues.put("blood_type", bloodType);
        
        db.update("patients", patientValues, "user_id = ?", new String[]{String.valueOf(patientId)});
        
        if (userResult > 0) {
            Toast.makeText(this, "Patient modifié avec succès", Toast.LENGTH_SHORT).show();
            loadPatients();
        } else {
            Toast.makeText(this, "Erreur lors de la modification", Toast.LENGTH_SHORT).show();
        }
    }

    private class PatientAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return patients.size();
        }

        @Override
        public Object getItem(int position) {
            return patients.get(position);
        }

        @Override
        public long getItemId(int position) {
            return patients.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(SecretaryPatientManagementActivity.this)
                        .inflate(R.layout.item_patient_card, parent, false);
            }

            Patient patient = patients.get(position);
            
            TextView tvName = convertView.findViewById(R.id.tvPatientName);
            TextView tvEmail = convertView.findViewById(R.id.tvPatientEmail);
            TextView tvDetails = convertView.findViewById(R.id.tvPatientDetails);
            Button btnEdit = convertView.findViewById(R.id.btnEditPatient);

            tvName.setText(patient.name);
            tvEmail.setText(patient.email);
            tvDetails.setText("Tél: " + (patient.phone != null ? patient.phone : "N/A") + 
                            "\nNé le: " + (patient.birthDate != null ? patient.birthDate : "N/A") +
                            "\nGroupe: " + (patient.bloodType != null ? patient.bloodType : "N/A"));

            btnEdit.setOnClickListener(v -> showEditPatientDialog(patient));

            return convertView;
        }
    }

    private static class Patient {
        int id;
        String name, email, phone, birthDate, bloodType;
        
        Patient(int id, String name, String email, String phone, String birthDate, String bloodType) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.birthDate = birthDate;
            this.bloodType = bloodType;
        }
    }
}