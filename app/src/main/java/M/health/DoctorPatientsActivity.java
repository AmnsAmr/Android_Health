package M.health;

import android.content.ContentValues;
import android.content.Intent; // Added import
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class DoctorPatientsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager; // Added AuthManager
    private ListView patientsListView;
    private ArrayAdapter<String> adapter;
    private int doctorId;
    private List<Patient> patients;
    private TextView totalPatientsText, recordsCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this); // Initialize AuthManager

        // 1. Validate Session
        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            redirectToLogin();
            return;
        }

        // 2. Validate Permission
        if (!authManager.hasPermission("doctor_view_patients")) {
            Toast.makeText(this, "Accès refusé: Permissions insuffisantes", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_doctor_patients);

        // 3. Get Doctor ID from Session (Secure) instead of Intent
        doctorId = authManager.getUserId();

        patientsListView = findViewById(R.id.patientsListView);
        totalPatientsText = findViewById(R.id.totalPatientsText);
        recordsCountText = findViewById(R.id.recordsCountText);
        patients = new ArrayList<>();

        LinearLayout addTestResultBtn = findViewById(R.id.addTestResultBtn);
        LinearLayout searchPatientBtn = findViewById(R.id.searchPatientBtn);

        addTestResultBtn.setOnClickListener(v -> showAddTestResultDialog());
        searchPatientBtn.setOnClickListener(v -> showSearchDialog());

        patientsListView.setOnItemClickListener((parent, view, position, id) ->
                showPatientOptionsDialog(patients.get(position)));

        loadPatients();
        loadStatistics();
    }

    // Added Session Management Helper
    private void redirectToLogin() {
        Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Added onResume to re-validate session if app is resumed
    @Override
    protected void onResume() {
        super.onResume();
        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            redirectToLogin();
            return;
        }
        // Optional: reload data to keep it fresh
        loadPatients();
        loadStatistics();
    }

    private void loadPatients() {
        patients.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT u.id, u.full_name, p.date_of_birth, p.blood_type " +
                        "FROM users u " +
                        "JOIN patients p ON u.id = p.user_id " +
                        "JOIN appointments a ON u.id = a.patient_id " +
                        "WHERE a.doctor_id = ? AND u.role = 'patient' ORDER BY u.full_name",
                new String[]{String.valueOf(doctorId)});

        List<String> patientStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            Patient patient = new Patient(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3)
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

        // Total patients for this doctor
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(DISTINCT patient_id) FROM appointments WHERE doctor_id = ?",
                new String[]{String.valueOf(doctorId)});
        if (cursor.moveToFirst()) {
            totalPatientsText.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();

        // Total medical records by this doctor
        cursor = db.rawQuery(
                "SELECT COUNT(*) FROM medical_records WHERE doctor_id = ?",
                new String[]{String.valueOf(doctorId)});
        if (cursor.moveToFirst()) {
            recordsCountText.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rechercher Patient");

        EditText searchEdit = new EditText(this);
        searchEdit.setHint("Nom du patient");
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
                "SELECT DISTINCT u.id, u.full_name, p.date_of_birth, p.blood_type " +
                        "FROM users u " +
                        "JOIN patients p ON u.id = p.user_id " +
                        "JOIN appointments a ON u.id = a.patient_id " +
                        "WHERE a.doctor_id = ? AND u.role = 'patient' AND u.full_name LIKE ? " +
                        "ORDER BY u.full_name",
                new String[]{String.valueOf(doctorId), "%" + query + "%"});

        List<String> patientStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            Patient patient = new Patient(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3)
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

    private void showAddTestResultDialog() {
        if (patients.isEmpty()) {
            Toast.makeText(this, "Aucun patient disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ajouter Résultat de Test");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        Spinner patientSpinner = new Spinner(this);
        List<String> patientNames = new ArrayList<>();
        List<Integer> patientIds = new ArrayList<>();
        for (Patient patient : patients) {
            patientNames.add(patient.fullName);
            patientIds.add(patient.id);
        }
        ArrayAdapter<String> patientAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, patientNames);
        patientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        patientSpinner.setAdapter(patientAdapter);
        layout.addView(patientSpinner);

        EditText testNameEdit = new EditText(this);
        testNameEdit.setHint("Nom du test (ex: Prise de sang)");
        layout.addView(testNameEdit);

        EditText resultEdit = new EditText(this);
        resultEdit.setHint("Résultat du test");
        resultEdit.setLines(3);
        layout.addView(resultEdit);

        EditText dateEdit = new EditText(this);
        dateEdit.setHint("Date du test (YYYY-MM-DD)");
        layout.addView(dateEdit);

        builder.setView(layout);
        builder.setPositiveButton("Ajouter", (dialog, which) -> {
            int selectedPatientId = patientIds.get(patientSpinner.getSelectedItemPosition());
            String testName = testNameEdit.getText().toString().trim();
            String result = resultEdit.getText().toString().trim();
            String date = dateEdit.getText().toString().trim();

            if (!testName.isEmpty() && !result.isEmpty()) {
                addTestResult(selectedPatientId, testName, result, date);
            } else {
                Toast.makeText(this, "Veuillez remplir les champs obligatoires", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void addTestResult(int patientId, String testName, String result, String date) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("patient_id", patientId);
        values.put("doctor_id", doctorId);
        values.put("test_name", testName);
        values.put("result", result);
        if (!date.isEmpty()) {
            values.put("test_date", date);
        }

        long resultId = db.insert("test_results", null, values);
        if (resultId != -1) {
            Toast.makeText(this, "Résultat de test ajouté avec succès", Toast.LENGTH_SHORT).show();
            loadStatistics();
        } else {
            Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPatientOptionsDialog(Patient patient) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(patient.fullName);

        String[] options = {"Voir dossier complet", "Ajouter dossier médical", "Voir résultats tests", "Modifier résultat test"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showPatientDetails(patient);
                    break;
                case 1:
                    showAddMedicalRecordDialog(patient);
                    break;
                case 2:
                    showTestResults(patient);
                    break;
                case 3:
                    showEditTestResultDialog(patient);
                    break;
            }
        });
        builder.show();
    }

    private void showPatientDetails(Patient patient) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder details = new StringBuilder();

        details.append("Patient: ").append(patient.fullName).append("\n");
        details.append("Date de naissance: ").append(patient.dateOfBirth != null ? patient.dateOfBirth : "N/A").append("\n");
        details.append("Groupe sanguin: ").append(patient.bloodType != null ? patient.bloodType : "N/A").append("\n\n");

        // Medical records
        details.append("DOSSIERS MÉDICAUX:\n");
        Cursor cursor = db.rawQuery(
                "SELECT diagnosis, treatment, created_at FROM medical_records " +
                        "WHERE patient_id = ? AND doctor_id = ? ORDER BY created_at DESC LIMIT 5",
                new String[]{String.valueOf(patient.id), String.valueOf(doctorId)});

        if (cursor.getCount() == 0) {
            details.append("Aucun dossier médical\n\n");
        } else {
            while (cursor.moveToNext()) {
                details.append("• ").append(cursor.getString(0)).append("\n");
                details.append("  Traitement: ").append(cursor.getString(1)).append("\n");
                details.append("  Date: ").append(cursor.getString(2)).append("\n\n");
            }
        }
        cursor.close();

        // Test results
        details.append("RÉSULTATS DE TESTS RÉCENTS:\n");
        cursor = db.rawQuery(
                "SELECT test_name, result, test_date FROM test_results " +
                        "WHERE patient_id = ? AND doctor_id = ? ORDER BY test_date DESC LIMIT 3",
                new String[]{String.valueOf(patient.id), String.valueOf(doctorId)});

        if (cursor.getCount() == 0) {
            details.append("Aucun résultat de test");
        } else {
            while (cursor.moveToNext()) {
                details.append("• ").append(cursor.getString(0)).append(": ");
                details.append(cursor.getString(1)).append("\n");
                details.append("  Date: ").append(cursor.getString(2) != null ? cursor.getString(2) : "N/A").append("\n\n");
            }
        }
        cursor.close();

        new AlertDialog.Builder(this)
                .setTitle("Dossier Patient Complet")
                .setMessage(details.toString())
                .setPositiveButton("Fermer", null)
                .show();
    }

    private void showAddMedicalRecordDialog(Patient patient) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nouveau Dossier Médical - " + patient.fullName);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        EditText diagnosisEdit = new EditText(this);
        diagnosisEdit.setHint("Diagnostic");
        layout.addView(diagnosisEdit);

        EditText treatmentEdit = new EditText(this);
        treatmentEdit.setHint("Traitement prescrit");
        treatmentEdit.setLines(3);
        layout.addView(treatmentEdit);

        builder.setView(layout);
        builder.setPositiveButton("Ajouter", (dialog, which) -> {
            String diagnosis = diagnosisEdit.getText().toString().trim();
            String treatment = treatmentEdit.getText().toString().trim();

            if (!diagnosis.isEmpty() && !treatment.isEmpty()) {
                addMedicalRecord(patient.id, diagnosis, treatment);
            } else {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void addMedicalRecord(int patientId, String diagnosis, String treatment) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("patient_id", patientId);
        values.put("doctor_id", doctorId);
        values.put("diagnosis", diagnosis);
        values.put("treatment", treatment);

        long result = db.insert("medical_records", null, values);
        if (result != -1) {
            Toast.makeText(this, "Dossier médical ajouté avec succès", Toast.LENGTH_SHORT).show();
            loadStatistics();
        } else {
            Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTestResults(Patient patient) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder results = new StringBuilder();

        results.append("RÉSULTATS DE TESTS - ").append(patient.fullName).append("\n\n");

        Cursor cursor = db.rawQuery(
                "SELECT id, test_name, result, test_date FROM test_results " +
                        "WHERE patient_id = ? AND doctor_id = ? ORDER BY test_date DESC",
                new String[]{String.valueOf(patient.id), String.valueOf(doctorId)});

        if (cursor.getCount() == 0) {
            results.append("Aucun résultat de test disponible");
        } else {
            while (cursor.moveToNext()) {
                results.append("Test: ").append(cursor.getString(1)).append("\n");
                results.append("Résultat: ").append(cursor.getString(2)).append("\n");
                results.append("Date: ").append(cursor.getString(3) != null ? cursor.getString(3) : "N/A").append("\n");
                results.append("ID: ").append(cursor.getInt(0)).append("\n\n");
            }
        }
        cursor.close();

        new AlertDialog.Builder(this)
                .setTitle("Résultats de Tests")
                .setMessage(results.toString())
                .setPositiveButton("Fermer", null)
                .show();
    }

    private void showEditTestResultDialog(Patient patient) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Get test results for this patient
        Cursor cursor = db.rawQuery(
                "SELECT id, test_name, result FROM test_results " +
                        "WHERE patient_id = ? AND doctor_id = ? ORDER BY test_date DESC",
                new String[]{String.valueOf(patient.id), String.valueOf(doctorId)});

        if (cursor.getCount() == 0) {
            cursor.close();
            Toast.makeText(this, "Aucun résultat de test à modifier", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> testOptions = new ArrayList<>();
        List<Integer> testIds = new ArrayList<>();

        while (cursor.moveToNext()) {
            testIds.add(cursor.getInt(0));
            testOptions.add(cursor.getString(1) + " - " + cursor.getString(2));
        }
        cursor.close();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifier Résultat - " + patient.fullName);
        builder.setItems(testOptions.toArray(new String[0]), (dialog, which) -> {
            int testId = testIds.get(which);
            showEditTestDialog(testId);
        });
        builder.show();
    }

    private void showEditTestDialog(int testId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT test_name, result, test_date FROM test_results WHERE id = ?",
                new String[]{String.valueOf(testId)});

        if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }

        String currentTestName = cursor.getString(0);
        String currentResult = cursor.getString(1);
        String currentDate = cursor.getString(2);
        cursor.close();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifier Résultat de Test");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        EditText testNameEdit = new EditText(this);
        testNameEdit.setText(currentTestName);
        testNameEdit.setHint("Nom du test");
        layout.addView(testNameEdit);

        EditText resultEdit = new EditText(this);
        resultEdit.setText(currentResult);
        resultEdit.setHint("Résultat du test");
        resultEdit.setLines(3);
        layout.addView(resultEdit);

        EditText dateEdit = new EditText(this);
        dateEdit.setText(currentDate != null ? currentDate : "");
        dateEdit.setHint("Date du test (YYYY-MM-DD)");
        layout.addView(dateEdit);

        builder.setView(layout);
        builder.setPositiveButton("Modifier", (dialog, which) -> {
            String testName = testNameEdit.getText().toString().trim();
            String result = resultEdit.getText().toString().trim();
            String date = dateEdit.getText().toString().trim();

            if (!testName.isEmpty() && !result.isEmpty()) {
                updateTestResult(testId, testName, result, date);
            } else {
                Toast.makeText(this, "Veuillez remplir les champs obligatoires", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.setNeutralButton("Supprimer", (dialog, which) -> deleteTestResult(testId));
        builder.show();
    }

    private void updateTestResult(int testId, String testName, String result, String date) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("test_name", testName);
        values.put("result", result);
        if (!date.isEmpty()) {
            values.put("test_date", date);
        }

        int updateResult = db.update("test_results", values, "id = ?", new String[]{String.valueOf(testId)});
        if (updateResult > 0) {
            Toast.makeText(this, "Résultat modifié avec succès", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Erreur lors de la modification", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteTestResult(int testId) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmer la suppression")
                .setMessage("Supprimer ce résultat de test ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    int result = db.delete("test_results", "id = ?", new String[]{String.valueOf(testId)});
                    if (result > 0) {
                        Toast.makeText(this, "Résultat supprimé avec succès", Toast.LENGTH_SHORT).show();
                        loadStatistics();
                    } else {
                        Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private static class Patient {
        int id;
        String fullName, dateOfBirth, bloodType;

        Patient(int id, String fullName, String dateOfBirth, String bloodType) {
            this.id = id;
            this.fullName = fullName;
            this.dateOfBirth = dateOfBirth;
            this.bloodType = bloodType;
        }
    }
}