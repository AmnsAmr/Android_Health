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

public class DoctorMedicalRecordsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private ListView recordsListView;
    private int doctorId;
    private List<MedicalRecord> records;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_medical_records);

        dbHelper = new DatabaseHelper(this);
        doctorId = getIntent().getIntExtra("doctor_id", -1);
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
        // Get patients list
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT DISTINCT u.id, u.full_name FROM users u " +
            "JOIN appointments a ON u.id = a.patient_id " +
            "WHERE a.doctor_id = ? AND u.role = 'patient'", 
            new String[]{String.valueOf(doctorId)});

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
        values.put("doctor_id", doctorId);
        values.put("diagnosis", diagnosis);
        values.put("treatment", treatment);

        long result = db.insert("medical_records", null, values);
        if (result != -1) {
            Toast.makeText(this, "Dossier médical ajouté", Toast.LENGTH_SHORT).show();
            loadRecords();
        }
    }

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