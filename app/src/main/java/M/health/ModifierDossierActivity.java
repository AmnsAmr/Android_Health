package M.health;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ModifierDossierActivity extends AppCompatActivity {

    private EditText etIdSearch, etAddress, etPhone, etMutuelle;
    private Button btnLoad, btnSave, btnRetourMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modifier_dossier);

        etIdSearch = findViewById(R.id.et_id_search);
        etAddress = findViewById(R.id.et_edit_address);
        etPhone = findViewById(R.id.et_edit_phone);
        etMutuelle = findViewById(R.id.et_edit_mutuelle);

        btnLoad = findViewById(R.id.btn_load_data);
        btnSave = findViewById(R.id.btn_save_changes);
        btnRetourMenu = findViewById(R.id.btn_retour_menu);

        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = etIdSearch.getText().toString().trim();
                if (!id.isEmpty()) {
                    Toast.makeText(ModifierDossierActivity.this,
                            "Données du patient " + id + " chargées", Toast.LENGTH_SHORT).show();

                    etAddress.setText("123 Rue de la Santé");
                    etPhone.setText("0612345678");
                    etMutuelle.setText("Ma Mutuelle Santé");
                } else {
                    etIdSearch.setError("Entrez un ID valide");
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String adresse = etAddress.getText().toString();
                String tel = etPhone.getText().toString();

                if (adresse.isEmpty() || tel.isEmpty()) {
                    Toast.makeText(ModifierDossierActivity.this,
                            "Veuillez remplir les champs obligatoires", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ModifierDossierActivity.this,
                            "Dossier mis à jour avec succès !", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });

      btnRetourMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}