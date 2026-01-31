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
    private TextView tvPatientName, tvPatientAge, tvBloodType, tvAllergies;
    private CardView cardResultatsLab, cardHistorique, cardMedicaments, cardImagerie;
    private CardView cardResultat1, cardResultat2, cardResultat3;

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

        // Custom header with back button
        // Header elements are defined in the layout file

        initializeViews();
        loadPatientData();
        setupClickListeners();
    }

    private void initializeViews() {
        // Header
        btnBack = findViewById(R.id.btnBack);
        btnDownloadAll = findViewById(R.id.btnDownloadAll);

        // Informations personnelles
        tvPatientName = findViewById(R.id.tvPatientName);
        tvPatientAge = findViewById(R.id.tvPatientAge);
        tvBloodType = findViewById(R.id.tvBloodType);
        tvAllergies = findViewById(R.id.tvAllergies);

        // Cartes catégories
        cardResultatsLab = findViewById(R.id.cardResultatsLab);
        cardHistorique = findViewById(R.id.cardHistorique);
        cardMedicaments = findViewById(R.id.cardMedicaments);
        cardImagerie = findViewById(R.id.cardImagerie);

        // Cartes résultats
        cardResultat1 = findViewById(R.id.cardResultat1);
        cardResultat2 = findViewById(R.id.cardResultat2);
        cardResultat3 = findViewById(R.id.cardResultat3);
    }

    private void loadPatientData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            // Load patient information from users and patients tables
            cursor = db.rawQuery(
                "SELECT u.full_name, u.email, p.date_of_birth, p.blood_type, p.emergency_contact " +
                "FROM users u LEFT JOIN patients p ON u.id = p.user_id " +
                "WHERE u.id = ?",
                new String[]{String.valueOf(patientId)}
            );

            if (cursor.moveToFirst()) {
                String name = cursor.getString(0);
                String birthDate = cursor.getString(2);
                String bloodType = cursor.getString(3);
                String emergencyContact = cursor.getString(4);
                
                tvPatientName.setText(name);
                tvBloodType.setText(bloodType != null ? bloodType : "Non renseigné");
                
                // Calculate age from birth date
                if (birthDate != null && !birthDate.isEmpty()) {
                    try {
                        String[] dateParts = birthDate.split("-");
                        if (dateParts.length == 3) {
                            int birthYear = Integer.parseInt(dateParts[0]);
                            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                            int age = currentYear - birthYear;
                            tvPatientAge.setText(age + " ans");
                        } else {
                            tvPatientAge.setText("Non renseigné");
                        }
                    } catch (Exception e) {
                        tvPatientAge.setText("Non renseigné");
                    }
                } else {
                    tvPatientAge.setText("Non renseigné");
                }
                
                // Load allergies from medical records
                loadAllergies();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur chargement données", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }
    }
    
    private void loadAllergies() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        
        try {
            cursor = db.rawQuery(
                "SELECT diagnosis FROM medical_records " +
                "WHERE patient_id = ? AND (diagnosis LIKE '%allergie%' OR diagnosis LIKE '%allergique%') " +
                "ORDER BY created_at DESC LIMIT 3",
                new String[]{String.valueOf(patientId)}
            );
            
            StringBuilder allergies = new StringBuilder();
            while (cursor.moveToNext()) {
                if (allergies.length() > 0) allergies.append(", ");
                allergies.append(cursor.getString(0));
            }
            
            tvAllergies.setText(allergies.length() > 0 ? allergies.toString() : "Aucune allergie connue");
            
        } catch (Exception e) {
            tvAllergies.setText("Non renseigné");
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
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Retour à la page précédente
            }
        });

        // Bouton télécharger tout
        btnDownloadAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(page_dossier_medical.this,
                        "Téléchargement de tous les documents...", Toast.LENGTH_SHORT).show();
                // TODO: Implémenter le téléchargement de tous les documents
            }
        });

        // Cartes catégories
        cardResultatsLab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(page_dossier_medical.this, MedicalHistoryTimelineActivity.class);
                startActivity(intent);
            }
        });

        cardHistorique.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(page_dossier_medical.this, PatientMedicalRecordsActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(page_dossier_medical.this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        cardMedicaments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(page_dossier_medical.this, page_medicament.class);
                startActivity(intent);
            }
        });

        cardImagerie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(page_dossier_medical.this,
                        "Rapports d'Imagerie", Toast.LENGTH_SHORT).show();
                // TODO: Ouvrir page imagerie
            }
        });

        // Cartes résultats individuels - Load from database
        loadRecentTestResults();
    }
}
