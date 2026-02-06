package M.health;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class page_dossier_medical extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private int patientId;

    // UI Components
    private ImageView btnBack, btnDownloadAll;
    private TextView tvPatientName;
    private CardView cardResultatsLab; // This is the "Historique" button

    // 1. Diagnosis Components (Linked to medical_records table)
    private CardView cardLatestDiagnosis;
    private TextView tvDiagnosisDoctor, tvDiagnosisContent, tvTreatmentContent, tvDiagnosisDate, tvNoDiagnosis;

    // 2. Prescription Components (Linked to prescriptions table)
    private CardView cardLatestPrescription;
    private TextView tvPrescriptionMed, tvPrescriptionDosage, tvPrescriptionInstructions, tvPrescriptionDate, tvNoPrescription;

    // 3. Lab Results Components (Linked to test_results table)
    private CardView cardResultat1, cardResultat2, cardResultat3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_dossier_medical);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        // Security Check: Verify Login
        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Security Check: Verify Permission
        if (!authManager.hasPermission("patient_view_own_records")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get the Logged-in Patient's ID
        AuthManager.User currentUser = authManager.getCurrentUser();
        patientId = currentUser.id;

        initializeViews();
        loadPatientName();

        // FETCHING DATA
        loadLatestDiagnosis();      // Fetches from medical_records
        loadLatestPrescription();   // Fetches from prescriptions
        loadRecentTestResults();    // Fetches from test_results

        setupClickListeners();
    }

    private void initializeViews() {
        // Header
        btnBack = findViewById(R.id.btnBack);
        btnDownloadAll = findViewById(R.id.btnDownloadAll);

        // Personal Info (Name Only)
        tvPatientName = findViewById(R.id.tvPatientName);

        // Historique Button
        cardResultatsLab = findViewById(R.id.cardResultatsLab);

        // Diagnosis Cards
        cardLatestDiagnosis = findViewById(R.id.cardLatestDiagnosis);
        tvDiagnosisDoctor = findViewById(R.id.tvDiagnosisDoctor);
        tvDiagnosisContent = findViewById(R.id.tvDiagnosisContent);
        tvTreatmentContent = findViewById(R.id.tvTreatmentContent);
        tvDiagnosisDate = findViewById(R.id.tvDiagnosisDate);
        tvNoDiagnosis = findViewById(R.id.tvNoDiagnosis);

        // Prescription Cards
        cardLatestPrescription = findViewById(R.id.cardLatestPrescription);
        tvPrescriptionMed = findViewById(R.id.tvPrescriptionMed);
        tvPrescriptionDosage = findViewById(R.id.tvPrescriptionDosage);
        tvPrescriptionInstructions = findViewById(R.id.tvPrescriptionInstructions);
        tvPrescriptionDate = findViewById(R.id.tvPrescriptionDate);
        tvNoPrescription = findViewById(R.id.tvNoPrescription);
        // Lab Result Cards
        cardResultat1 = findViewById(R.id.cardResultat1);
        cardResultat2 = findViewById(R.id.cardResultat2);
        cardResultat3 = findViewById(R.id.cardResultat3);
    }

    private void loadPatientName() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT full_name FROM users WHERE id = ?", new String[]{String.valueOf(patientId)});
            if (cursor.moveToFirst()) {
                tvPatientName.setText(cursor.getString(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // --- FIX: LOGIC TO FETCH MEDICAL RECORDS ---
    private void loadLatestDiagnosis() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            // FIX: Use LEFT JOIN so the record appears even if the doctor_id is -1 (broken).
            String query = "SELECT mr.diagnosis, mr.treatment, mr.created_at, u.full_name " +
                    "FROM medical_records mr " +
                    "LEFT JOIN users u ON mr.doctor_id = u.id " +
                    "WHERE mr.patient_id = ? " +
                    "ORDER BY mr.created_at DESC LIMIT 1";

            cursor = db.rawQuery(query, new String[]{String.valueOf(patientId)});

            if (cursor.moveToFirst()) {
                String diagnosis = cursor.getString(0);
                String treatment = cursor.getString(1);
                String date = cursor.getString(2);
                String doctorName = cursor.getString(3);

                // HANDLE THE NULL: If doctor is missing (ID -1), show "Non spécifié"
                if (doctorName == null) {
                    doctorName = "Non spécifié";
                }

                tvDiagnosisDoctor.setText("Dr. " + doctorName);
                tvDiagnosisContent.setText(diagnosis);
                tvTreatmentContent.setText(treatment);
                tvDiagnosisDate.setText(date);

                cardLatestDiagnosis.setVisibility(View.VISIBLE);
                tvNoDiagnosis.setVisibility(View.GONE);
            } else {
                cardLatestDiagnosis.setVisibility(View.GONE);
                tvNoDiagnosis.setVisibility(View.VISIBLE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur chargement diagnostic", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }
    }
    // --- LOGIC TO FETCH PRESCRIPTIONS ---
    private void loadLatestPrescription() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT medication, dosage, instructions, created_at FROM prescriptions WHERE patient_id = ? ORDER BY created_at DESC LIMIT 1",
                    new String[]{String.valueOf(patientId)}
            );
            if (cursor.moveToFirst()) {
                tvPrescriptionMed.setText(cursor.getString(0));
                tvPrescriptionDosage.setText("Dosage: " + cursor.getString(1));
                tvPrescriptionInstructions.setText("Instructions: " + cursor.getString(2));
                tvPrescriptionDate.setText(cursor.getString(3));

                cardLatestPrescription.setVisibility(View.VISIBLE);
                tvNoPrescription.setVisibility(View.GONE);
            } else {
                cardLatestPrescription.setVisibility(View.GONE);
                tvNoPrescription.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // --- LOGIC TO FETCH LAB RESULTS ---
    private void loadRecentTestResults() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT tr.id, tr.test_name, tr.result, tr.test_date FROM test_results tr WHERE tr.patient_id = ? ORDER BY tr.test_date DESC LIMIT 3",
                    new String[]{String.valueOf(patientId)}
            );

            TextView[] testNames = {findViewById(R.id.tvTestName1), findViewById(R.id.tvTestName2), findViewById(R.id.tvTestName3)};
            TextView[] testDates = {findViewById(R.id.tvTestDate1), findViewById(R.id.tvTestDate2), findViewById(R.id.tvTestDate3)};
            CardView[] cards = {cardResultat1, cardResultat2, cardResultat3};
            int cardIndex = 0;

            while (cursor.moveToNext() && cardIndex < 3) {
                final int testId = cursor.getInt(0);
                final String testName = cursor.getString(1);
                final String result = cursor.getString(2);
                final String testDate = cursor.getString(3);

                testNames[cardIndex].setText(testName);
                testDates[cardIndex].setText(testDate);
                cards[cardIndex].setVisibility(View.VISIBLE);
                cards[cardIndex].setOnClickListener(v -> showTestResultDetails(testId, testName, result, testDate));
                cardIndex++;
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showTestResultDetails(int testId, String testName, String result, String testDate) {
        new AlertDialog.Builder(this)
                .setTitle(testName)
                .setMessage("Résultat: " + result + "\nDate: " + testDate)
                .setPositiveButton("Fermer", null)
                .show();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnDownloadAll.setOnClickListener(v ->
                Toast.makeText(this, "Téléchargement en cours...", Toast.LENGTH_SHORT).show()
        );

        cardResultatsLab.setOnClickListener(v ->
                startActivity(new Intent(this, MedicalHistoryTimelineActivity.class))
        );
    }
}