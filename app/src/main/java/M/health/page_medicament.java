package M.health;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private SharedPreferences prefs;

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
        prefs = getSharedPreferences("user_session", MODE_PRIVATE);

        // Retrieve current User ID from session
        int userId = prefs.getInt("user_id", -1);

        if (userId == -1) {
            Toast.makeText(this, "Erreur: Session expirée", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
        // TODO: Charger les données des médicaments depuis la base de données
        // Pour l'instant, les données sont statiques dans le XML
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            // Exemple de requête pour charger les médicaments
            // Cette requête devra être adaptée selon votre structure de base de données
            String query = "SELECT * FROM medications WHERE patient_id = ? ORDER BY created_at DESC";
            cursor = db.rawQuery(query, new String[]{String.valueOf(patientId)});

            // Traiter les résultats
            // Pour l'instant, afficher un message si aucun médicament n'est trouvé
            if (cursor.getCount() == 0) {
                // Les données statiques du XML seront affichées
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Les données statiques du XML seront affichées en cas d'erreur
        } finally {
            if (cursor != null) cursor.close();
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
        // TODO: Créer une demande de renouvellement d'ordonnance
        Toast.makeText(this,
                "Demande de renouvellement envoyée pour " + medicamentNom,
                Toast.LENGTH_LONG).show();

        // Possibilité d'ouvrir une activité pour la demande de renouvellement
        // ou d'envoyer une notification au médecin
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
        int userId = prefs.getInt("user_id", -1);
        if (userId != -1) {
            loadMedicationsData(userId);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
