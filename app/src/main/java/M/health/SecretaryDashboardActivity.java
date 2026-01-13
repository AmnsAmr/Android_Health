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

        Button btnManageRdv = findViewById(R.id.btn_gerer_rdv);
        btnManageRdv.setOnClickListener(v -> {
            Intent intent = new Intent(SecretaryDashboardActivity.this, ManageRdvActivity.class);
            startActivity(intent);
        });

        Button btnManagePatients = findViewById(R.id.btn_gerer_profils);
        btnManagePatients.setOnClickListener(v -> {
            Intent intent = new Intent(SecretaryDashboardActivity.this, UpdateDossierPatientActivity.class);
            startActivity(intent);
        });

        Button btnCoordination = findViewById(R.id.btn_coordination);
        btnCoordination.setOnClickListener(v -> {
            Intent intent = new Intent(SecretaryDashboardActivity.this, CoordinationMedecinActivity.class);
            startActivity(intent);
        });
    }
}