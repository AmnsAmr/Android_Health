package M.health;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class UpdateDossierPatientActivity extends AppCompatActivity {
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_dossier_patient);

        authManager = AuthManager.getInstance(this);
        
        if (!authManager.hasPermission("secretary_manage_patients")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Button btnNouveau = findViewById(R.id.btn_nouveau_dossier);
        Button btnModifier = findViewById(R.id.btn_maj_dossier_existant);
        Button btnVoirTout = findViewById(R.id.btn_voir_tous_dossier);
        Button btnRetour = findViewById(R.id.btn_retour_menu);

        btnNouveau.setOnClickListener(v -> {
            Intent intent = new Intent(this, SecretaryPatientManagementActivity.class);
            startActivity(intent);
        });

        btnModifier.setOnClickListener(v -> {
            Intent intent = new Intent(this, SecretaryPatientManagementActivity.class);
            startActivity(intent);
        });

        btnVoirTout.setOnClickListener(v -> {
            Intent intent = new Intent(this, SecretaryPatientManagementActivity.class);
            startActivity(intent);
        });

        btnRetour.setOnClickListener(v -> finish());
    }
}