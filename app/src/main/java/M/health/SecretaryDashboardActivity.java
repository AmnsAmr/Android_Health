package M.health;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class SecretaryDashboardActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secretary_dashboard);

        if (!authManager.hasPermission("secretary_manage_appointments")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup reusable user profile header
        View userProfileHeader = findViewById(R.id.userProfileHeader);
        UIHelper.setupUserProfileHeader(this, userProfileHeader, authManager);

        Button btnManageRdv = findViewById(R.id.btn_gerer_rdv);
        btnManageRdv.setOnClickListener(v -> {
            if (authManager.hasPermission("secretary_manage_appointments")) {
                Intent intent = new Intent(SecretaryDashboardActivity.this, ManageRdvActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnManagePatients = findViewById(R.id.btn_gerer_profils);
        btnManagePatients.setOnClickListener(v -> {
            if (authManager.hasPermission("secretary_manage_patients")) {
                Intent intent = new Intent(SecretaryDashboardActivity.this, UpdateDossierPatientActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnCoordination = findViewById(R.id.btn_coordination);
        btnCoordination.setOnClickListener(v -> {
            if (authManager.hasPermission("secretary_coordinate_doctors")) {
                Intent intent = new Intent(SecretaryDashboardActivity.this, DoctorScheduleActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnUrgentMessage = findViewById(R.id.btn_urgent_message);
        btnUrgentMessage.setOnClickListener(v -> {
            if (authManager.hasPermission("secretary_manage_appointments")) {
                Intent intent = new Intent(SecretaryDashboardActivity.this, TransmissionUrgentActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            }
        });
    }
}