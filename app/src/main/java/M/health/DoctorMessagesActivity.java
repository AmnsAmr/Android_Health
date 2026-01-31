package M.health;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class DoctorMessagesActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private ImageView btnBack, btnNewMessage;
    private CardView cardConversation1, cardConversation2, cardConversation3, cardConversation4;

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

        btnBack = findViewById(R.id.btnBack);
        btnNewMessage = findViewById(R.id.btnNewMessage);
        cardConversation1 = findViewById(R.id.cardConversation1);
        cardConversation2 = findViewById(R.id.cardConversation2);
        cardConversation3 = findViewById(R.id.cardConversation3);
        cardConversation4 = findViewById(R.id.cardConversation4);

        btnBack.setOnClickListener(v -> finish());
        btnNewMessage.setOnClickListener(v -> startActivity(new Intent(this, DoctorComposeMessageActivity.class)));

        setupConversationCards();
    }

    private void setupConversationCards() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int userId = authManager.getUserId();
        
        Log.d("DoctorMessages", "Loading messages for doctor ID: " + userId);

        cardConversation1.setVisibility(View.GONE);
        cardConversation2.setVisibility(View.GONE);
        cardConversation3.setVisibility(View.GONE);
        cardConversation4.setVisibility(View.GONE);
        
        // First check if there are any messages at all
        Cursor checkCursor = db.rawQuery("SELECT COUNT(*) FROM messages WHERE sender_id = ? OR receiver_id = ?", 
            new String[]{String.valueOf(userId), String.valueOf(userId)});
        if (checkCursor.moveToFirst()) {
            int count = checkCursor.getInt(0);
            Log.d("DoctorMessages", "Total messages involving this doctor: " + count);
        }
        checkCursor.close();

        try {
            String query = "SELECT DISTINCT " +
                    "CASE WHEN m.sender_id = ? THEN m.receiver_id ELSE m.sender_id END as other_user_id, " +
                    "u.full_name, " +
                    "u.role, " +
                    "MAX(m.sent_at) as last_message_time, " +
                    "MAX(m.is_urgent) as has_urgent " +
                    "FROM messages m " +
                    "JOIN users u ON (CASE WHEN m.sender_id = ? THEN m.receiver_id ELSE m.sender_id END) = u.id " +
                    "WHERE m.sender_id = ? OR m.receiver_id = ? " +
                    "GROUP BY other_user_id " +
                    "ORDER BY has_urgent DESC, last_message_time DESC LIMIT 4";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), String.valueOf(userId),
                    String.valueOf(userId), String.valueOf(userId)});
            
            Log.d("DoctorMessages", "Query returned " + cursor.getCount() + " conversations");

            CardView[] cards = {cardConversation1, cardConversation2, cardConversation3, cardConversation4};
            int cardIndex = 0;

            while (cursor.moveToNext() && cardIndex < 4) {
                final int otherUserId = cursor.getInt(0);
                final String fullName = cursor.getString(1);
                final String role = cursor.getString(2);
                final int hasUrgent = cursor.getInt(4);
                
                Log.d("DoctorMessages", "Conversation with: " + fullName + " (ID: " + otherUserId + ", Role: " + role + ", Urgent: " + hasUrgent + ")");

                CardView card = cards[cardIndex];
                card.setVisibility(View.VISIBLE);

                TextView tvName = card.findViewById(android.R.id.text1);
                TextView tvSpec = card.findViewById(android.R.id.text2);
                if (tvName != null) tvName.setText(fullName + (hasUrgent == 1 ? " ⚠️" : ""));
                if (tvSpec != null) tvSpec.setText(role.equals("patient") ? "Patient" : "Secrétaire");

                card.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ConversationActivity.class);
                    intent.putExtra("other_user_id", otherUserId);
                    intent.putExtra("other_user_name", fullName);
                    startActivity(intent);
                });

                cardIndex++;
            }
            cursor.close();

            if (cardIndex == 0) {
                Log.d("DoctorMessages", "No conversations found");
                Toast.makeText(this, "Aucune conversation. Créez un nouveau message.", Toast.LENGTH_LONG).show();
            } else {
                Log.d("DoctorMessages", "Loaded " + cardIndex + " conversations");
            }

        } catch (Exception e) {
            Log.e("DoctorMessages", "Error loading conversations", e);
            e.printStackTrace();
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authManager.isLoggedIn() && authManager.validateSession()) {
            setupConversationCards();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}
