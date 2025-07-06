package com.projectpivot.app;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    
    private TextView titleText;
    private TextView backButton;
    private TextView userEmailText;
    private TextView userNameText;
    private TextView memberSinceText;
    private CardView profileInfoCard;
    private CardView settingsCard;
    private CardView accountCard;
    private CardView signOutCard;
    private CardView deleteAccountCard;
    private LinearLayout profileContainer;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionTracker sessionTracker;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionTracker = SessionTracker.getInstance(this);
        
        initViews();
        setupClickListeners();
        loadUserData();
        // Production build - startup animations removed
    }
    
    private void initViews() {
        titleText = findViewById(R.id.titleText);
        backButton = findViewById(R.id.backButton);
        userEmailText = findViewById(R.id.userEmailText);
        userNameText = findViewById(R.id.userNameText);
        memberSinceText = findViewById(R.id.memberSinceText);
        profileInfoCard = findViewById(R.id.profileInfoCard);
        settingsCard = findViewById(R.id.settingsCard);
        accountCard = findViewById(R.id.accountCard);
        signOutCard = findViewById(R.id.signOutCard);
        deleteAccountCard = findViewById(R.id.deleteAccountCard);
        profileContainer = findViewById(R.id.profileContainer);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            finish();
            // Removed annoying transition animation
        });
        
        settingsCard.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            SoundManager.getInstance(this).hapticClick();
            Intent intent = new Intent(ProfileActivity.this, SettingsActivity.class);
            startActivity(intent);
            // Removed annoying transition animation
        });
        
        accountCard.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            showAccountManagementDialog();
        });
        
        signOutCard.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            showSignOutDialog();
        });
        
        deleteAccountCard.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            showDeleteAccountDialog();
        });
    }
    
    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Set email
            String email = currentUser.getEmail();
            userEmailText.setText(email != null ? email : "No email");
            
            // Set display name
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                userNameText.setText(displayName);
            } else {
                // Extract name from email
                String emailName = email != null ? email.split("@")[0] : "User";
                userNameText.setText(emailName.substring(0, 1).toUpperCase() + emailName.substring(1));
            }
            
            // Set member since date
            long creationTimestamp = currentUser.getMetadata().getCreationTimestamp();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault());
            String memberSince = "Member since " + sdf.format(new java.util.Date(creationTimestamp));
            memberSinceText.setText(memberSince);
            
            // User data loaded successfully
        } else {
            // No user signed in, redirect to sign-in
            redirectToSignIn();
        }
    }
    
    private void showAccountManagementDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Account Management")
                .setMessage("Choose an account action:")
                .setPositiveButton("Change Password", (dialog, which) -> {
                    changePassword();
                })
                .setNeutralButton("Update Profile", (dialog, which) -> {
                    updateProfile();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void changePassword() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            String email = user.getEmail();
            
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password reset email sent to " + email + 
                                    "\n\nCheck your email and follow the instructions.", 
                                    Toast.LENGTH_LONG).show();
                            // Password reset email sent
                        } else {
                            String errorMessage = "Failed to send password reset email";
                            Exception exception = task.getException();
                            
                            if (exception != null && exception.getMessage() != null) {
                                String exceptionMessage = exception.getMessage();
                                if (exceptionMessage.contains("network")) {
                                    errorMessage = "Network error. Check your internet connection";
                                } else if (exceptionMessage.contains("user-not-found")) {
                                    errorMessage = "Email address not found";
                                } else {
                                    errorMessage = "Error: " + exceptionMessage;
                                }
                            }
                            
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                            // Failed to send password reset email
                        }
                    });
        } else {
            Toast.makeText(this, "No email associated with this account", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateProfile() {
        // For now, just show a message. In a full app, you'd implement profile editing
        Toast.makeText(this, "Profile update coming soon!", Toast.LENGTH_SHORT).show();
    }
    
    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    signOutUser();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("⚠️ WARNING: This will permanently delete your account and all workout data. This action cannot be undone.\n\nAre you absolutely sure?")
                .setPositiveButton("DELETE", (dialog, which) -> {
                    deleteUserAccount();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void signOutUser() {
        // Sign out from Firebase
        mAuth.signOut();
        
        // Clear local session data
        SharedPreferences sessionPrefs = getSharedPreferences("SESSION_DATA", MODE_PRIVATE);
        sessionPrefs.edit().clear().apply();
        
        // Clear user-specific workout data
        sessionTracker.refreshUserSession();
        
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
        
        // Redirect to sign-in
        redirectToSignIn();
    }
    
    private void deleteUserAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            
            // First delete user data from Firestore
            db.collection("users").document(userId)
                    .delete()
                    .addOnCompleteListener(firestoreTask -> {
                        if (firestoreTask.isSuccessful()) {
                            // User data deleted from Firestore
                        } else {
                            // Failed to delete user data from Firestore
                        }
                        
                        // Then delete the Firebase Auth account
                        user.delete()
                                .addOnCompleteListener(authTask -> {
                                    if (authTask.isSuccessful()) {
                                        // Clear all local data
                                        clearAllLocalData();
                                        
                                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                        redirectToSignIn();
                                    } else {
                                        String errorMessage = "Failed to delete account";
                                        Exception exception = authTask.getException();
                                        
                                        if (exception != null) {
                                            String exceptionMessage = exception.getMessage();
                                            if (exceptionMessage != null) {
                                                if (exceptionMessage.contains("recent")) {
                                                    errorMessage = "Please sign out and sign in again, then try deleting your account";
                                                } else if (exceptionMessage.contains("network")) {
                                                    errorMessage = "Network error. Check your internet connection";
                                                } else {
                                                    errorMessage = "Error: " + exceptionMessage;
                                                }
                                            }
                                        }
                                        
                                        // Failed to delete account
                                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                                    }
                                });
                    });
        } else {
            Toast.makeText(this, "No user signed in", Toast.LENGTH_SHORT).show();
            redirectToSignIn();
        }
    }
    
    private void clearAllLocalData() {
        // Clear session data
        SharedPreferences sessionPrefs = getSharedPreferences("SESSION_DATA", MODE_PRIVATE);
        sessionPrefs.edit().clear().apply();
        
        // Clear app settings
        SharedPreferences appPrefs = getSharedPreferences("PROJECT_PIVOT_SETTINGS", MODE_PRIVATE);
        appPrefs.edit().clear().apply();
        
        // Clear workout data
        sessionTracker.refreshUserSession();
        
        // All local data cleared
    }
    
    private void redirectToSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    // Production build - startup animations removed
    
    // Production build - card animations removed
    
    // Production build - press animations removed
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
} 