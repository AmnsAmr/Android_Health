package M.health;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private AuthManager authManager;
    private DatabaseHelper dbHelper;
    
    private ImageView ivProfilePicture;
    private EditText etFullName, etEmail, etCurrentPassword, etNewPassword, etConfirmPassword;
    private TextView tvRole, tvUserId, tvLastLogin, tvHeaderTitle;
    private Button btnSaveProfile, btnChangePassword, btnLogout, btnChangeProfilePicture;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        authManager = AuthManager.getInstance(this);
        dbHelper = new DatabaseHelper(this);

        if (!authManager.validateSession()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initializeViews();
        loadUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        btnBack = findViewById(R.id.btnBack);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        tvRole = findViewById(R.id.tvRole);
        tvUserId = findViewById(R.id.tvUserId);
        tvLastLogin = findViewById(R.id.tvLastLogin);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        btnChangeProfilePicture = findViewById(R.id.btnChangeProfilePicture);

        tvHeaderTitle.setText("Paramètres");
    }

    private void loadUserData() {
        AuthManager.User currentUser = authManager.getCurrentUser();
        if (currentUser == null) return;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT full_name, email, role, last_login FROM users WHERE id = ?",
            new String[]{String.valueOf(currentUser.id)});

        if (cursor.moveToFirst()) {
            etFullName.setText(cursor.getString(0));
            etEmail.setText(cursor.getString(1));
            tvRole.setText(getRoleDisplay(cursor.getString(2)));
            tvUserId.setText(String.valueOf(currentUser.id));
            
            long lastLogin = cursor.getLong(3);
            if (lastLogin > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE);
                tvLastLogin.setText(sdf.format(new Date(lastLogin)));
            } else {
                tvLastLogin.setText("Jamais connecté");
            }
        }
        cursor.close();
    }

    private String getRoleDisplay(String role) {
        switch (role) {
            case "admin": return "Administrateur";
            case "doctor": return "Médecin";
            case "patient": return "Patient";
            case "secretary": return "Secrétaire";
            default: return role;
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSaveProfile.setOnClickListener(v -> saveProfile());
        
        btnChangePassword.setOnClickListener(v -> changePassword());
        
        btnLogout.setOnClickListener(v -> logout());
        
        btnChangeProfilePicture.setOnClickListener(v -> 
            Toast.makeText(this, "Fonctionnalité à venir", Toast.LENGTH_SHORT).show());
    }

    private void saveProfile() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Email invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthManager.User currentUser = authManager.getCurrentUser();
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Check if email already exists for another user
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE email = ? AND id != ?", 
            new String[]{email, String.valueOf(currentUser.id)});
        if (cursor.moveToFirst()) {
            cursor.close();
            Toast.makeText(this, "Cet email est déjà utilisé", Toast.LENGTH_SHORT).show();
            return;
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put("full_name", fullName);
        values.put("email", email);

        int result = db.update("users", values, "id = ?", 
            new String[]{String.valueOf(currentUser.id)});
        
        if (result > 0) {
            Toast.makeText(this, "Profil mis à jour avec succès", Toast.LENGTH_SHORT).show();
            // Refresh auth manager with updated user data
            authManager.refreshPermissions();
        } else {
            Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
        }
    }

    private void changePassword() {
        String currentPassword = etCurrentPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Les nouveaux mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthManager.User currentUser = authManager.getCurrentUser();
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Verify current password
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE id = ? AND password_hash = ?",
            new String[]{String.valueOf(currentUser.id), currentPassword});
        
        if (!cursor.moveToFirst()) {
            cursor.close();
            Toast.makeText(this, "Mot de passe actuel incorrect", Toast.LENGTH_SHORT).show();
            return;
        }
        cursor.close();

        // Update password
        ContentValues values = new ContentValues();
        values.put("password_hash", newPassword);

        int result = db.update("users", values, "id = ?", 
            new String[]{String.valueOf(currentUser.id)});
        
        if (result > 0) {
            Toast.makeText(this, "Mot de passe changé avec succès", Toast.LENGTH_SHORT).show();
            etCurrentPassword.setText("");
            etNewPassword.setText("");
            etConfirmPassword.setText("");
        } else {
            Toast.makeText(this, "Erreur lors du changement de mot de passe", Toast.LENGTH_SHORT).show();
        }
    }

    private void logout() {
        authManager.logout();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }
}