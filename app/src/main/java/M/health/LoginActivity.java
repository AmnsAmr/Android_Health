package M.health;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private AuthManager authManager;
    private EditText emailEdit, passwordEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = AuthManager.getInstance(this);
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

        AuthManager.User user = authManager.login(email, password);
        if (user != null) {
            Intent intent;
            switch (user.role) {
                case "admin":
                    if (authManager.hasPermission("admin_manage_users")) {
                        intent = new Intent(this, AdminDashboardActivity.class);
                    } else {
                        Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    break;
                case "patient":
                    if (authManager.hasPermission("patient_view_own_records")) {
                        intent = new Intent(this, PatientDashboardActivity.class);
                    } else {
                        Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    break;
                case "doctor":
                    if (authManager.hasPermission("doctor_view_patients")) {
                        intent = new Intent(this, DoctorDashboardActivity.class);
                        intent.putExtra("user_id", user.id);
                    } else {
                        Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    break;
                case "secretary":
                    if (authManager.hasPermission("secretary_view_patient_list")) {
                        intent = new Intent(this, SecretaryDashboardActivity.class);
                        intent.putExtra("user_id", user.id);
                    } else {
                        Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    break;
                default:
                    Toast.makeText(this, "Rôle inconnu", Toast.LENGTH_SHORT).show();
                    return;
            }
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Email ou mot de passe incorrect", Toast.LENGTH_SHORT).show();
        }
    }
}
