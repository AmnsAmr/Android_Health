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

        authManager = AuthManager.getInstance(this);

        // Check if user is already logged in
        if (authManager.isLoggedIn() && authManager.validateSession()) {
            redirectToDashboard();
            return;
        }

        setContentView(R.layout.activity_login);

        emailEdit = findViewById(R.id.emailEdit);
        passwordEdit = findViewById(R.id.passwordEdit);
        Button loginBtn = findViewById(R.id.loginBtn);

        loginBtn.setOnClickListener(v -> login());
    }

    private void login() {
        String email = emailEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthManager.User user = authManager.login(email, password);
        if (user != null) {
            redirectToDashboard();
        } else {
            Toast.makeText(this, "Email ou mot de passe incorrect", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Redirect user to appropriate dashboard based on role and permissions
     */
    private void redirectToDashboard() {
        AuthManager.User user = authManager.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Erreur de session", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = null;

        switch (user.role) {
            case "admin":
                if (authManager.hasPermission("admin_manage_users")) {
                    intent = new Intent(this, AdminDashboardActivity.class);
                } else {
                    showAccessDenied();
                    return;
                }
                break;

            case "patient":
                if (authManager.hasPermission("patient_view_own_records")) {
                    intent = new Intent(this, PatientDashboardActivity.class);
                } else {
                    showAccessDenied();
                    return;
                }
                break;

            case "doctor":
                if (authManager.hasPermission("doctor_view_patients")) {
                    intent = new Intent(this, DoctorDashboardActivity.class);
                } else {
                    showAccessDenied();
                    return;
                }
                break;

            case "secretary":
                if (authManager.hasPermission("secretary_view_patient_list")) {
                    intent = new Intent(this, SecretaryDashboardActivity.class);
                } else {
                    showAccessDenied();
                    return;
                }
                break;

            default:
                Toast.makeText(this, "Rôle inconnu: " + user.role, Toast.LENGTH_SHORT).show();
                authManager.logout();
                return;
        }

        if (intent != null) {
            // Clear the back stack so user can't return to login with back button
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void showAccessDenied() {
        Toast.makeText(this, "Accès refusé - Permissions insuffisantes", Toast.LENGTH_LONG).show();
        authManager.logout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if returning from registration or other activity
        if (authManager.isLoggedIn() && authManager.validateSession()) {
            redirectToDashboard();
        }
    }
}