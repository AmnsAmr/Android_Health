package M.health;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class TransmissionUrgentActivity extends AppCompatActivity {

    private Spinner spinnerDestinataire;
    private EditText etObjet, etMessage;
    private Button btnEnvoyer, btnRetour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transmission_urgent);

        spinnerDestinataire = findViewById(R.id.spinner_dest_medecin);
        etObjet = findViewById(R.id.et_objet_urgent);
        etMessage = findViewById(R.id.et_corps_message);
        btnEnvoyer = findViewById(R.id.btn_envoyer_urgent);
        btnRetour = findViewById(R.id.btn_retour_menu);

        List<String> medecins = new ArrayList<>();
        medecins.add("Choisir le destinataire...");
        medecins.add("Dr. Ahmed (Urgence Cardio)");
        medecins.add("Dr. Sarah (Pédiatrie)");
        medecins.add("Dr. Martin (Généraliste)");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                medecins
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDestinataire.setAdapter(adapter);

        btnEnvoyer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                envoyerAlerte();
            }
        });

        btnRetour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void envoyerAlerte() {
        String objet = etObjet.getText().toString().trim();
        String message = etMessage.getText().toString().trim();
        int medecinPosition = spinnerDestinataire.getSelectedItemPosition();

        if (medecinPosition == 0) {
            Toast.makeText(this, "Veuillez choisir un médecin destinataire", Toast.LENGTH_SHORT).show();
        } else if (objet.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "L'objet et le message sont obligatoires", Toast.LENGTH_SHORT).show();
        } else {
            String destinataire = spinnerDestinataire.getSelectedItem().toString();
            String alerte = "ALERTE ENVOYÉE à " + destinataire;

            Toast.makeText(this, alerte, Toast.LENGTH_LONG).show();

            finish();
        }
    }
}