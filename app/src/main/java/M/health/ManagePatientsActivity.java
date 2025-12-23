package M.health;

import android.content.ContentValues;
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
    private ListView patientsListView;
    private ArrayAdapter<String> adapter;
    private List<Patient> patients;

    // Variable to store the current user's role
    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_patients);

        // Retrieve the role passed from the previous activity
        // If coming from Admin (and admin sends nothing), this might be null, which is fine
        currentUserRole = getIntent().getStringExtra("USER_ROLE");

        dbHelper = new DatabaseHelper(this);
        patientsListView = findViewById(R.id.patientsListView);
        patients = new ArrayList<>();

        patientsListView.setOnItemClickListener((parent, view, position, id) ->
                showPatientDetailsDialog(patients.get(position)));

        loadPatients();
    }

    // ... [loadPatients method remains exactly the same] ...
    private void loadPatients() {
        patients.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // Querying users and patients tables
        Cursor cursor = db.rawQuery(
                "SELECT u.id, u.full_name, u.email, p.date_of_birth, p.gender, p.blood_type " +
                        "FROM users u LEFT JOIN patients p ON u.id = p.user_id WHERE u.role = 'patient'", null);

        List<String> patientStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            Patient patient = new Patient(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5)
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
        layout.setPadding(50, 20, 50, 20); // Added some padding for better look

        TextView nameText = new TextView(this);
        nameText.setText("Nom: " + patient.fullName);
        layout.addView(nameText);

        TextView emailText = new TextView(this);
        emailText.setText("Email: " + patient.email);
        layout.addView(emailText);

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

        // --- DYNAMIC LOGIC STARTS HERE ---

        // Always add the Close button
        builder.setNegativeButton("Fermer", null);

        // Check if the user is a secretary
        boolean isSecretary = "secretary".equals(currentUserRole);

        // Only add 'Modifier' and 'Supprimer' buttons if the user is NOT a secretary
        if (!isSecretary) {
            builder.setPositiveButton("Modifier", (dialog, which) -> showEditPatientDialog(patient));
            builder.setNeutralButton("Supprimer", (dialog, which) -> deletePatient(patient.id));
        }

        // --- DYNAMIC LOGIC ENDS HERE ---

        builder.show();
    }

    // ... [Rest of the file: showEditPatientDialog, updatePatient, deletePatient, Patient class remain the same] ...

    // For context, here are the unchanged methods just to keep the file valid in your mind:
    private void showEditPatientDialog(Patient patient) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifier Patient");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText nameEdit = new EditText(this);
        nameEdit.setText(patient.fullName);
        nameEdit.setHint("Nom complet");
        layout.addView(nameEdit);

        EditText emailEdit = new EditText(this);
        emailEdit.setText(patient.email);
        emailEdit.setHint("Email");
        layout.addView(emailEdit);

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
            String name = nameEdit.getText().toString();
            String email = emailEdit.getText().toString();
            String dob = dobEdit.getText().toString();
            String gender = genderSpinner.getSelectedItem().toString();
            String blood = bloodEdit.getText().toString();
            updatePatient(patient.id, name, email, dob, gender, blood);
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void updatePatient(int id, String name, String email, String dob, String gender, String blood) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues userValues = new ContentValues();
        userValues.put("full_name", name);
        userValues.put("email", email);
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

        Toast.makeText(this, "Patient modifié", Toast.LENGTH_SHORT).show();
        loadPatients();
    }

    private void deletePatient(int id) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmer")
                .setMessage("Supprimer ce patient et toutes ses données?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    int result = db.delete("users", "id = ?", new String[]{String.valueOf(id)});
                    if (result > 0) {
                        Toast.makeText(this, "Patient supprimé", Toast.LENGTH_SHORT).show();
                        loadPatients();
                    }
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private static class Patient {
        int id;
        String fullName, email, dateOfBirth, gender, bloodType;

        Patient(int id, String fullName, String email, String dateOfBirth, String gender, String bloodType) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.dateOfBirth = dateOfBirth;
            this.gender = gender;
            this.bloodType = bloodType;
        }
    }
}