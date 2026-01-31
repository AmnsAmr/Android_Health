package M.health;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class ComposeMessageActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private Spinner spinnerDoctors;
    private EditText etMessage;
    private List<Integer> doctorIds;
    private List<String> doctorNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_message);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        
        if (!authManager.isLoggedIn()) {
            finish();
            return;
        }

        View headerView = findViewById(R.id.headerLayout);
        UIHelper.setupHeaderWithSignOut(this, headerView, "Nouveau Message", authManager);
        
        spinnerDoctors = findViewById(R.id.spinnerDoctors);
        etMessage = findViewById(R.id.etMessage);
        
        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
        
        loadDoctors();
    }

    private void loadDoctors() {
        doctorIds = new ArrayList<>();
        doctorNames = new ArrayList<>();
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT u.id, u.full_name, d.specialization " +
            "FROM users u " +
            "JOIN doctors d ON u.id = d.user_id " +
            "WHERE u.role = 'doctor'", null);

        while (cursor.moveToNext()) {
            doctorIds.add(cursor.getInt(0));
            doctorNames.add("Dr. " + cursor.getString(1) + " - " + cursor.getString(2));
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, doctorNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDoctors.setAdapter(adapter);
    }

    private void sendMessage() {
        if (doctorIds.isEmpty()) {
            Toast.makeText(this, "Aucun médecin disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir un message", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPosition = spinnerDoctors.getSelectedItemPosition();
        int doctorId = doctorIds.get(selectedPosition);
        int currentUserId = authManager.getCurrentUser().id;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sender_id", currentUserId);
        values.put("receiver_id", doctorId);
        values.put("message", messageText);
        values.put("is_urgent", 0);
        
        long result = db.insert("messages", null, values);
        
        if (result != -1) {
            Toast.makeText(this, "Message envoyé", Toast.LENGTH_SHORT).show();
            
            Intent intent = new Intent(this, ConversationActivity.class);
            intent.putExtra("other_user_id", doctorId);
            intent.putExtra("other_user_name", doctorNames.get(selectedPosition));
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Erreur lors de l'envoi", Toast.LENGTH_SHORT).show();
        }
    }
}