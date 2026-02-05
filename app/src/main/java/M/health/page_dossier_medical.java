package M.health;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class page_dossier_medical extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private int patientId;

    // UI Components
    private ImageView btnBack, btnDownloadAll;
    private TextView tvPatientName;
    private CardView cardResultatsLab;
    private CardView cardResultat1, cardResultat2, cardResultat3;

    // Prescription Components
    private CardView cardLatestPrescription;
    private TextView tvPrescriptionMed, tvPrescriptionDosage, tvPrescriptionInstructions, tvPrescriptionDate, tvNoPrescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_dossier_medical);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (!authManager.hasPermission("patient_view_own_records")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        AuthManager.User currentUser = authManager.getCurrentUser();
        patientId = currentUser.id;

        initializeViews();
        loadPatientData();
        loadLatestPrescription();
        loadRecentTestResults();
        setupClickListeners();
    }

    private void initializeViews() {
        // Header
        btnBack = findViewById(R.id.btnBack);
        btnDownloadAll = findViewById(R.id.btnDownloadAll);

        // Informations personnelles
        tvPatientName = findViewById(R.id.tvPatientName);

        // Carte Historique
        cardResultatsLab = findViewById(R.id.cardResultatsLab);

        // Latest Prescription
        cardLatestPrescription = findViewById(R.id.cardLatestPrescription);
        tvPrescriptionMed = findViewById(R.id.tvPrescriptionMed);
        tvPrescriptionDosage = findViewById(R.id.tvPrescriptionDosage);
        tvPrescriptionInstructions = findViewById(R.id.tvPrescriptionInstructions);
        tvPrescriptionDate = findViewById(R.id.tvPrescriptionDate);
        tvNoPrescription = findViewById(R.id.tvNoPrescription);

        // Cartes résultats de laboratoire
        cardResultat1 = findViewById(R.id.cardResultat1);
        cardResultat2 = findViewById(R.id.cardResultat2);
        cardResultat3 = findViewById(R.id.cardResultat3);
    }

    private void loadPatientData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            // Only fetch the name now
            cursor = db.rawQuery(
                    "SELECT u.full_name FROM users u WHERE u.id = ?",
                    new String[]{String.valueOf(patientId)}
            );

            if (cursor.moveToFirst()) {
                String name = cursor.getString(0);
                tvPatientName.setText(name);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur chargement données", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void loadLatestPrescription() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            // Fetch the most recent prescription for this patient
            cursor = db.rawQuery(
                    "SELECT medication, dosage, instructions, created_at " +
                            "FROM prescriptions " +
                            "WHERE patient_id = ? " +
                            "ORDER BY created_at DESC LIMIT 1",
                    new String[]{String.valueOf(patientId)}
            );

            if (cursor.moveToFirst()) {
                String med = cursor.getString(0);
                String dosage = cursor.getString(1);
                String instructions = cursor.getString(2);
                String date = cursor.getString(3);

                tvPrescriptionMed.setText(med);
                tvPrescriptionDosage.setText("Dosage: " + dosage);
                tvPrescriptionInstructions.setText("Instructions: " + instructions);
                tvPrescriptionDate.setText(date);

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

    private void loadRecentTestResults() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            Cursor cursor = db.rawQuery(
                    "SELECT tr.id, tr.test_name, tr.result, tr.test_date " +
                            "FROM test_results tr " +
                            "WHERE tr.patient_id = ? " +
                            "ORDER BY tr.test_date DESC LIMIT 3",
                    new String[]{String.valueOf(patientId)}
            );

            TextView[] testNames = {
                    findViewById(R.id.tvTestName1),
                    findViewById(R.id.tvTestName2),
                    findViewById(R.id.tvTestName3)
            };
            TextView[] testDates = {
                    findViewById(R.id.tvTestDate1),
                    findViewById(R.id.tvTestDate2),
                    findViewById(R.id.tvTestDate3)
            };
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
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder details = new StringBuilder();

        details.append("TEST: ").append(testName).append("\n\n");
        details.append("RÉSULTAT:\n").append(result).append("\n\n");
        details.append("DATE: ").append(testDate != null ? testDate : "Non spécifiée").append("\n\n");

        // Load comments
        Cursor cursor = db.rawQuery(
                "SELECT trc.comment, u.full_name " +
                        "FROM test_result_comments trc " +
                        "JOIN users u ON trc.doctor_id = u.id " +
                        "WHERE trc.test_result_id = ? " +
                        "ORDER BY trc.created_at DESC",
                new String[]{String.valueOf(testId)}
        );

        if (cursor.moveToFirst()) {
            details.append("COMMENTAIRES MÉDICAUX:\n");
            do {
                details.append("• Dr. ").append(cursor.getString(1)).append(": ");
                details.append(cursor.getString(0)).append("\n");
            } while (cursor.moveToNext());
        }
        cursor.close();

        new android.app.AlertDialog.Builder(this)
                .setTitle("Détails du Test")
                .setMessage(details.toString())
                .setPositiveButton("Fermer", null)
                .show();
    }

    private void setupClickListeners() {
        // Bouton retour
        btnBack.setOnClickListener(v -> finish());

        // Bouton télécharger tout
        btnDownloadAll.setOnClickListener(v ->
                Toast.makeText(page_dossier_medical.this, "Téléchargement en cours...", Toast.LENGTH_SHORT).show()
        );

        // Carte Historique
        cardResultatsLab.setOnClickListener(v -> {
            Intent intent = new Intent(page_dossier_medical.this, MedicalHistoryTimelineActivity.class);
            startActivity(intent);
        });
    }
}