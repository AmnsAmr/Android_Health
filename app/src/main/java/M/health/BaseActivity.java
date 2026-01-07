package M.health;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    protected AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = AuthManager.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!validateSession()) {
            redirectToLogin();
        }
    }

    protected boolean validateSession() {
        return authManager.isLoggedIn() && authManager.validateSession();
    }

    protected boolean checkPermission(String permission) {
        return authManager.hasPermission(permission);
    }

    protected void requirePermission(String permission) {
        if (!checkPermission(permission)) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    protected void redirectToLogin() {
        Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    protected AuthManager.User getCurrentUser() {
        return authManager.getCurrentUser();
    }
}