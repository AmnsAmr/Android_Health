package M.health;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManageAppointmentAdapter extends RecyclerView.Adapter<ManageAppointmentAdapter.ViewHolder> {

    private List<ManageAppointmentsActivity.AppointmentItem> appointments;
    private OnAppointmentActionListener listener;

    public interface OnAppointmentActionListener {
        void onAppointmentAction(ManageAppointmentsActivity.AppointmentItem appointment, String action);
    }

    public ManageAppointmentAdapter(List<ManageAppointmentsActivity.AppointmentItem> appointments,
                                    OnAppointmentActionListener listener) {
        this.appointments = appointments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment_manage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ManageAppointmentsActivity.AppointmentItem appointment = appointments.get(position);

        // Format date and time
        String formattedDateTime = formatDateTime(appointment.getDateTime());
        holder.tvDateTime.setText(formattedDateTime);

        holder.tvPatientName.setText(appointment.getPatientName());
        holder.tvPatientPhone.setText(appointment.getPatientPhone() != null ?
                appointment.getPatientPhone() : "N/A");

        String doctorInfo = "Dr. " + appointment.getDoctorName();
        if (appointment.getSpecialization() != null && !appointment.getSpecialization().isEmpty()) {
            doctorInfo += " - " + appointment.getSpecialization();
        }
        holder.tvDoctorInfo.setText(doctorInfo);

        // Status badge
        String status = appointment.getStatus().toUpperCase();
        holder.tvStatus.setText(status);

        switch (appointment.getStatus().toLowerCase()) {
            case "scheduled":
                holder.tvStatus.setBackgroundColor(Color.parseColor("#4CAF50"));
                break;
            case "cancelled":
                holder.tvStatus.setBackgroundColor(Color.parseColor("#9E9E9E"));
                break;
            case "completed":
                holder.tvStatus.setBackgroundColor(Color.parseColor("#2196F3"));
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppointmentAction(appointment, "view");
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    private String formatDateTime(String dateTime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.FRENCH);
            Date date = inputFormat.parse(dateTime);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateTime;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateTime;
        TextView tvPatientName;
        TextView tvPatientPhone;
        TextView tvDoctorInfo;
        TextView tvStatus;
        Button btnEdit;
        Button btnCancel;

        ViewHolder(View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tvAppointmentDateTime);
            tvPatientName = itemView.findViewById(R.id.tvAppointmentPatient);
            tvPatientPhone = itemView.findViewById(R.id.tvAppointmentPatient);
            tvDoctorInfo = itemView.findViewById(R.id.tvAppointmentDoctor);
            tvStatus = itemView.findViewById(R.id.tvAppointmentStatus);
            btnEdit = null;
            btnCancel = null;
        }
    }
}