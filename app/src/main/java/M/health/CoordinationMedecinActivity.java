package M.health;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class CoordinationMedecinActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coordination_medecin);

        Button btnPlanning = findViewById(R.id.btn_consulter_planning_medecin);
        Button btnDemandes = findViewById(R.id.btn_transmettre_demandes_rdv);
        Button btnUrgences = findViewById(R.id.btn_transmettre_messages_urgents);
        Button btnRetour = findViewById(R.id.btn_retour_menu);

        btnPlanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CoordinationMedecinActivity.this, ConsultationPlanningActivity.class);
                startActivity(intent);
            }
        });

        btnUrgences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CoordinationMedecinActivity.this, TransmissionUrgentActivity.class);
                startActivity(intent);
            }
        });

        btnDemandes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CoordinationMedecinActivity.this,
                        "Demande de RDV transmise au médecin", Toast.LENGTH_SHORT).show();
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