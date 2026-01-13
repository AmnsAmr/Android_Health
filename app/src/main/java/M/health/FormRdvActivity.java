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

public class FormRdvActivity extends AppCompatActivity {

    private EditText etNomPatient, etDate, etHeure;
    private Spinner spinnerMedecin;
    private Button btnEnregistrer, btnRetour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_rdv);

        etNomPatient = findViewById(R.id.et_nom_patient_rdv);
        etDate = findViewById(R.id.et_date_rdv);
        etHeure = findViewById(R.id.et_heure_rdv);
        spinnerMedecin = findViewById(R.id.spinner_medecin);
        btnEnregistrer = findViewById(R.id.btn_enregistrer_rdv);
        btnRetour = findViewById(R.id.btn_retour_menu); // Liaison du bouton retour

        List<String> listeMedecins = new ArrayList<>();
        listeMedecins.add("Choisir un médecin...");
        listeMedecins.add("Dr. Ahmed (Cardiologue)");
        listeMedecins.add("Dr. Sarah (Pédiatre)");
        listeMedecins.add("Dr. Martin (Généraliste)");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                listeMedecins
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMedecin.setAdapter(adapter);

        btnEnregistrer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enregistrerRendezVous();
            }
        });

        btnRetour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void enregistrerRendezVous() {
        String nom = etNomPatient.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String heure = etHeure.getText().toString().trim();

        if (nom.isEmpty() || date.isEmpty() || heure.isEmpty() || spinnerMedecin.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Veuillez remplir tous les champs et choisir un médecin", Toast.LENGTH_SHORT).show();
        } else {
            String medecin = spinnerMedecin.getSelectedItem().toString();
            String message = "RDV enregistré pour " + nom + " le " + date + " à " + heure + " avec " + medecin;
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            finish();
        }
    }
}