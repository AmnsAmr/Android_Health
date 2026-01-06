package M.health;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class RdvListActivity extends AppCompatActivity {

    private ListView lvRendezVous;
    private List<String> listePatients;
    private Button btnRetour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rdv_list);

        lvRendezVous = findViewById(R.id.lv_rendezvous);


        btnRetour = findViewById(R.id.btn_retour_planning);

        listePatients = new ArrayList<>();
        listePatients.add("Jean Dupont");
        listePatients.add("Marie Curie");
        listePatients.add("Thomas Edison");

        RdvAdapter adapter = new RdvAdapter();
        lvRendezVous.setAdapter(adapter);

        btnRetour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private class RdvAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return listePatients.size();
        }

        @Override
        public Object getItem(int position) {
            return listePatients.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(RdvListActivity.this)
                        .inflate(R.layout.activity_item_rdv, parent, false);
            }

            TextView tvNom = convertView.findViewById(R.id.tv_patient_nom);
            TextView tvDetails = convertView.findViewById(R.id.tv_rdv_details);
            Button btnConfirm = convertView.findViewById(R.id.btn_confirm);
            Button btnCancel = convertView.findViewById(R.id.btn_cancel);

            String nomPatient = listePatients.get(position);
            tvNom.setText(nomPatient);
            tvDetails.setText("RDV prévu à 14:00");

            btnConfirm.setOnClickListener(v ->
                    Toast.makeText(RdvListActivity.this, "Confirmé : " + nomPatient, Toast.LENGTH_SHORT).show()
            );

            btnCancel.setOnClickListener(v ->
                    Toast.makeText(RdvListActivity.this, "Annulé : " + nomPatient, Toast.LENGTH_SHORT).show()
            );

            return convertView;
        }
    }
}