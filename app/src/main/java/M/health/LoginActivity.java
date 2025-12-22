package M.health;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private EditText emailEdit, passwordEdit;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        emailEdit = findViewById(R.id.emailEdit);
        passwordEdit = findViewById(R.id.passwordEdit);
        Button loginBtn = findViewById(R.id.loginBtn);
        Button registerBtn = findViewById(R.id.registerBtn);

        loginBtn.setOnClickListener(v -> login());
        registerBtn.setOnClickListener(v -> 
            startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void login() {
        String email = emailEdit.getText().toString();
        String password = passwordEdit.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, role FROM users WHERE email = ? AND password_hash = ?", 
                                   new String[]{email, password});

        if (cursor.moveToFirst()) {
            int userId = cursor.getInt(0);
            String role = cursor.getString(1);
            cursor.close();

            prefs.edit()
                .putInt("user_id", userId)
                .putString("user_role", role)
                .putString("user_email", email)
                .apply();

            Intent intent;
            switch (role) {
                case "admin":
                    intent = new Intent(this, AdminDashboardActivity.class);
                    break;
                case "patient":
                    intent = new Intent(this, PatientDashboardActivity.class);
                    break;
                case "doctor":
                    intent = new Intent(this, DoctorDashboardActivity.class);
                    break;
                case "secretary":
                    intent = new Intent(this, SecretaryDashboardActivity.class);
                    break;
                default:
                    Toast.makeText(this, "Rôle non reconnu", Toast.LENGTH_SHORT).show();
                    return;
            }
            startActivity(intent);
            finish();
        } else {
            cursor.close();
            Toast.makeText(this, "Email ou mot de passe incorrect", Toast.LENGTH_SHORT).show();
        }
    }
}