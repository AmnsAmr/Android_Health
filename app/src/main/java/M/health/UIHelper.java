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
        ImageView btnSignOut = headerView.findViewById(R.id.btnSignOut);
        ImageView ivUserAvatar = headerView.findViewById(R.id.ivUserAvatar);

        if (tvUserName != null) {
            tvUserName.setText(currentUser.fullName);
        }
        
        if (tvUserRole != null) {
            tvUserRole.setText(getRoleDisplay(currentUser.role));
        }
        
        if (tvUserInfo != null) {
            tvUserInfo.setVisibility(View.GONE);
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(activity, SettingsActivity.class);
                activity.startActivity(intent);
            });
        }

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
                authManager.logout();
                Intent intent = new Intent(activity, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                activity.startActivity(intent);
                activity.finish();
            });
        }

        // Set user avatar based on role
        if (ivUserAvatar != null) {
            int avatarRes = getAvatarForRole(currentUser.role);
            ivUserAvatar.setImageResource(avatarRes);
        }
    }

    public static void setupHeader(Activity activity, View headerView, String title, 
                                 View.OnClickListener backClickListener, 
                                 View.OnClickListener actionClickListener) {
        // This method is deprecated - use setupUserProfileHeader instead
    }

    public static void setupHeader(Activity activity, View headerView, String title) {
        setupHeader(activity, headerView, title, v -> activity.finish(), null);
    }

    public static void setupHeaderWithSignOut(Activity activity, View headerView, String title, AuthManager authManager) {
        // This method is deprecated - use setupUserProfileHeader instead
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

    private static int getAvatarForRole(String role) {
        switch (role) {
            case "admin": return R.drawable.avatar_admin;
            case "doctor": return R.drawable.avatar_doctor;
            case "patient": return R.drawable.avatar_patient;
            case "secretary": return R.drawable.avatar_secretary;
            default: return R.drawable.ic_header_logo;
        }
    }

    public static String formatLastLogin(long timestamp) {
        if (timestamp <= 0) return "Jamais connecté";
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE);
        return sdf.format(new Date(timestamp));
    }
}