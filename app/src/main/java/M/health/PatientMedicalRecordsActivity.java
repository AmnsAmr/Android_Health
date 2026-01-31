package M.health;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class PatientMedicalRecordsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private ListView lvMedicalRecords;
    private List<MedicalRecord> medicalRecords;
    private MedicalRecordAdapter adapter;
    private int patientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_patient_medical_records);

            dbHelper = new DatabaseHelper(this);
            authManager = AuthManager.getInstance(this);
            
            if (!authManager.isLoggedIn() || !authManager.hasPermission("patient_view_own_records")) {
                Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            patientId = authManager.getCurrentUser().id;
            
            View userProfileHeader = findViewById(R.id.userProfileHeader);
            if (userProfileHeader != null) {
                UIHelper.setupUserProfileHeader(this, userProfileHeader, authManager);
            }
            
            lvMedicalRecords = findViewById(R.id.lvMedicalRecords);
            
            medicalRecords = new ArrayList<>();
            adapter = new MedicalRecordAdapter();
            lvMedicalRecords.setAdapter(adapter);
            
            loadMedicalRecords();
            
            lvMedicalRecords.setOnItemClickListener((parent, view, position, id) -> 
                showMedicalRecordDetails(medicalRecords.get(position)));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadMedicalRecords() {
        medicalRecords.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT mr.id, mr.diagnosis, mr.treatment, mr.created_at, " +
            "u.full_name as doctor_name, d.specialization " +
            "FROM medical_records mr " +
            "JOIN users u ON mr.doctor_id = u.id " +
            "LEFT JOIN doctors d ON mr.doctor_id = d.user_id " +
            "WHERE mr.patient_id = ? " +
            "ORDER BY mr.created_at DESC", 
            new String[]{String.valueOf(patientId)});

        while (cursor.moveToNext()) {
            medicalRecords.add(new MedicalRecord(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5)
            ));
        }
        cursor.close();
        
        if (medicalRecords.isEmpty()) {
            Toast.makeText(this, "Aucun dossier médical trouvé", Toast.LENGTH_SHORT).show();
        }
        
        adapter.notifyDataSetChanged();
    }

    private void showMedicalRecordDetails(MedicalRecord record) {
        StringBuilder details = new StringBuilder();
        details.append("DIAGNOSTIC:\\n").append(record.diagnosis).append("\\n\\n");
        details.append("TRAITEMENT:\\n").append(record.treatment).append("\\n\\n");
        details.append("MÉDECIN:\\nDr. ").append(record.doctorName);
        if (record.specialization != null) {
            details.append(" (").append(record.specialization).append(")");
        }
        details.append("\\n\\n");
        details.append("DATE:\\n").append(record.createdAt);

        new AlertDialog.Builder(this)
            .setTitle("Détails du Dossier Médical")
            .setMessage(details.toString())
            .setPositiveButton("Fermer", null)
            .setNeutralButton("Voir Tests", (dialog, which) -> showTestResults(record))
            .show();
    }

    private void showTestResults(MedicalRecord record) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT tr.test_name, tr.result, tr.test_date, " +
            "GROUP_CONCAT(trc.comment, '\\n') as comments " +
            "FROM test_results tr " +
            "LEFT JOIN test_result_comments trc ON tr.id = trc.test_result_id " +
            "WHERE tr.patient_id = ? AND tr.doctor_id = ? " +
            "AND date(tr.test_date) >= date(?) " +
            "GROUP BY tr.id " +
            "ORDER BY tr.test_date DESC",
            new String[]{String.valueOf(patientId), String.valueOf(record.id), record.createdAt});

        StringBuilder testResults = new StringBuilder();
        testResults.append("RÉSULTATS DE TESTS ASSOCIÉS:\\n\\n");
        
        boolean hasResults = false;
        while (cursor.moveToNext()) {
            hasResults = true;
            testResults.append("Test: ").append(cursor.getString(0)).append("\\n");
            testResults.append("Résultat: ").append(cursor.getString(1)).append("\\n");
            testResults.append("Date: ").append(cursor.getString(2) != null ? cursor.getString(2) : "N/A").append("\\n");
            
            String comments = cursor.getString(3);
            if (comments != null && !comments.isEmpty()) {
                testResults.append("Commentaires: ").append(comments).append("\\n");
            }
            testResults.append("\\n");
        }
        cursor.close();
        
        if (!hasResults) {
            testResults.append("Aucun résultat de test associé à ce dossier.");
        }

        new AlertDialog.Builder(this)
            .setTitle("Résultats de Tests")
            .setMessage(testResults.toString())
            .setPositiveButton("Fermer", null)
            .show();
    }

    private class MedicalRecordAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return medicalRecords.size();
        }

        @Override
        public Object getItem(int position) {
            return medicalRecords.get(position);
        }

        @Override
        public long getItemId(int position) {
            return medicalRecords.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(PatientMedicalRecordsActivity.this)
                        .inflate(R.layout.item_medical_record_card, parent, false);
            }

            MedicalRecord record = medicalRecords.get(position);
            
            TextView tvDiagnosis = convertView.findViewById(R.id.tvDiagnosis);
            TextView tvDoctor = convertView.findViewById(R.id.tvDoctor);
            TextView tvDate = convertView.findViewById(R.id.tvDate);
            TextView tvTreatment = convertView.findViewById(R.id.tvTreatment);

            tvDiagnosis.setText(record.diagnosis);
            tvDoctor.setText("Dr. " + record.doctorName + 
                (record.specialization != null ? " (" + record.specialization + ")" : ""));
            tvDate.setText(record.createdAt);
            
            // Show truncated treatment
            String treatment = record.treatment;
            if (treatment.length() > 100) {
                treatment = treatment.substring(0, 100) + "...";
            }
            tvTreatment.setText(treatment);

            return convertView;
        }
    }

    private static class MedicalRecord {
        int id;
        String diagnosis, treatment, createdAt, doctorName, specialization;
        
        MedicalRecord(int id, String diagnosis, String treatment, String createdAt, 
                     String doctorName, String specialization) {
            this.id = id;
            this.diagnosis = diagnosis;
            this.treatment = treatment;
            this.createdAt = createdAt;
            this.doctorName = doctorName;
            this.specialization = specialization;
        }
    }
}