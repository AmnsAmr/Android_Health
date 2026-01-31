package M.health;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;

public class DoctorComposeMessageActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private Spinner spinnerRecipient;
    private EditText etMessageContent;
    private Button btnSend;
    private ImageView btnBack;
    private HashMap<String, Integer> recipientMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_compose_message);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        btnBack = findViewById(R.id.btnBack);
        spinnerRecipient = findViewById(R.id.spinnerRecipient);
        etMessageContent = findViewById(R.id.etMessageContent);
        btnSend = findViewById(R.id.btnSend);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());

        loadRecipients();
    }

    private void loadRecipients() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        recipientMap = new HashMap<>();
        ArrayList<String> recipientNames = new ArrayList<>();

        // Load patients
        Cursor cursor = db.rawQuery(
                "SELECT u.id, u.full_name FROM users u " +
                        "JOIN patients p ON u.id = p.user_id " +
                        "WHERE u.is_active = 1",
                null);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            recipientNames.add(name + " (Patient)");
            recipientMap.put(name + " (Patient)", id);
        }
        cursor.close();

        // Load secretaries
        cursor = db.rawQuery(
                "SELECT u.id, u.full_name FROM users u " +
                        "WHERE u.role = 'secretary' AND u.is_active = 1",
                null);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            recipientNames.add(name + " (Secrétaire)");
            recipientMap.put(name + " (Secrétaire)", id);
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, recipientNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecipient.setAdapter(adapter);
    }

    private void sendMessage() {
        String selectedRecipient = (String) spinnerRecipient.getSelectedItem();
        String messageContent = etMessageContent.getText().toString().trim();

        if (selectedRecipient == null || selectedRecipient.isEmpty()) {
            Toast.makeText(this, "Sélectionnez un destinataire", Toast.LENGTH_SHORT).show();
            return;
        }

        if (messageContent.isEmpty()) {
            Toast.makeText(this, "Entrez un message", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer recipientId = recipientMap.get(selectedRecipient);
        if (recipientId == null) {
            Toast.makeText(this, "Destinataire invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sender_id", authManager.getUserId());
        values.put("receiver_id", recipientId);
        values.put("message_content", messageContent);
        values.put("sent_at", System.currentTimeMillis());
        values.put("is_read", 0);

        long result = db.insert("messages", null, values);

        if (result != -1) {
            Toast.makeText(this, "Message envoyé", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Erreur lors de l'envoi", Toast.LENGTH_SHORT).show();
        }
    }
}
