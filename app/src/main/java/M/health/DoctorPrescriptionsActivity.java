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

public class DoctorPrescriptionsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private ListView prescriptionsListView;
    private TextView refillRequestsText;
    private int doctorId;
    private List<Prescription> prescriptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_prescriptions);

        dbHelper = new DatabaseHelper(this);
        doctorId = getIntent().getIntExtra("doctor_id", -1);
        prescriptionsListView = findViewById(R.id.prescriptionsListView);
        refillRequestsText = findViewById(R.id.refillRequestsText);
        prescriptions = new ArrayList<>();

        LinearLayout addPrescriptionBtn = findViewById(R.id.addPrescriptionBtn);
        addPrescriptionBtn.setOnClickListener(v -> showAddPrescriptionDialog());

        prescriptionsListView.setOnItemClickListener((parent, view, position, id) -> 
            showPrescriptionDetails(prescriptions.get(position)));

        loadPrescriptions();
        loadRefillRequests();
    }

    private void loadPrescriptions() {
        prescriptions.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT p.id, u.full_name, p.medication, p.dosage, p.created_at " +
            "FROM prescriptions p " +
            "JOIN users u ON p.patient_id = u.id " +
            "WHERE p.doctor_id = ? ORDER BY p.created_at DESC", 
            new String[]{String.valueOf(doctorId)});

        List<String> prescriptionStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            Prescription prescription = new Prescription(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4)
            );
            prescriptions.add(prescription);
            prescriptionStrings.add(prescription.patientName + " - " + prescription.medication);
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_list_item_1, prescriptionStrings);
        prescriptionsListView.setAdapter(adapter);
    }

    private void loadRefillRequests() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder requests = new StringBuilder();
        
        Cursor cursor = db.rawQuery(
            "SELECT u.full_name, p.medication, pr.status, pr.requested_at " +
            "FROM prescription_refill_requests pr " +
            "JOIN prescriptions p ON pr.prescription_id = p.id " +
            "JOIN users u ON p.patient_id = u.id " +
            "WHERE p.doctor_id = ? AND pr.status = 'pending' " +
            "ORDER BY pr.requested_at DESC", 
            new String[]{String.valueOf(doctorId)});

        if (cursor.getCount() == 0) {
            requests.append("Aucune demande de renouvellement en attente");
        } else {
            requests.append("DEMANDES DE RENOUVELLEMENT:\n\n");
            while (cursor.moveToNext()) {
                requests.append("• ").append(cursor.getString(0)).append("\n");
                requests.append("  Médicament: ").append(cursor.getString(1)).append("\n");
                requests.append("  Demandé le: ").append(cursor.getString(3)).append("\n\n");
            }
        }
        cursor.close();

        refillRequestsText.setText(requests.toString());
    }

    private void showAddPrescriptionDialog() {
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
        builder.setTitle("Nouvelle Prescription");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        Spinner patientSpinner = new Spinner(this);
        ArrayAdapter<String> patientAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, patientNames);
        patientSpinner.setAdapter(patientAdapter);
        layout.addView(patientSpinner);

        EditText medicationEdit = new EditText(this);
        medicationEdit.setHint("Médicament");
        layout.addView(medicationEdit);

        EditText dosageEdit = new EditText(this);
        dosageEdit.setHint("Dosage");
        layout.addView(dosageEdit);

        EditText instructionsEdit = new EditText(this);
        instructionsEdit.setHint("Instructions");
        instructionsEdit.setLines(2);
        layout.addView(instructionsEdit);

        builder.setView(layout);
        builder.setPositiveButton("Prescrire", (dialog, which) -> {
            int selectedPatientId = patientIds.get(patientSpinner.getSelectedItemPosition());
            String medication = medicationEdit.getText().toString();
            String dosage = dosageEdit.getText().toString();
            String instructions = instructionsEdit.getText().toString();

            if (!medication.isEmpty() && !dosage.isEmpty()) {
                addPrescription(selectedPatientId, medication, dosage, instructions);
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void addPrescription(int patientId, String medication, String dosage, String instructions) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("patient_id", patientId);
        values.put("doctor_id", doctorId);
        values.put("medication", medication);
        values.put("dosage", dosage);
        values.put("instructions", instructions);

        long result = db.insert("prescriptions", null, values);
        if (result != -1) {
            Toast.makeText(this, "Prescription ajoutée", Toast.LENGTH_SHORT).show();
            loadPrescriptions();
        }
    }

    private void showPrescriptionDetails(Prescription prescription) {
        String details = "Patient: " + prescription.patientName + "\n" +
                        "Médicament: " + prescription.medication + "\n" +
                        "Dosage: " + prescription.dosage + "\n" +
                        "Date: " + prescription.createdAt;

        new AlertDialog.Builder(this)
            .setTitle("Prescription")
            .setMessage(details)
            .setPositiveButton("Fermer", null)
            .show();
    }

    private static class Prescription {
        int id;
        String patientName, medication, dosage, createdAt;

        Prescription(int id, String patientName, String medication, String dosage, String createdAt) {
            this.id = id;
            this.patientName = patientName;
            this.medication = medication;
            this.dosage = dosage;
            this.createdAt = createdAt;
        }
    }
}