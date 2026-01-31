package M.health;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class page_medicament extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private AuthManager authManager;

    // UI Components
    private ImageView btnBack;
    private ImageView btnAddReminder;
    private Button btnMarquerPris;
    private Button btnRenouveler1;
    private Button btnRenouveler2;
    private Button btnDetails3;
    private CardView cardMedicament1;
    private CardView cardMedicament2;
    private CardView cardMedicament3;

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

        // Setup reusable header with sign out functionality
        View headerView = findViewById(R.id.headerLayout);
        UIHelper.setupHeaderWithSignOut(this, headerView, "Mes Médicaments", authManager);

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
        btnMarquerPris = findViewById(R.id.btnMarquerPris);
        btnRenouveler1 = findViewById(R.id.btnRenouveler1);
        btnRenouveler2 = findViewById(R.id.btnRenouveler2);
        btnDetails3 = findViewById(R.id.btnDetails3);
        cardMedicament1 = findViewById(R.id.cardMedicament1);
        cardMedicament2 = findViewById(R.id.cardMedicament2);
        cardMedicament3 = findViewById(R.id.cardMedicament3);
    }

    private void setupClickListeners() {
        // Bouton Retour
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); // Retour à l'écran précédent
                }
            });
        }

        // Bouton Ajouter un rappel
        if (btnAddReminder != null) {
            btnAddReminder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(page_medicament.this,
                            "Ajouter un rappel - À venir", Toast.LENGTH_SHORT).show();
                    // TODO: Créer l'activité pour ajouter un rappel
                }
            });
        }

        // Bouton Marquer comme pris
        if (btnMarquerPris != null) {
            btnMarquerPris.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    marquerMedicamentPris("Doliprane 1000mg");
                }
            });
        }

        // Bouton Renouveler Médicament 1
        if (btnRenouveler1 != null) {
            btnRenouveler1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    renouvellerOrdonnance("Doliprane 1000mg");
                }
            });
        }

        // Bouton Renouveler Médicament 2 (urgent)
        if (btnRenouveler2 != null) {
            btnRenouveler2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    renouvellerOrdonnance("Amoxicilline 500mg");
                }
            });
        }

        // Bouton Détails Médicament 3
        if (btnDetails3 != null) {
            btnDetails3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    afficherDetailsMedicament("Aspirine 100mg");
                }
            });
        }

        // Click sur carte médicament 1
        if (cardMedicament1 != null) {
            cardMedicament1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    afficherDetailsMedicament("Doliprane 1000mg");
                }
            });
        }

        // Click sur carte médicament 2
        if (cardMedicament2 != null) {
            cardMedicament2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    afficherDetailsMedicament("Amoxicilline 500mg");
                }
            });
        }

        // Click sur carte médicament 3
        if (cardMedicament3 != null) {
            cardMedicament3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    afficherDetailsMedicament("Aspirine 100mg");
                }
            });
        }
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
        // Hide all cards initially
        cardMedicament1.setVisibility(View.GONE);
        cardMedicament2.setVisibility(View.GONE);
        cardMedicament3.setVisibility(View.GONE);
        
        int cardIndex = 0;
        while (cursor.moveToNext() && cardIndex < 3) {
            String medication = cursor.getString(1);
            String dosage = cursor.getString(2);
            String doctorName = cursor.getString(4);
            
            // Show and update the appropriate card
            CardView card = cardIndex == 0 ? cardMedicament1 : 
                           cardIndex == 1 ? cardMedicament2 : cardMedicament3;
            card.setVisibility(View.VISIBLE);
            
            // Update medication name in the card (you'll need to add TextViews to the layout)
            // For now, we'll update the click listeners with real data
            final String finalMedication = medication;
            card.setOnClickListener(v -> afficherDetailsMedicament(finalMedication));
            
            cardIndex++;
        }
    }

    private void marquerMedicamentPris(String medicamentNom) {
        // TODO: Enregistrer la prise du médicament dans la base de données
        Toast.makeText(this,
                "✓ " + medicamentNom + " marqué comme pris",
                Toast.LENGTH_SHORT).show();

        // Mettre à jour l'interface si nécessaire
        // Par exemple, changer la couleur ou désactiver le bouton
    }

    private void renouvellerOrdonnance(String medicamentNom) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        AuthManager.User currentUser = authManager.getCurrentUser();
        
        try {
            // Find the prescription ID for this medication
            Cursor cursor = db.rawQuery(
                "SELECT id FROM prescriptions WHERE patient_id = ? AND medication = ? ORDER BY created_at DESC LIMIT 1",
                new String[]{String.valueOf(currentUser.id), medicamentNom});
            
            if (cursor.moveToFirst()) {
                int prescriptionId = cursor.getInt(0);
                cursor.close();
                
                // Check if there's already a pending request
                Cursor existingRequest = db.rawQuery(
                    "SELECT id FROM prescription_refill_requests WHERE prescription_id = ? AND status = 'pending'",
                    new String[]{String.valueOf(prescriptionId)});
                
                if (existingRequest.moveToFirst()) {
                    existingRequest.close();
                    Toast.makeText(this, "Une demande de renouvellement est déjà en cours", Toast.LENGTH_SHORT).show();
                    return;
                }
                existingRequest.close();
                
                // Create new refill request
                android.content.ContentValues values = new android.content.ContentValues();
                values.put("prescription_id", prescriptionId);
                values.put("status", "pending");
                
                long result = db.insert("prescription_refill_requests", null, values);
                
                if (result != -1) {
                    Toast.makeText(this, "✓ Demande de renouvellement envoyée pour " + medicamentNom, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Erreur lors de l'envoi de la demande", Toast.LENGTH_SHORT).show();
                }
            } else {
                cursor.close();
                Toast.makeText(this, "Prescription non trouvée", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void afficherDetailsMedicament(String medicamentNom) {
        // TODO: Ouvrir une activité détaillée pour le médicament
        Toast.makeText(this,
                "Détails de " + medicamentNom,
                Toast.LENGTH_SHORT).show();

        // Intent intent = new Intent(this, MedicamentDetailsActivity.class);
        // intent.putExtra("medicament_nom", medicamentNom);
        // startActivity(intent);
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
