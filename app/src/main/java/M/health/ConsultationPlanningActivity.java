package M.health;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;
import android.content.Intent;

public class ConsultationPlanningActivity extends AppCompatActivity {

    private ListView listViewPlanning;
    private Button btnRetour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultation_planning);

        listViewPlanning = findViewById(R.id.list_planning);
        btnRetour = findViewById(R.id.btn_retour_planning);

        ArrayList<String> planningData = new ArrayList<>();
        planningData.add("Dr. Ahmed - Cardiologie - 09:00");
        planningData.add("Dr. Sarah - Pédiatrie - 10:30");
        planningData.add("Dr. Martin - Généraliste - 14:00");
        planningData.add("Dr. Laila - Dermatologie - 16:15");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                planningData
        );
        listViewPlanning.setAdapter(adapter);

        listViewPlanning.setOnItemClickListener((parent, view, position, id) -> {
            String selection = planningData.get(position);
            Toast.makeText(this, "Détails : " + selection, Toast.LENGTH_SHORT).show();
        });

        btnRetour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConsultationPlanningActivity.this, SecretaryDashboardActivity.class);
                startActivity(intent);

                finish();
            }
        });
    }
}
