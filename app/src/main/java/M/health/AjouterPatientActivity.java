package M.health;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AjouterPatientActivity extends AppCompatActivity {

    private EditText etNom, etPrenom, etDate, etTel, etAdresse;
    private Button btnEnregistrer, btnRetour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajouter_patient);

        etNom = findViewById(R.id.et_nom);
        etPrenom = findViewById(R.id.et_prenom);
        etDate = findViewById(R.id.et_date_naissance);
        etTel = findViewById(R.id.et_telephone);
        etAdresse = findViewById(R.id.et_adresse);

        btnEnregistrer = findViewById(R.id.btn_enregistrer_patient);
        btnRetour = findViewById(R.id.btn_retour_menu);

        btnEnregistrer.setOnClickListener(v -> sauvegarderPatient());

        btnRetour.setOnClickListener(v -> {
            finish();
        });
    }

    private void sauvegarderPatient() {
        String nom = etNom.getText().toString().trim();
        String prenom = etPrenom.getText().toString().trim();

        if (nom.isEmpty() || prenom.isEmpty()) {
            Toast.makeText(this, "Le nom et le prénom sont obligatoires", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Patient " + nom + " ajouté avec succès !", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}