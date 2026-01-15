package M.health;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class ManagePatientsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private ListView patientsListView;
    private ArrayAdapter<String> adapter;
    private List<Patient> patients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        // Validate session
        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            redirectToLogin();
            return;
        }

        // Check permissions based on role
        boolean hasAccess = authManager.hasPermission("admin_manage_patients") ||
                authManager.hasPermission("secretary_view_patient_list");

        if (!hasAccess) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_manage_patients);

        patientsListView = findViewById(R.id.patientsListView);
        patients = new ArrayList<>();

        patientsListView.setOnItemClickListener((parent, view, position, id) ->
                showPatientDetailsDialog(patients.get(position)));

        loadPatients();
    }

    private void loadPatients() {
        patients.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT u.id, u.full_name, u.email, u.phone, p.date_of_birth, p.gender, p.blood_type " +
                        "FROM users u LEFT JOIN patients p ON u.id = p.user_id " +
                        "WHERE u.role = 'patient' AND u.is_active = 1 " +
                        "ORDER BY u.full_name",
                null);

        List<String> patientStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            Patient patient = new Patient(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6)
            );
            patients.add(patient);
            patientStrings.add(patient.fullName + " - " + patient.email);
        }
        cursor.close();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, patientStrings);
        patientsListView.setAdapter(adapter);
    }

    private void showPatientDetailsDialog(Patient patient) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Détails Patient");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView nameText = new TextView(this);
        nameText.setText("Nom: " + patient.fullName);
        layout.addView(nameText);

        TextView emailText = new TextView(this);
        emailText.setText("Email: " + patient.email);
        layout.addView(emailText);

        TextView phoneText = new TextView(this);
        phoneText.setText("Téléphone: " + (patient.phone != null ? patient.phone : "Non renseigné"));
        layout.addView(phoneText);

        TextView dobText = new TextView(this);
        dobText.setText("Date de naissance: " + (patient.dateOfBirth != null ? patient.dateOfBirth : "Non renseignée"));
        layout.addView(dobText);

        TextView genderText = new TextView(this);
        genderText.setText("Genre: " + (patient.gender != null ? patient.gender : "Non renseigné"));
        layout.addView(genderText);

        TextView bloodText = new TextView(this);
        bloodText.setText("Groupe sanguin: " + (patient.bloodType != null ? patient.bloodType : "Non renseigné"));
        layout.addView(bloodText);

        builder.setView(layout);
        builder.setNegativeButton("Fermer", null);

        // Only admins can modify or delete patients
        // Secretaries can only view administrative data
        if (authManager.hasPermission("admin_manage_patients")) {
            builder.setPositiveButton("Modifier", (dialog, which) -> showEditPatientDialog(patient));
            builder.setNeutralButton("Supprimer", (dialog, which) -> confirmDeletePatient(patient.id));
        }

        builder.show();
    }

    private void showEditPatientDialog(Patient patient) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifier Patient");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        EditText nameEdit = new EditText(this);
        nameEdit.setText(patient.fullName);
        nameEdit.setHint("Nom complet");
        layout.addView(nameEdit);

        EditText emailEdit = new EditText(this);
        emailEdit.setText(patient.email);
        emailEdit.setHint("Email");
        layout.addView(emailEdit);

        EditText phoneEdit = new EditText(this);
        phoneEdit.setText(patient.phone != null ? patient.phone : "");
        phoneEdit.setHint("Téléphone");
        layout.addView(phoneEdit);

        EditText dobEdit = new EditText(this);
        dobEdit.setText(patient.dateOfBirth != null ? patient.dateOfBirth : "");
        dobEdit.setHint("Date de naissance (YYYY-MM-DD)");
        layout.addView(dobEdit);

        Spinner genderSpinner = new Spinner(this);
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"", "M", "F"});
        genderSpinner.setAdapter(genderAdapter);
        if (patient.gender != null) {
            genderSpinner.setSelection(genderAdapter.getPosition(patient.gender));
        }
        layout.addView(genderSpinner);

        EditText bloodEdit = new EditText(this);
        bloodEdit.setText(patient.bloodType != null ? patient.bloodType : "");
        bloodEdit.setHint("Groupe sanguin");
        layout.addView(bloodEdit);

        builder.setView(layout);
        builder.setPositiveButton("Modifier", (dialog, which) -> {
            String name = nameEdit.getText().toString().trim();
            String email = emailEdit.getText().toString().trim();
            String phone = phoneEdit.getText().toString().trim();
            String dob = dobEdit.getText().toString().trim();
            String gender = genderSpinner.getSelectedItem().toString();
            String blood = bloodEdit.getText().toString().trim();

            if (!name.isEmpty() && !email.isEmpty()) {
                updatePatient(patient.id, name, email, phone, dob, gender, blood);
            } else {
                Toast.makeText(this, "Le nom et l'email sont obligatoires", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void updatePatient(int id, String name, String email, String phone, String dob, String gender, String blood) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues userValues = new ContentValues();
        userValues.put("full_name", name);
        userValues.put("email", email);
        if (!phone.isEmpty()) {
            userValues.put("phone", phone);
        }
        db.update("users", userValues, "id = ?", new String[]{String.valueOf(id)});

        ContentValues patientValues = new ContentValues();
        patientValues.put("user_id", id);
        if (!dob.isEmpty()) patientValues.put("date_of_birth", dob);
        if (!gender.isEmpty()) patientValues.put("gender", gender);
        if (!blood.isEmpty()) patientValues.put("blood_type", blood);

        int result = db.update("patients", patientValues, "user_id = ?", new String[]{String.valueOf(id)});
        if (result == 0) {
            db.insert("patients", null, patientValues);
        }

        Toast.makeText(this, "Patient modifié avec succès", Toast.LENGTH_SHORT).show();
        loadPatients();
    }

    private void confirmDeletePatient(int id) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmer la suppression")
                .setMessage("Supprimer ce patient et toutes ses données?\n\nCette action est irréversible.")
                .setPositiveButton("Supprimer", (dialog, which) -> deletePatient(id))
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void deletePatient(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete("users", "id = ?", new String[]{String.valueOf(id)});
        if (result > 0) {
            Toast.makeText(this, "Patient supprimé avec succès", Toast.LENGTH_SHORT).show();
            loadPatients();
        } else {
            Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
        }
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
            return;
        }
        loadPatients();
    }

    private static class Patient {
        int id;
        String fullName, email, phone, dateOfBirth, gender, bloodType;

        Patient(int id, String fullName, String email, String phone, String dateOfBirth, String gender, String bloodType) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.phone = phone;
            this.dateOfBirth = dateOfBirth;
            this.gender = gender;
            this.bloodType = bloodType;
        }
    }
}