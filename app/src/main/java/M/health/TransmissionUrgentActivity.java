package M.health;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class TransmissionUrgentActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private Spinner spinnerDestinataire;
    private EditText etObjet, etMessage;
    private Button btnEnvoyer, btnRetour;
    private List<Integer> doctorIds;
    private List<String> doctorNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transmission_urgent);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        
        if (!authManager.isLoggedIn()) {
            finish();
            return;
        }

        // Setup header
        View headerView = findViewById(R.id.headerLayout);
        UIHelper.setupHeaderWithSignOut(this, headerView, "Message Urgent", authManager);

        spinnerDestinataire = findViewById(R.id.spinner_dest_medecin);
        etObjet = findViewById(R.id.et_objet_urgent);
        etMessage = findViewById(R.id.et_corps_message);
        btnEnvoyer = findViewById(R.id.btn_envoyer_urgent);
        btnRetour = findViewById(R.id.btn_retour_menu);

        loadDoctors();

        btnEnvoyer.setOnClickListener(v -> envoyerAlerte());
        btnRetour.setOnClickListener(v -> finish());
    }

    private void loadDoctors() {
        doctorIds = new ArrayList<>();
        doctorNames = new ArrayList<>();
        
        doctorNames.add("Choisir le destinataire...");
        doctorIds.add(-1);
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT u.id, u.full_name, d.specialization " +
            "FROM users u " +
            "JOIN doctors d ON u.id = d.user_id " +
            "WHERE u.role = 'doctor'", null);

        while (cursor.moveToNext()) {
            doctorIds.add(cursor.getInt(0));
            doctorNames.add("Dr. " + cursor.getString(1) + " (" + cursor.getString(2) + ")");
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, doctorNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDestinataire.setAdapter(adapter);
    }

    private void envoyerAlerte() {
        String objet = etObjet.getText().toString().trim();
        String message = etMessage.getText().toString().trim();
        int position = spinnerDestinataire.getSelectedItemPosition();

        if (position == 0) {
            Toast.makeText(this, "Veuillez choisir un médecin destinataire", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (objet.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "L'objet et le message sont obligatoires", Toast.LENGTH_SHORT).show();
            return;
        }

        int doctorId = doctorIds.get(position);
        int senderId = authManager.getCurrentUser().id;
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Send urgent message
        ContentValues messageValues = new ContentValues();
        messageValues.put("sender_id", senderId);
        messageValues.put("receiver_id", doctorId);
        messageValues.put("message", "[URGENT] " + objet + "\n\n" + message);
        messageValues.put("is_urgent", 1);
        
        long messageResult = db.insert("messages", null, messageValues);
        
        // Create urgent notification
        ContentValues notificationValues = new ContentValues();
        notificationValues.put("user_id", doctorId);
        notificationValues.put("content", "MESSAGE URGENT: " + objet);
        notificationValues.put("is_read", 0);
        
        long notificationResult = db.insert("notifications", null, notificationValues);
        
        if (messageResult != -1 && notificationResult != -1) {
            String destinataire = doctorNames.get(position);
            Toast.makeText(this, "ALERTE URGENTE ENVOYÉE à " + destinataire, Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Erreur lors de l'envoi", Toast.LENGTH_SHORT).show();
        }
    }
}