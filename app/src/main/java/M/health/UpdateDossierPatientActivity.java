package M.health;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class UpdateDossierPatientActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_dossier_patient);

        Button btnNouveau = findViewById(R.id.btn_nouveau_dossier);
        Button btnModifier = findViewById(R.id.btn_maj_dossier_existant);
        Button btnVoirTout = findViewById(R.id.btn_voir_tous_dossier);
        Button btnRetour = findViewById(R.id.btn_retour_menu); // Nouveau bouton

        btnNouveau.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UpdateDossierPatientActivity.this, ModifierDossierActivity.class);
                startActivity(intent);
            }
        });

        btnModifier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UpdateDossierPatientActivity.this, ModifierDossierActivity.class);
                startActivity(intent);
            }
        });

        btnVoirTout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UpdateDossierPatientActivity.this, RdvListActivity.class);
                startActivity(intent);
            }
        });

        btnRetour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


    }
}