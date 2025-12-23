package M.health;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SecretaryDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_secretary_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.secretaire_title), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the button from the XML
        Button btnManagePatients = findViewById(R.id.btn_nav_acces_patients);

        // Set the click listener
        btnManagePatients.setOnClickListener(v -> {
            Intent intent = new Intent(SecretaryDashboardActivity.this, ManagePatientsActivity.class);
            //
            // We pass the role "secretary" to the next activity
            intent.putExtra("USER_ROLE", "secretary");
            startActivity(intent);
        });

        // You can add listeners for other buttons (btn_nav_gestion_rdv, etc.) here later
    }
}