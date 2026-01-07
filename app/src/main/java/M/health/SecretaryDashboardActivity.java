package M.health;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SecretaryDashboardActivity extends AppCompatActivity {

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_secretary_dashboard);

        authManager = AuthManager.getInstance(this);

        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (!authManager.hasPermission("secretary_view_patient_list")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.secretaire_title), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the button from the XML
        Button btnManagePatients = findViewById(R.id.btn_nav_acces_patients);

        // Set the click listener
        btnManagePatients.setOnClickListener(v -> {
            if (authManager.hasPermission("secretary_view_patient_list")) {
                Intent intent = new Intent(SecretaryDashboardActivity.this, ManagePatientsActivity.class);
                intent.putExtra("USER_ROLE", "secretary");
                startActivity(intent);
            } else {
                Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            }
        });

        // You can add listeners for other buttons (btn_nav_gestion_rdv, etc.) here later
    }
}