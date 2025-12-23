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
    private TextView totalPatientsText, appointmentsCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_patients);

        dbHelper = new DatabaseHelper(this);
        patientsListView = findViewById(R.id.patientsListView);
        totalPatientsText = findViewById(R.id.totalPatientsText);
        appointmentsCountText = findViewById(R.id.appointmentsCountText);
        patients = new ArrayList<>();

        LinearLayout addPatientBtn = findViewById(R.id.addPatientBtn);
        LinearLayout searchPatientBtn = findViewById(R.id.searchPatientBtn);
        
        addPatientBtn.setOnClickListener(v -> showAddPatientDialog());
        searchPatientBtn.setOnClickListener(v -> showSearchDialog());

        patientsListView.setOnItemClickListener((parent, view, position, id) -> 
            showPatientOptionsDialog(patients.get(position)));

        loadPatients();
        loadStatistics();
    }

    private void loadPatients() {
        patients.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT u.id, u.full_name, u.email, p.blood_type, p.date_of_birth " +
            "FROM users u LEFT JOIN patients p ON u.id = p.user_id " +
            "WHERE u.role = 'patient' ORDER BY u.full_name", null);

        List<String> patientStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            Patient patient = new Patient(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4)
            );
            patients.add(patient);
            
            String bloodType = patient.bloodType != null ? patient.bloodType : "N/A";
            String patientDisplay = String.format("%-20s | %-8s | Actions", 
                patient.fullName.length() > 18 ? patient.fullName.substring(0, 18) + ".." : patient.fullName,
                bloodType);
            patientStrings.add(patientDisplay);
        }
        cursor.close();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, patientStrings) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTypeface(android.graphics.Typeface.MONOSPACE);
                textView.setTextSize(12);
                textView.setPadding(16, 12, 16, 12);
                return view;
            }
        };
        patientsListView.setAdapter(adapter);
    }

    private void loadStatistics() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // Total patients
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = 'patient'", null);
        if (cursor.moveToFirst()) {
            totalPatientsText.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();
        
        // Total appointments
        cursor = db.rawQuery("SELECT COUNT(*) FROM appointments", null);
        if (cursor.moveToFirst()) {
            appointmentsCountText.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rechercher Patient");

        EditText searchEdit = new EditText(this);
        searchEdit.setHint("Nom ou email");
        builder.setView(searchEdit);

        builder.setPositiveButton("Rechercher", (dialog, which) -> {
            String query = searchEdit.getText().toString();
            if (!query.isEmpty()) {
                searchPatients(query);
            }
        });
        builder.setNegativeButton("Tout afficher", (dialog, which) -> loadPatients());
        builder.show();
    }

    private void searchPatients(String query) {
        patients.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT u.id, u.full_name, u.email, p.blood_type, p.date_of_birth " +
            "FROM users u LEFT JOIN patients p ON u.id = p.user_id " +
            "WHERE u.role = 'patient' AND (u.full_name LIKE ? OR u.email LIKE ?) " +
            "ORDER BY u.full_name", 
            new String[]{"%" + query + "%", "%" + query + "%"});

        List<String> patientStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            Patient patient = new Patient(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4)
            );
            patients.add(patient);
            
            String bloodType = patient.bloodType != null ? patient.bloodType : "N/A";
            String patientDisplay = String.format("%-20s | %-8s | Actions", 
                patient.fullName.length() > 18 ? patient.fullName.substring(0, 18) + ".." : patient.fullName,
                bloodType);
            patientStrings.add(patientDisplay);
        }
        cursor.close();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, patientStrings) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTypeface(android.graphics.Typeface.MONOSPACE);
                textView.setTextSize(12);
                textView.setPadding(16, 12, 16, 12);
                return view;
            }
        };
        patientsListView.setAdapter(adapter);
        
        if (patients.isEmpty()) {
            Toast.makeText(this, "Aucun patient trouvé", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddPatientDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ajouter Patient");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        EditText nameEdit = new EditText(this);
        nameEdit.setHint("Nom complet");
        layout.addView(nameEdit);

        EditText emailEdit = new EditText(this);
        emailEdit.setHint("Email");
        emailEdit.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailEdit);

        EditText passwordEdit = new EditText(this);
        passwordEdit.setHint("Mot de passe");
        passwordEdit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordEdit);

        EditText dobEdit = new EditText(this);
        dobEdit.setHint("Date de naissance (YYYY-MM-DD)");
        layout.addView(dobEdit);

        Spinner bloodTypeSpinner = new Spinner(this);
        ArrayAdapter<String> bloodAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, 
            new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bloodTypeSpinner.setAdapter(bloodAdapter);
        layout.addView(bloodTypeSpinner);

        EditText emergencyEdit = new EditText(this);
        emergencyEdit.setHint("Contact d'urgence");
        layout.addView(emergencyEdit);

        builder.setView(layout);
        builder.setPositiveButton("Ajouter", (dialog, which) -> {
            String name = nameEdit.getText().toString().trim();
            String email = emailEdit.getText().toString().trim();
            String password = passwordEdit.getText().toString().trim();
            String dob = dobEdit.getText().toString().trim();
            String bloodType = bloodTypeSpinner.getSelectedItem().toString();
            String emergency = emergencyEdit.getText().toString().trim();

            if (!name.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
                if (isValidEmail(email)) {
                    addPatient(name, email, password, dob, bloodType, emergency);
                } else {
                    Toast.makeText(this, "Email invalide", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Veuillez remplir les champs obligatoires", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void addPatient(String name, String email, String password, String dob, String bloodType, String emergency) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Check if email already exists
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE email = ?", new String[]{email});
        if (cursor.moveToFirst()) {
            cursor.close();
            Toast.makeText(this, "Cet email existe déjà", Toast.LENGTH_SHORT).show();
            return;
        }
        cursor.close();
        
        db.beginTransaction();
        try {
            // Insert user
            ContentValues userValues = new ContentValues();
            userValues.put("full_name", name);
            userValues.put("email", email);
            userValues.put("password_hash", password);
            userValues.put("role", "patient");

            long userId = db.insert("users", null, userValues);
            if (userId != -1) {
                // Insert patient profile
                ContentValues patientValues = new ContentValues();
                patientValues.put("user_id", userId);
                if (!dob.isEmpty()) patientValues.put("date_of_birth", dob);
                patientValues.put("blood_type", bloodType);
                if (!emergency.isEmpty()) patientValues.put("emergency_contact", emergency);

                long patientResult = db.insert("patients", null, patientValues);
                if (patientResult != -1) {
                    db.setTransactionSuccessful();
                    Toast.makeText(this, "Patient ajouté avec succès", Toast.LENGTH_SHORT).show();
                    loadPatients();
                    loadStatistics();
                } else {
                    Toast.makeText(this, "Erreur lors de l'ajout du profil patient", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Erreur lors de l'ajout de l'utilisateur", Toast.LENGTH_SHORT).show();
            }
        } finally {
            db.endTransaction();
        }
    }

    private void showPatientOptionsDialog(Patient patient) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(patient.fullName);
        
        String[] options = {"Voir détails", "Modifier", "Supprimer"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showPatientDetails(patient);
                    break;
                case 1:
                    showEditPatientDialog(patient);
                    break;
                case 2:
                    confirmDeletePatient(patient);
                    break;
            }
        });
        builder.show();
    }

    private void showPatientDetails(Patient patient) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder details = new StringBuilder();
        
        details.append("Nom: ").append(patient.fullName).append("\n");
        details.append("Email: ").append(patient.email).append("\n");
        if (patient.dateOfBirth != null) {
            details.append("Date de naissance: ").append(patient.dateOfBirth).append("\n");
        }
        if (patient.bloodType != null) {
            details.append("Groupe sanguin: ").append(patient.bloodType).append("\n");
        }
        
        // Get additional patient info
        Cursor cursor = db.rawQuery(
            "SELECT emergency_contact, gender FROM patients WHERE user_id = ?", 
            new String[]{String.valueOf(patient.id)});
        if (cursor.moveToFirst()) {
            String emergency = cursor.getString(0);
            String gender = cursor.getString(1);
            if (emergency != null) details.append("Contact d'urgence: ").append(emergency).append("\n");
            if (gender != null) details.append("Genre: ").append(gender).append("\n");
        }
        cursor.close();
        
        // Get appointment count
        cursor = db.rawQuery(
            "SELECT COUNT(*) FROM appointments WHERE patient_id = ?", 
            new String[]{String.valueOf(patient.id)});
        if (cursor.moveToFirst()) {
            details.append("\nRendez-vous: ").append(cursor.getInt(0));
        }
        cursor.close();
        
        // Get medical records count
        cursor = db.rawQuery(
            "SELECT COUNT(*) FROM medical_records WHERE patient_id = ?", 
            new String[]{String.valueOf(patient.id)});
        if (cursor.moveToFirst()) {
            details.append("\nDossiers médicaux: ").append(cursor.getInt(0));
        }
        cursor.close();

        new AlertDialog.Builder(this)
            .setTitle("Détails Patient")
            .setMessage(details.toString())
            .setPositiveButton("Fermer", null)
            .show();
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
        emailEdit.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailEdit);

        builder.setView(layout);
        builder.setPositiveButton("Modifier", (dialog, which) -> {
            String name = nameEdit.getText().toString().trim();
            String email = emailEdit.getText().toString().trim();
            
            if (!name.isEmpty() && !email.isEmpty()) {
                if (isValidEmail(email)) {
                    updatePatient(patient.id, name, email);
                } else {
                    Toast.makeText(this, "Email invalide", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void updatePatient(int id, String name, String email) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Check if email already exists for another user
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE email = ? AND id != ?", 
            new String[]{email, String.valueOf(id)});
        if (cursor.moveToFirst()) {
            cursor.close();
            Toast.makeText(this, "Cet email est déjà utilisé", Toast.LENGTH_SHORT).show();
            return;
        }
        cursor.close();
        
        ContentValues values = new ContentValues();
        values.put("full_name", name);
        values.put("email", email);

        int result = db.update("users", values, "id = ?", new String[]{String.valueOf(id)});
        if (result > 0) {
            Toast.makeText(this, "Patient modifié avec succès", Toast.LENGTH_SHORT).show();
            loadPatients();
        } else {
            Toast.makeText(this, "Erreur lors de la modification", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeletePatient(Patient patient) {
        new AlertDialog.Builder(this)
            .setTitle("Confirmer la suppression")
            .setMessage("Supprimer " + patient.fullName + " ?\n\nCette action supprimera aussi tous ses rendez-vous et dossiers médicaux.")
            .setPositiveButton("Supprimer", (dialog, which) -> deletePatient(patient.id))
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void deletePatient(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete("users", "id = ?", new String[]{String.valueOf(id)});
        if (result > 0) {
            Toast.makeText(this, "Patient supprimé avec succès", Toast.LENGTH_SHORT).show();
            loadPatients();
            loadStatistics();
        } else {
            Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
        }
    }

    private static class Patient {
        int id;
        String fullName, email, bloodType, dateOfBirth;

        Patient(int id, String fullName, String email, String bloodType, String dateOfBirth) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.bloodType = bloodType;
            this.dateOfBirth = dateOfBirth;
        }
    }
}