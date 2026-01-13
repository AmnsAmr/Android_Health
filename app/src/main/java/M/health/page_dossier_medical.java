package M.health;

import android.content.SharedPreferences;
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
    private SharedPreferences prefs;
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
        prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        patientId = prefs.getInt("user_id", -1);

        if (patientId == -1) {
            Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
            // Charger les informations du patient depuis la table users
            cursor = db.rawQuery(
                    "SELECT full_name, email FROM users WHERE id = ?",
                    new String[]{String.valueOf(patientId)}
            );

            if (cursor.moveToFirst()) {
                String name = cursor.getString(0);
                tvPatientName.setText(name);
            }

            // Pour l'instant, on affiche des données statiques
            // Vous pouvez les remplacer par des données de la base plus tard
            tvPatientAge.setText("35 ans");
            tvBloodType.setText("A+");
            tvAllergies.setText("Pénicilline, Pollen");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur chargement données", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }
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
                Toast.makeText(page_dossier_medical.this,
                        "Résultats de Laboratoire", Toast.LENGTH_SHORT).show();
                // TODO: Ouvrir page détaillée des résultats de laboratoire
            }
        });

        cardHistorique.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(page_dossier_medical.this,
                        "Historique Médical", Toast.LENGTH_SHORT).show();
                // TODO: Ouvrir page historique médical
            }
        });

        cardMedicaments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(page_dossier_medical.this,
                        "Médicaments Actuels", Toast.LENGTH_SHORT).show();
                // TODO: Ouvrir page médicaments
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

        // Cartes résultats individuels
        cardResultat1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(page_dossier_medical.this,
                        "Détails: Bilan Sanguin Complet", Toast.LENGTH_SHORT).show();
                // TODO: Afficher détails du résultat
            }
        });

        cardResultat2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(page_dossier_medical.this,
                        "Détails: Analyse d'Urine", Toast.LENGTH_SHORT).show();
                // TODO: Afficher détails du résultat
            }
        });

        cardResultat3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(page_dossier_medical.this,
                        "Détails: ECG", Toast.LENGTH_SHORT).show();
                // TODO: Afficher détails du résultat
            }
        });
    }
}
