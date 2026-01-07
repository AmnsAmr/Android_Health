package M.health;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class page_message extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private AuthManager authManager;

    // UI Components
    private ImageView btnBack;
    private ImageView btnNewMessage;
    private CardView cardConversation1;
    private CardView cardConversation2;
    private CardView cardConversation3;
    private CardView cardConversation4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_message);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            Toast.makeText(this, "Erreur: Session expirée", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (!authManager.hasPermission("patient_message_doctor")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        AuthManager.User currentUser = authManager.getCurrentUser();
        int userId = currentUser.id;

        // Initialize Views
        initializeViews();

        // Setup Click Listeners
        setupClickListeners();

        // Load messages data
        loadMessagesData(userId);
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnNewMessage = findViewById(R.id.btnNewMessage);
        cardConversation1 = findViewById(R.id.cardConversation1);
        cardConversation2 = findViewById(R.id.cardConversation2);
        cardConversation3 = findViewById(R.id.cardConversation3);
        cardConversation4 = findViewById(R.id.cardConversation4);
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

        // Bouton Nouveau Message
        if (btnNewMessage != null) {
            btnNewMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ouvrirNouveauMessage();
                }
            });
        }

        // Click sur Conversation 1 (Dr. Rachid Bennani)
        if (cardConversation1 != null) {
            cardConversation1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ouvrirConversation(1, "Dr. Rachid Bennani", "Cardiologue");
                }
            });
        }

        // Click sur Conversation 2 (Dr. Fatima Zahra)
        if (cardConversation2 != null) {
            cardConversation2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ouvrirConversation(2, "Dr. Fatima Zahra", "Médecin généraliste");
                }
            });
        }

        // Click sur Conversation 3 (Dr. Karim El Alaoui)
        if (cardConversation3 != null) {
            cardConversation3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ouvrirConversation(3, "Dr. Karim El Alaoui", "Dermatologue");
                }
            });
        }

        // Click sur Conversation 4 (Secrétariat Médical)
        if (cardConversation4 != null) {
            cardConversation4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ouvrirConversation(4, "Secrétariat Médical", "Service administratif");
                }
            });
        }
    }

    private void loadMessagesData(int patientId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            // Exemple de requête pour charger les conversations
            // Cette requête devra être adaptée selon votre structure de base de données
            String query = "SELECT m.*, u.full_name, d.specialization " +
                    "FROM messages m " +
                    "JOIN users u ON m.sender_id = u.id " +
                    "LEFT JOIN doctors d ON u.id = d.user_id " +
                    "WHERE m.receiver_id = ? OR m.sender_id = ? " +
                    "GROUP BY m.conversation_id " +
                    "ORDER BY m.created_at DESC";

            cursor = db.rawQuery(query, new String[]{String.valueOf(patientId), String.valueOf(patientId)});

            // Traiter les résultats
            if (cursor.getCount() == 0) {
                // Les données statiques du XML seront affichées
                Toast.makeText(this, "Aucune conversation trouvée", Toast.LENGTH_SHORT).show();
            } else {
                // TODO: Mettre à jour l'interface avec les données de la base
                // Pour l'instant, les données statiques sont affichées
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Les données statiques du XML seront affichées en cas d'erreur
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void ouvrirNouveauMessage() {
        // TODO: Ouvrir une activité pour composer un nouveau message
        Toast.makeText(this,
                "Nouveau message - À venir",
                Toast.LENGTH_SHORT).show();

        // Intent intent = new Intent(this, ComposeMessageActivity.class);
        // startActivity(intent);
    }

    private void ouvrirConversation(int conversationId, String doctorName, String speciality) {
        // TODO: Ouvrir l'activité de conversation avec le médecin
        Toast.makeText(this,
                "Ouvrir conversation avec " + doctorName,
                Toast.LENGTH_SHORT).show();

        // Intent intent = new Intent(this, ConversationActivity.class);
        // intent.putExtra("conversation_id", conversationId);
        // intent.putExtra("doctor_name", doctorName);
        // intent.putExtra("speciality", speciality);
        // startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les données quand on revient sur cette page
        if (authManager.isLoggedIn() && authManager.validateSession()) {
            AuthManager.User currentUser = authManager.getCurrentUser();
            if (currentUser != null) {
                loadMessagesData(currentUser.id);
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
