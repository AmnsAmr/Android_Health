package M.health;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PatientListAdapter extends RecyclerView.Adapter<PatientListAdapter.ViewHolder> {

    private List<ViewPatientsActivity.PatientInfo> patients;
    private OnPatientClickListener listener;

    public interface OnPatientClickListener {
        void onPatientClick(ViewPatientsActivity.PatientInfo patient);
    }

    public PatientListAdapter(List<ViewPatientsActivity.PatientInfo> patients,
                              OnPatientClickListener listener) {
        this.patients = patients;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ViewPatientsActivity.PatientInfo patient = patients.get(position);

        holder.tvPatientName.setText(patient.getFullName());
        holder.tvPhone.setText(patient.getPhone() != null ? patient.getPhone() : "N/A");
        holder.tvEmail.setText(patient.getEmail() != null ? patient.getEmail() : "N/A");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPatientClick(patient);
            }
        });
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName;
        TextView tvPhone;
        TextView tvEmail;

        ViewHolder(View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvEmail = itemView.findViewById(R.id.tvEmail);
        }
    }
}