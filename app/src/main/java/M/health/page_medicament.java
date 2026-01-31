package M.health;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class page_medicament extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private ImageView btnBack, btnAddReminder;
    private CardView cardMedicament1, cardMedicament2, cardMedicament3;
    private Button btnRenouveler1, btnRenouveler2, btnDetails3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_medicament);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            Toast.makeText(this, "Erreur: Session expirée", Toast.LENGTH_SHORT).show();
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
        int userId = currentUser.id;

        // Custom header with back button
        // Header elements are defined in the layout file

        // Initialize Views
        initializeViews();

        // Setup Click Listeners
        setupClickListeners();

        // Load medications data
        loadMedicationsData(userId);
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnAddReminder = findViewById(R.id.btnAddReminder);
        btnRenouveler1 = findViewById(R.id.btnRenouveler1);
        btnRenouveler2 = findViewById(R.id.btnRenouveler2);
        btnDetails3 = findViewById(R.id.btnDetails3);
        cardMedicament1 = findViewById(R.id.cardMedicament1);
        cardMedicament2 = findViewById(R.id.cardMedicament2);
        cardMedicament3 = findViewById(R.id.cardMedicament3);
    }

    private void setupClickListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnAddReminder != null) btnAddReminder.setOnClickListener(v -> 
            Toast.makeText(this, "Ajouter un rappel - À venir", Toast.LENGTH_SHORT).show());
    }

    private void loadMedicationsData(int patientId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            String query = "SELECT p.id, p.medication, p.dosage, p.instructions, u.full_name as doctor_name " +
                    "FROM prescriptions p " +
                    "JOIN users u ON p.doctor_id = u.id " +
                    "WHERE p.patient_id = ? ORDER BY p.created_at DESC";
            cursor = db.rawQuery(query, new String[]{String.valueOf(patientId)});

            if (cursor.getCount() == 0) {
                Toast.makeText(this, "Aucun médicament prescrit", Toast.LENGTH_SHORT).show();
            } else {
                // Update UI with real medication data
                updateMedicationCards(cursor);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur lors du chargement des médicaments", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }
    }
    
    private void updateMedicationCards(Cursor cursor) {
        TextView[] medNames = {
            findViewById(R.id.tvMedName1),
            findViewById(R.id.tvMedName2),
            findViewById(R.id.tvMedName3)
        };
        TextView[] medDosages = {
            findViewById(R.id.tvMedDosage1),
            findViewById(R.id.tvMedDosage2),
            findViewById(R.id.tvMedDosage3)
        };
        CardView[] cards = {cardMedicament1, cardMedicament2, cardMedicament3};
        Button[] renewButtons = {btnRenouveler1, btnRenouveler2, btnDetails3};
        int cardIndex = 0;
        
        while (cursor.moveToNext() && cardIndex < 3) {
            final String medication = cursor.getString(1);
            final String dosage = cursor.getString(2);
            final int prescriptionId = cursor.getInt(0);
            
            medNames[cardIndex].setText(medication);
            medDosages[cardIndex].setText(dosage);
            cards[cardIndex].setVisibility(View.VISIBLE);
            cards[cardIndex].setOnClickListener(v -> afficherDetailsMedicament(medication));
            
            if (renewButtons[cardIndex] != null) {
                renewButtons[cardIndex].setOnClickListener(v -> renouvellerOrdonnance(prescriptionId));
            }
            
            cardIndex++;
        }
    }

private void renouvellerOrdonnance(int prescriptionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        try {
            Cursor existingRequest = db.rawQuery(
                "SELECT id FROM prescription_refill_requests WHERE prescription_id = ? AND status = 'pending'",
                new String[]{String.valueOf(prescriptionId)});
            
            if (existingRequest.moveToFirst()) {
                existingRequest.close();
                Toast.makeText(this, "Une demande de renouvellement est déjà en cours", Toast.LENGTH_SHORT).show();
                return;
            }
            existingRequest.close();
            
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("prescription_id", prescriptionId);
            values.put("status", "pending");
            
            long result = db.insert("prescription_refill_requests", null, values);
            
            if (result != -1) {
                Toast.makeText(this, "✓ Demande de renouvellement envoyée", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Erreur lors de l'envoi de la demande", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void afficherDetailsMedicament(String medicamentNom) {
        Toast.makeText(this, "Détails de " + medicamentNom, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les données quand on revient sur cette page
        if (authManager.isLoggedIn() && authManager.validateSession()) {
            AuthManager.User currentUser = authManager.getCurrentUser();
            if (currentUser != null) {
                loadMedicationsData(currentUser.id);
            }
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
