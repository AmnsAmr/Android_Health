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

public class AdminDoctorManagementActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private ListView lvDoctors;
    private List<Doctor> doctors;
    private DoctorAdapter adapter;
    private Button btnAddDoctor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_doctor_management);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        
        if (!authManager.hasPermission("admin_manage_users")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        View headerView = findViewById(R.id.headerLayout);
        UIHelper.setupHeaderWithSignOut(this, headerView, "Gestion Médecins", authManager);
        
        lvDoctors = findViewById(R.id.lvDoctors);
        btnAddDoctor = findViewById(R.id.btnAddDoctor);
        
        doctors = new ArrayList<>();
        adapter = new DoctorAdapter();
        lvDoctors.setAdapter(adapter);
        
        loadDoctors();
        
        btnAddDoctor.setOnClickListener(v -> showAddDoctorDialog());
    }

    private void loadDoctors() {
        doctors.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT u.id, u.full_name, u.email, u.phone, u.is_active, " +
            "d.specialization, d.license_number " +
            "FROM users u LEFT JOIN doctors d ON u.id = d.user_id " +
            "WHERE u.role = 'doctor' " +
            "ORDER BY u.full_name ASC", null);

        while (cursor.moveToNext()) {
            doctors.add(new Doctor(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getInt(4) == 1,
                cursor.getString(5),
                cursor.getString(6)
            ));
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    private void showAddDoctorDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_doctor, null);
        
        EditText etName = dialogView.findViewById(R.id.etDoctorName);
        EditText etEmail = dialogView.findViewById(R.id.etDoctorEmail);
        EditText etPhone = dialogView.findViewById(R.id.etDoctorPhone);
        EditText etSpecialization = dialogView.findViewById(R.id.etDoctorSpecialization);
        EditText etLicenseNumber = dialogView.findViewById(R.id.etDoctorLicenseNumber);
        
        new AlertDialog.Builder(this)
            .setTitle("Ajouter Médecin")
            .setView(dialogView)
            .setPositiveButton("Ajouter", (dialog, which) -> {
                String name = etName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                String specialization = etSpecialization.getText().toString().trim();
                String licenseNumber = etLicenseNumber.getText().toString().trim();
                
                if (!name.isEmpty() && !email.isEmpty() && !specialization.isEmpty()) {
                    addDoctor(name, email, phone, specialization, licenseNumber);
                } else {
                    Toast.makeText(this, "Nom, email et spécialisation obligatoires", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void addDoctor(String name, String email, String phone, String specialization, String licenseNumber) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Insert user
        ContentValues userValues = new ContentValues();
        userValues.put("full_name", name);
        userValues.put("email", email);
        userValues.put("password_hash", "doctor123"); // Default password
        userValues.put("role", "doctor");
        userValues.put("role_id", 2);
        userValues.put("phone", phone);
        userValues.put("is_active", 1);
        
        long userId = db.insert("users", null, userValues);
        
        if (userId != -1) {
            // Insert doctor details
            ContentValues doctorValues = new ContentValues();
            doctorValues.put("user_id", userId);
            doctorValues.put("specialization", specialization);
            doctorValues.put("license_number", licenseNumber);
            
            long doctorResult = db.insert("doctors", null, doctorValues);
            
            if (doctorResult != -1) {
                Toast.makeText(this, "Médecin ajouté avec succès", Toast.LENGTH_SHORT).show();
                loadDoctors();
            } else {
                // Rollback user creation if doctor creation fails
                db.delete("users", "id = ?", new String[]{String.valueOf(userId)});
                Toast.makeText(this, "Erreur lors de l'ajout du profil médecin", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditDoctorDialog(Doctor doctor) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_doctor, null);
        
        EditText etName = dialogView.findViewById(R.id.etDoctorName);
        EditText etEmail = dialogView.findViewById(R.id.etDoctorEmail);
        EditText etPhone = dialogView.findViewById(R.id.etDoctorPhone);
        EditText etSpecialization = dialogView.findViewById(R.id.etDoctorSpecialization);
        EditText etLicenseNumber = dialogView.findViewById(R.id.etDoctorLicenseNumber);
        
        // Pre-fill with existing data
        etName.setText(doctor.name);
        etEmail.setText(doctor.email);
        etPhone.setText(doctor.phone != null ? doctor.phone : "");
        etSpecialization.setText(doctor.specialization != null ? doctor.specialization : "");
        etLicenseNumber.setText(doctor.licenseNumber != null ? doctor.licenseNumber : "");
        
        new AlertDialog.Builder(this)
            .setTitle("Modifier Médecin")
            .setView(dialogView)
            .setPositiveButton("Modifier", (dialog, which) -> {
                updateDoctor(doctor.id, 
                    etName.getText().toString().trim(),
                    etEmail.getText().toString().trim(),
                    etPhone.getText().toString().trim(),
                    etSpecialization.getText().toString().trim(),
                    etLicenseNumber.getText().toString().trim());
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void updateDoctor(int doctorId, String name, String email, String phone, String specialization, String licenseNumber) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Update user
        ContentValues userValues = new ContentValues();
        userValues.put("full_name", name);
        userValues.put("email", email);
        userValues.put("phone", phone);
        
        int userResult = db.update("users", userValues, "id = ?", new String[]{String.valueOf(doctorId)});
        
        // Update doctor details
        ContentValues doctorValues = new ContentValues();
        doctorValues.put("specialization", specialization);
        doctorValues.put("license_number", licenseNumber);
        
        db.update("doctors", doctorValues, "user_id = ?", new String[]{String.valueOf(doctorId)});
        
        if (userResult > 0) {
            Toast.makeText(this, "Médecin modifié avec succès", Toast.LENGTH_SHORT).show();
            loadDoctors();
        } else {
            Toast.makeText(this, "Erreur lors de la modification", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleDoctorStatus(Doctor doctor) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_active", doctor.isActive ? 0 : 1);
        
        int result = db.update("users", values, "id = ?", new String[]{String.valueOf(doctor.id)});
        
        if (result > 0) {
            String message = doctor.isActive ? "Médecin désactivé" : "Médecin activé";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            loadDoctors();
        } else {
            Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteDoctor(Doctor doctor) {
        new AlertDialog.Builder(this)
            .setTitle("Confirmer la suppression")
            .setMessage("Supprimer le Dr. " + doctor.name + " ? Cette action est irréversible.")
            .setPositiveButton("Supprimer", (dialog, which) -> {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                
                // Delete doctor profile first (foreign key constraint)
                db.delete("doctors", "user_id = ?", new String[]{String.valueOf(doctor.id)});
                
                // Delete user
                int result = db.delete("users", "id = ?", new String[]{String.valueOf(doctor.id)});
                
                if (result > 0) {
                    Toast.makeText(this, "Médecin supprimé avec succès", Toast.LENGTH_SHORT).show();
                    loadDoctors();
                } else {
                    Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private class DoctorAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return doctors.size();
        }

        @Override
        public Object getItem(int position) {
            return doctors.get(position);
        }

        @Override
        public long getItemId(int position) {
            return doctors.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(AdminDoctorManagementActivity.this)
                        .inflate(R.layout.item_doctor_card, parent, false);
            }

            Doctor doctor = doctors.get(position);
            
            TextView tvName = convertView.findViewById(R.id.tvDoctorName);
            TextView tvEmail = convertView.findViewById(R.id.tvDoctorEmail);
            TextView tvDetails = convertView.findViewById(R.id.tvDoctorDetails);
            TextView tvStatus = convertView.findViewById(R.id.tvDoctorStatus);
            Button btnEdit = convertView.findViewById(R.id.btnEditDoctor);
            Button btnToggleStatus = convertView.findViewById(R.id.btnToggleStatus);
            Button btnDelete = convertView.findViewById(R.id.btnDeleteDoctor);

            tvName.setText("Dr. " + doctor.name);
            tvEmail.setText(doctor.email);
            tvDetails.setText("Spécialité: " + (doctor.specialization != null ? doctor.specialization : "N/A") + 
                            "\nLicence: " + (doctor.licenseNumber != null ? doctor.licenseNumber : "N/A") +
                            "\nTél: " + (doctor.phone != null ? doctor.phone : "N/A"));
            
            tvStatus.setText(doctor.isActive ? "ACTIF" : "INACTIF");
            tvStatus.setTextColor(doctor.isActive ? 0xFF4CAF50 : 0xFFE74C3C);
            
            btnToggleStatus.setText(doctor.isActive ? "Désactiver" : "Activer");
            btnToggleStatus.setBackgroundTint(doctor.isActive ? 0xFFE74C3C : 0xFF4CAF50);

            btnEdit.setOnClickListener(v -> showEditDoctorDialog(doctor));
            btnToggleStatus.setOnClickListener(v -> toggleDoctorStatus(doctor));
            btnDelete.setOnClickListener(v -> deleteDoctor(doctor));

            return convertView;
        }
    }

    private static class Doctor {
        int id;
        String name, email, phone, specialization, licenseNumber;
        boolean isActive;
        
        Doctor(int id, String name, String email, String phone, boolean isActive, String specialization, String licenseNumber) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.isActive = isActive;
            this.specialization = specialization;
            this.licenseNumber = licenseNumber;
        }
    }
}