package M.health;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ManageRdvActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_rdv);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnCreer = findViewById(R.id.btn_creer_rdv);
        Button btnModifier = findViewById(R.id.btn_modifier_annuler_rdv);
        Button btnConfirmer = findViewById(R.id.btn_confirmer_rdv);
        Button btnConsulter = findViewById(R.id.btn_consulter_liste_rdv);
        Button btnRetour = findViewById(R.id.btn_retour_menu_rdv);


        btnCreer.setOnClickListener(v -> {
            startActivity(new Intent(this, FormRdvActivity.class));
        });

        btnModifier.setOnClickListener(v -> {
            startActivity(new Intent(this, RdvListActivity.class));
        });

        btnConfirmer.setOnClickListener(v -> {
            startActivity(new Intent(this, ItemRdvActivity.class));
        });

        btnConsulter.setOnClickListener(v -> {
            startActivity(new Intent(this, RdvListActivity.class));
        });

        btnRetour.setOnClickListener(v -> {
            finish();
        });
    }
}