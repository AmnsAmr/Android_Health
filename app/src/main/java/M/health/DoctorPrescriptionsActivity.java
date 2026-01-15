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

public class DoctorPrescriptionsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager; // Added AuthManager
    private ListView prescriptionsListView;
    private TextView refillRequestsText, totalPrescriptionsText, pendingRefillsText;
    private ArrayAdapter<String> adapter;
    private int doctorId;
    private List<Prescription> prescriptions;

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
        if (!authManager.hasPermission("doctor_prescribe_medication")) {
            Toast.makeText(this, "Accès refusé: Permissions insuffisantes", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_doctor_prescriptions);

        // 3. Get Doctor ID from Session
        doctorId = authManager.getUserId();

        prescriptionsListView = findViewById(R.id.prescriptionsListView);
        refillRequestsText = findViewById(R.id.refillRequestsText);
        totalPrescriptionsText = findViewById(R.id.totalPrescriptionsText);
        pendingRefillsText = findViewById(R.id.pendingRefillsText);
        prescriptions = new ArrayList<>();

        LinearLayout addPrescriptionBtn = findViewById(R.id.addPrescriptionBtn);
        LinearLayout searchPrescriptionBtn = findViewById(R.id.searchPrescriptionBtn);
        LinearLayout manageRefillsBtn = findViewById(R.id.manageRefillsBtn);

        addPrescriptionBtn.setOnClickListener(v -> showAddPrescriptionDialog());
        searchPrescriptionBtn.setOnClickListener(v -> showSearchDialog());
        manageRefillsBtn.setOnClickListener(v -> showManageRefillsDialog());

        prescriptionsListView.setOnItemClickListener((parent, view, position, id) ->
                showPrescriptionOptionsDialog(prescriptions.get(position)));

        loadPrescriptions();
        loadRefillRequests();
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

    // Added onResume to re-validate session
    @Override
    protected void onResume() {
        super.onResume();
        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            redirectToLogin();
            return;
        }
        // Refresh data
        loadPrescriptions();
        loadRefillRequests();
        loadStatistics();
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

            String prescriptionDisplay = String.format("%-18s | %-12s | Actions",
                    prescription.patientName.length() > 16 ? prescription.patientName.substring(0, 16) + ".." : prescription.patientName,
                    prescription.medication.length() > 10 ? prescription.medication.substring(0, 10) + ".." : prescription.medication);
            prescriptionStrings.add(prescriptionDisplay);
        }
        cursor.close();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, prescriptionStrings) {
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
                        "ORDER BY pr.requested_at DESC LIMIT 3",
                new String[]{String.valueOf(doctorId)});

        if (cursor.getCount() == 0) {
            requests.append("Aucune demande de renouvellement en attente");
        } else {
            while (cursor.moveToNext()) {
                requests.append("• ").append(cursor.getString(0)).append("\n");
                requests.append("  ").append(cursor.getString(1)).append("\n");
                requests.append("  Demandé le: ").append(cursor.getString(3)).append("\n\n");
            }
        }
        cursor.close();

        refillRequestsText.setText(requests.toString());
    }

    private void loadStatistics() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Total prescriptions
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM prescriptions WHERE doctor_id = ?",
                new String[]{String.valueOf(doctorId)});
        if (cursor.moveToFirst()) {
            totalPrescriptionsText.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();

        // Pending refills
        cursor = db.rawQuery(
                "SELECT COUNT(*) FROM prescription_refill_requests pr " +
                        "JOIN prescriptions p ON pr.prescription_id = p.id " +
                        "WHERE p.doctor_id = ? AND pr.status = 'pending'",
                new String[]{String.valueOf(doctorId)});
        if (cursor.moveToFirst()) {
            pendingRefillsText.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rechercher Prescription");

        EditText searchEdit = new EditText(this);
        searchEdit.setHint("Nom patient ou médicament");
        builder.setView(searchEdit);

        builder.setPositiveButton("Rechercher", (dialog, which) -> {
            String query = searchEdit.getText().toString();
            if (!query.isEmpty()) {
                searchPrescriptions(query);
            }
        });
        builder.setNegativeButton("Tout afficher", (dialog, which) -> loadPrescriptions());
        builder.show();
    }

    private void searchPrescriptions(String query) {
        prescriptions.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT p.id, u.full_name, p.medication, p.dosage, p.created_at " +
                        "FROM prescriptions p " +
                        "JOIN users u ON p.patient_id = u.id " +
                        "WHERE p.doctor_id = ? AND (u.full_name LIKE ? OR p.medication LIKE ?) " +
                        "ORDER BY p.created_at DESC",
                new String[]{String.valueOf(doctorId), "%" + query + "%", "%" + query + "%"});

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

            String prescriptionDisplay = String.format("%-18s | %-12s | Actions",
                    prescription.patientName.length() > 16 ? prescription.patientName.substring(0, 16) + ".." : prescription.patientName,
                    prescription.medication.length() > 10 ? prescription.medication.substring(0, 10) + ".." : prescription.medication);
            prescriptionStrings.add(prescriptionDisplay);
        }
        cursor.close();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, prescriptionStrings) {
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
        prescriptionsListView.setAdapter(adapter);

        if (prescriptions.isEmpty()) {
            Toast.makeText(this, "Aucune prescription trouvée", Toast.LENGTH_SHORT).show();
        }
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
        patientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        patientSpinner.setAdapter(patientAdapter);
        layout.addView(patientSpinner);

        EditText medicationEdit = new EditText(this);
        medicationEdit.setHint("Médicament");
        layout.addView(medicationEdit);

        EditText dosageEdit = new EditText(this);
        dosageEdit.setHint("Dosage (ex: 1 comprimé 2x/jour)");
        layout.addView(dosageEdit);

        EditText instructionsEdit = new EditText(this);
        instructionsEdit.setHint("Instructions");
        instructionsEdit.setLines(2);
        layout.addView(instructionsEdit);

        builder.setView(layout);
        builder.setPositiveButton("Prescrire", (dialog, which) -> {
            int selectedPatientId = patientIds.get(patientSpinner.getSelectedItemPosition());
            String medication = medicationEdit.getText().toString().trim();
            String dosage = dosageEdit.getText().toString().trim();
            String instructions = instructionsEdit.getText().toString().trim();

            if (!medication.isEmpty() && !dosage.isEmpty()) {
                addPrescription(selectedPatientId, medication, dosage, instructions);
            } else {
                Toast.makeText(this, "Veuillez remplir les champs obligatoires", Toast.LENGTH_SHORT).show();
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
        if (!instructions.isEmpty()) {
            values.put("instructions", instructions);
        }

        long result = db.insert("prescriptions", null, values);
        if (result != -1) {
            Toast.makeText(this, "Prescription ajoutée avec succès", Toast.LENGTH_SHORT).show();
            loadPrescriptions();
            loadStatistics();
        } else {
            Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPrescriptionOptionsDialog(Prescription prescription) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(prescription.patientName + " - " + prescription.medication);

        String[] options = {"Voir détails", "Modifier", "Supprimer"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showPrescriptionDetails(prescription);
                    break;
                case 1:
                    showEditPrescriptionDialog(prescription);
                    break;
                case 2:
                    confirmDeletePrescription(prescription);
                    break;
            }
        });
        builder.show();
    }

    private void showPrescriptionDetails(Prescription prescription) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder details = new StringBuilder();

        // Get full prescription details
        Cursor cursor = db.rawQuery(
                "SELECT p.instructions, u.full_name, u.email FROM prescriptions p " +
                        "JOIN users u ON p.patient_id = u.id WHERE p.id = ?",
                new String[]{String.valueOf(prescription.id)});

        if (cursor.moveToFirst()) {
            details.append("Patient: ").append(cursor.getString(1)).append("\n");
            details.append("Email: ").append(cursor.getString(2)).append("\n\n");
            details.append("Médicament: ").append(prescription.medication).append("\n");
            details.append("Dosage: ").append(prescription.dosage).append("\n");
            String instructions = cursor.getString(0);
            if (instructions != null && !instructions.isEmpty()) {
                details.append("Instructions: ").append(instructions).append("\n");
            }
            details.append("Prescrit le: ").append(prescription.createdAt).append("\n\n");
        }
        cursor.close();

        // Check for refill requests
        cursor = db.rawQuery(
                "SELECT status, requested_at FROM prescription_refill_requests " +
                        "WHERE prescription_id = ? ORDER BY requested_at DESC LIMIT 3",
                new String[]{String.valueOf(prescription.id)});

        if (cursor.getCount() > 0) {
            details.append("DEMANDES DE RENOUVELLEMENT:\n");
            while (cursor.moveToNext()) {
                details.append("• Statut: ").append(cursor.getString(0)).append("\n");
                details.append("  Date: ").append(cursor.getString(1)).append("\n\n");
            }
        }
        cursor.close();

        new AlertDialog.Builder(this)
                .setTitle("Détails Prescription")
                .setMessage(details.toString())
                .setPositiveButton("Fermer", null)
                .show();
    }

    private void showEditPrescriptionDialog(Prescription prescription) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifier Prescription");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        EditText medicationEdit = new EditText(this);
        medicationEdit.setText(prescription.medication);
        medicationEdit.setHint("Médicament");
        layout.addView(medicationEdit);

        EditText dosageEdit = new EditText(this);
        dosageEdit.setText(prescription.dosage);
        dosageEdit.setHint("Dosage");
        layout.addView(dosageEdit);

        // Get current instructions
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT instructions FROM prescriptions WHERE id = ?",
                new String[]{String.valueOf(prescription.id)});
        String currentInstructions = "";
        if (cursor.moveToFirst()) {
            currentInstructions = cursor.getString(0) != null ? cursor.getString(0) : "";
        }
        cursor.close();

        EditText instructionsEdit = new EditText(this);
        instructionsEdit.setText(currentInstructions);
        instructionsEdit.setHint("Instructions");
        instructionsEdit.setLines(2);
        layout.addView(instructionsEdit);

        builder.setView(layout);
        builder.setPositiveButton("Modifier", (dialog, which) -> {
            String medication = medicationEdit.getText().toString().trim();
            String dosage = dosageEdit.getText().toString().trim();
            String instructions = instructionsEdit.getText().toString().trim();

            if (!medication.isEmpty() && !dosage.isEmpty()) {
                updatePrescription(prescription.id, medication, dosage, instructions);
            } else {
                Toast.makeText(this, "Veuillez remplir les champs obligatoires", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void updatePrescription(int prescriptionId, String medication, String dosage, String instructions) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("medication", medication);
        values.put("dosage", dosage);
        values.put("instructions", instructions);

        int result = db.update("prescriptions", values, "id = ?", new String[]{String.valueOf(prescriptionId)});
        if (result > 0) {
            Toast.makeText(this, "Prescription modifiée avec succès", Toast.LENGTH_SHORT).show();
            loadPrescriptions();
        } else {
            Toast.makeText(this, "Erreur lors de la modification", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeletePrescription(Prescription prescription) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmer la suppression")
                .setMessage("Supprimer la prescription " + prescription.medication + " pour " + prescription.patientName + " ?")
                .setPositiveButton("Supprimer", (dialog, which) -> deletePrescription(prescription.id))
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void deletePrescription(int prescriptionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete("prescriptions", "id = ?", new String[]{String.valueOf(prescriptionId)});
        if (result > 0) {
            Toast.makeText(this, "Prescription supprimée avec succès", Toast.LENGTH_SHORT).show();
            loadPrescriptions();
            loadStatistics();
        } else {
            Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
        }
    }

    private void showManageRefillsDialog() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT pr.id, u.full_name, p.medication, pr.requested_at " +
                        "FROM prescription_refill_requests pr " +
                        "JOIN prescriptions p ON pr.prescription_id = p.id " +
                        "JOIN users u ON p.patient_id = u.id " +
                        "WHERE p.doctor_id = ? AND pr.status = 'pending' " +
                        "ORDER BY pr.requested_at DESC",
                new String[]{String.valueOf(doctorId)});

        if (cursor.getCount() == 0) {
            cursor.close();
            Toast.makeText(this, "Aucune demande de renouvellement en attente", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> refillOptions = new ArrayList<>();
        List<Integer> refillIds = new ArrayList<>();

        while (cursor.moveToNext()) {
            refillIds.add(cursor.getInt(0));
            refillOptions.add(cursor.getString(1) + " - " + cursor.getString(2) + " (" + cursor.getString(3) + ")");
        }
        cursor.close();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Gérer les Demandes de Renouvellement");
        builder.setItems(refillOptions.toArray(new String[0]), (dialog, which) -> {
            int refillId = refillIds.get(which);
            showRefillActionDialog(refillId);
        });
        builder.show();
    }

    private void showRefillActionDialog(int refillId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Action sur la demande");

        String[] actions = {"Approuver", "Rejeter"};
        builder.setItems(actions, (dialog, which) -> {
            String status = which == 0 ? "approved" : "rejected";
            updateRefillStatus(refillId, status);
        });
        builder.show();
    }

    private void updateRefillStatus(int refillId, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);

        int result = db.update("prescription_refill_requests", values, "id = ?",
                new String[]{String.valueOf(refillId)});

        if (result > 0) {
            String message = status.equals("approved") ? "Demande approuvée" : "Demande rejetée";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            loadRefillRequests();
            loadStatistics();
        } else {
            Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
        }
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