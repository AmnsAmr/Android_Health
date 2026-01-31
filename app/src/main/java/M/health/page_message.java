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

        // Setup reusable header with sign out functionality
        View headerView = findViewById(R.id.headerLayout);
        UIHelper.setupHeaderWithSignOut(this, headerView, "Messages", authManager);

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
            btnBack.setOnClickListener(v -> finish());
        }

        // Bouton Nouveau Message
        if (btnNewMessage != null) {
            btnNewMessage.setOnClickListener(v -> ouvrirNouveauMessage());
        }

        // Load and setup conversation cards dynamically
        setupConversationCards();
    }
    
    private void setupConversationCards() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        AuthManager.User currentUser = authManager.getCurrentUser();
        int userId = currentUser.id;
        
        // Hide all cards initially
        cardConversation1.setVisibility(View.GONE);
        cardConversation2.setVisibility(View.GONE);
        cardConversation3.setVisibility(View.GONE);
        cardConversation4.setVisibility(View.GONE);
        
        try {
            String query = "SELECT DISTINCT " +
                    "CASE WHEN m.sender_id = ? THEN m.receiver_id ELSE m.sender_id END as other_user_id, " +
                    "u.full_name, " +
                    "u.role, " +
                    "COALESCE(d.specialization, 'Secrétaire') as specialization, " +
                    "MAX(m.sent_at) as last_message_time " +
                    "FROM messages m " +
                    "JOIN users u ON (CASE WHEN m.sender_id = ? THEN m.receiver_id ELSE m.sender_id END) = u.id " +
                    "LEFT JOIN doctors d ON u.id = d.user_id " +
                    "WHERE m.sender_id = ? OR m.receiver_id = ? " +
                    "GROUP BY other_user_id " +
                    "ORDER BY last_message_time DESC LIMIT 4";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), String.valueOf(userId), 
                                                           String.valueOf(userId), String.valueOf(userId)});

            CardView[] cards = {cardConversation1, cardConversation2, cardConversation3, cardConversation4};
            int cardIndex = 0;
            
            while (cursor.moveToNext() && cardIndex < 4) {
                final int otherUserId = cursor.getInt(0);
                final String fullName = cursor.getString(1);
                final String role = cursor.getString(2);
                final String specialization = cursor.getString(3);
                
                String displayName = role.equals("doctor") ? "Dr. " + fullName : fullName;
                
                cards[cardIndex].setVisibility(View.VISIBLE);
                cards[cardIndex].setOnClickListener(v -> 
                    ouvrirConversation(otherUserId, displayName, specialization));
                
                cardIndex++;
            }
            cursor.close();
            
            // If no conversations exist, show message
            if (cardIndex == 0) {
                Toast.makeText(this, "Aucune conversation. Créez un nouveau message.", Toast.LENGTH_LONG).show();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur lors du chargement des conversations", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMessagesData(int userId) {
        // This method is now handled by setupConversationCards()
        // Keep for compatibility but functionality moved to setupConversationCards
    }

    private void ouvrirNouveauMessage() {
        Intent intent = new Intent(this, ComposeMessageActivity.class);
        startActivity(intent);
    }

    private void ouvrirConversation(int otherUserId, String doctorName, String speciality) {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra("other_user_id", otherUserId);
        intent.putExtra("other_user_name", doctorName);
        startActivity(intent);
    }



    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les données quand on revient sur cette page
        if (authManager.isLoggedIn() && authManager.validateSession()) {
            setupConversationCards(); // Refresh conversations
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
