package M.health;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UIHelper {
    
    public static void setupUserProfileHeader(Activity activity, View headerView, AuthManager authManager) {
        if (headerView == null || authManager == null) return;
        
        AuthManager.User currentUser = authManager.getCurrentUser();
        if (currentUser == null) return;

        TextView tvUserName = headerView.findViewById(R.id.tvUserName);
        TextView tvUserRole = headerView.findViewById(R.id.tvUserRole);
        TextView tvUserInfo = headerView.findViewById(R.id.tvUserInfo);
        ImageView btnSettings = headerView.findViewById(R.id.btnSettings);
        ImageView ivUserAvatar = headerView.findViewById(R.id.ivUserAvatar);

        if (tvUserName != null) {
            tvUserName.setText(currentUser.fullName);
        }
        
        if (tvUserRole != null) {
            tvUserRole.setText(getRoleDisplay(currentUser.role));
        }
        
        if (tvUserInfo != null) {
            tvUserInfo.setText("ID: " + currentUser.id);
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(activity, SettingsActivity.class);
                activity.startActivity(intent);
            });
        }

        // Set user avatar based on role
        if (ivUserAvatar != null) {
            // You can customize avatar based on role or user preferences
            ivUserAvatar.setImageResource(R.mipmap.ic_launcher);
        }
    }

    public static void setupHeader(Activity activity, View headerView, String title, 
                                 View.OnClickListener backClickListener, 
                                 View.OnClickListener actionClickListener) {
        if (headerView == null) return;

        TextView tvHeaderTitle = headerView.findViewById(R.id.tvHeaderTitle);
        ImageView btnBack = headerView.findViewById(R.id.btnBack);
        ImageView btnHeaderAction = headerView.findViewById(R.id.btnHeaderAction);

        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText(title);
        }

        if (btnBack != null && backClickListener != null) {
            btnBack.setOnClickListener(backClickListener);
        }

        if (btnHeaderAction != null && actionClickListener != null) {
            btnHeaderAction.setOnClickListener(actionClickListener);
        }
    }

    public static void setupHeader(Activity activity, View headerView, String title) {
        setupHeader(activity, headerView, title, v -> activity.finish(), null);
    }

    private static String getRoleDisplay(String role) {
        switch (role) {
            case "admin": return "Administrateur";
            case "doctor": return "Médecin";
            case "patient": return "Patient";
            case "secretary": return "Secrétaire";
            default: return role;
        }
    }

    public static String formatLastLogin(long timestamp) {
        if (timestamp <= 0) return "Jamais connecté";
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE);
        return sdf.format(new Date(timestamp));
    }
}