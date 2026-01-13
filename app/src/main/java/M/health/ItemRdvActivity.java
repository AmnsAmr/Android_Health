package M.health;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ItemRdvActivity extends AppCompatActivity {

    private TextView tvNom, tvDetails;
    private Button btnConfirm, btnCancel, btnRetour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_rdv);

        tvNom = findViewById(R.id.tv_patient_nom);
        tvDetails = findViewById(R.id.tv_rdv_details);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnCancel = findViewById(R.id.btn_cancel);
        btnRetour = findViewById(R.id.btn_retour_menu);

        tvNom.setText("Jean Dupont");
        tvDetails.setText("15:45 - Dr. Martin (Salle 4)");

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ItemRdvActivity.this, "RDV de Jean Dupont confirmé", Toast.LENGTH_SHORT).show();
                btnConfirm.setEnabled(false);
                btnConfirm.setText("Confirmé");
                btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ItemRdvActivity.this, "RDV Annulé", Toast.LENGTH_SHORT).show();
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