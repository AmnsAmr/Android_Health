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

public class DoctorMedicalRecordsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager; // 1. Add AuthManager
    private ListView recordsListView;
    private int doctorId;
    private List<MedicalRecord> records;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this); // 2. Initialize

        // 3. SECURITY CHECK: Ensure user is logged in
        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_doctor_medical_records);

        // 4. CRITICAL FIX: Get Doctor ID from Session, NOT Intent
        // This guarantees we get the real ID (e.g., 5) instead of -1
        doctorId = authManager.getUserId();

        if (doctorId == -1) {
            Toast.makeText(this, "Erreur d'authentification", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recordsListView = findViewById(R.id.recordsListView);
        records = new ArrayList<>();

        LinearLayout addRecordBtn = findViewById(R.id.addRecordBtn);
        addRecordBtn.setOnClickListener(v -> showAddRecordDialog());

        recordsListView.setOnItemClickListener((parent, view, position, id) ->
                showRecordDetails(records.get(position)));

        loadRecords();
    }

    private void loadRecords() {
        records.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // This query will now work because doctorId is valid
        Cursor cursor = db.rawQuery(
                "SELECT mr.id, u.full_name, mr.diagnosis, mr.treatment, mr.created_at " +
                        "FROM medical_records mr " +
                        "JOIN users u ON mr.patient_id = u.id " +
                        "WHERE mr.doctor_id = ? ORDER BY mr.created_at DESC",
                new String[]{String.valueOf(doctorId)});

        List<String> recordStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            MedicalRecord record = new MedicalRecord(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4)
            );
            records.add(record);
            recordStrings.add(record.patientName + " - " + record.diagnosis);
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, recordStrings);
        recordsListView.setAdapter(adapter);
    }

    private void showAddRecordDialog() {
        // ... (Keep existing logic, it's correct)
        // Ensure you use the updated query I gave you before (SELECT from users WHERE role='patient')
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, full_name FROM users WHERE role = 'patient'",
                null);

        // ... rest of the dialog logic ...
        // (Copy the rest of the function from your previous working version)

        List<String> patientNames = new ArrayList<>();
        List<Integer> patientIds = new ArrayList<>();

        while (cursor.moveToNext()) {
            patientIds.add(cursor.getInt(0));
            patientNames.add(cursor.getString(1));
        }
        cursor.close();

        if (patientNames.isEmpty()) {
            Toast.makeText(this, "Aucun patient trouvé", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nouveau Dossier Médical");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        Spinner patientSpinner = new Spinner(this);
        ArrayAdapter<String> patientAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, patientNames);
        patientSpinner.setAdapter(patientAdapter);
        layout.addView(patientSpinner);

        EditText diagnosisEdit = new EditText(this);
        diagnosisEdit.setHint("Diagnostic");
        layout.addView(diagnosisEdit);

        EditText treatmentEdit = new EditText(this);
        treatmentEdit.setHint("Traitement");
        treatmentEdit.setLines(3);
        layout.addView(treatmentEdit);

        builder.setView(layout);
        builder.setPositiveButton("Ajouter", (dialog, which) -> {
            int selectedPatientId = patientIds.get(patientSpinner.getSelectedItemPosition());
            String diagnosis = diagnosisEdit.getText().toString();
            String treatment = treatmentEdit.getText().toString();

            if (!diagnosis.isEmpty() && !treatment.isEmpty()) {
                addMedicalRecord(selectedPatientId, diagnosis, treatment);
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void addMedicalRecord(int patientId, String diagnosis, String treatment) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("patient_id", patientId);
        values.put("doctor_id", doctorId); // This will now be CORRECT (e.g., 5)
        values.put("diagnosis", diagnosis);
        values.put("treatment", treatment);

        long result = db.insert("medical_records", null, values);
        if (result != -1) {
            Toast.makeText(this, "Dossier médical ajouté", Toast.LENGTH_SHORT).show();
            loadRecords();
        }
    }

    // ... showRecordDetails and MedicalRecord class ...
    private void showRecordDetails(MedicalRecord record) {
        String details = "Patient: " + record.patientName + "\n" +
                "Diagnostic: " + record.diagnosis + "\n" +
                "Traitement: " + record.treatment + "\n" +
                "Date: " + record.createdAt;

        new AlertDialog.Builder(this)
                .setTitle("Dossier Médical")
                .setMessage(details)
                .setPositiveButton("Fermer", null)
                .show();
    }

    private static class MedicalRecord {
        int id;
        String patientName, diagnosis, treatment, createdAt;

        MedicalRecord(int id, String patientName, String diagnosis, String treatment, String createdAt) {
            this.id = id;
            this.patientName = patientName;
            this.diagnosis = diagnosis;
            this.treatment = treatment;
            this.createdAt = createdAt;
        }
    }
}