package M.health;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private List<SecretaryDashboardActivity.Appointment> appointments;
    private OnAppointmentClickListener listener;

    public interface OnAppointmentClickListener {
        void onAppointmentClick(SecretaryDashboardActivity.Appointment appointment);
    }

    public AppointmentAdapter(List<SecretaryDashboardActivity.Appointment> appointments,
                              OnAppointmentClickListener listener) {
        this.appointments = appointments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment_secretary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SecretaryDashboardActivity.Appointment appointment = appointments.get(position);

        // Format time (extract time from datetime)
        String timeStr = extractTime(appointment.getDateTime());
        holder.tvAppointmentTime.setText(timeStr);

        holder.tvPatientName.setText(appointment.getPatientName());

        String doctorInfo = "Dr. " + appointment.getDoctorName();
        if (appointment.getSpecialization() != null && !appointment.getSpecialization().isEmpty()) {
            doctorInfo += " - " + appointment.getSpecialization();
        }
        holder.tvDoctorInfo.setText(doctorInfo);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppointmentClick(appointment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    private String extractTime(String dateTime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(dateTime);
            return outputFormat.format(date);
        } catch (ParseException e) {
            // If parsing fails, try to extract time directly
            if (dateTime.contains(" ")) {
                String[] parts = dateTime.split(" ");
                if (parts.length >= 2) {
                    return parts[1].substring(0, 5); // HH:mm
                }
            }
            return dateTime;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppointmentTime;
        TextView tvPatientName;
        TextView tvDoctorInfo;

        ViewHolder(View itemView) {
            super(itemView);
            tvAppointmentTime = itemView.findViewById(R.id.tvAppointmentTime);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvDoctorInfo = itemView.findViewById(R.id.tvDoctorInfo);
        }
    }
}