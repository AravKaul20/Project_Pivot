package com.projectpivot.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private EditText emailEditText, passwordEditText;
    private Button signInButton, signUpButton, googleSignInButton;
    private TextView toggleModeText, modeTitle, modeSubtitle;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db;

    private boolean isSignUpMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign In
        String webClientId = getString(R.string.default_web_client_id);
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signInButton = findViewById(R.id.signInButton);
        signUpButton = findViewById(R.id.signUpButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        toggleModeText = findViewById(R.id.toggleModeText);
        modeTitle = findViewById(R.id.modeTitle);
        modeSubtitle = findViewById(R.id.modeSubtitle);
        progressBar = findViewById(R.id.progressBar);

        updateUI();
    }

    private void setupClickListeners() {
        signInButton.setOnClickListener(v -> {
            if (isSignUpMode) {
                signUpWithEmail();
            } else {
                signInWithEmail();
            }
        });

        signUpButton.setOnClickListener(v -> {
            isSignUpMode = !isSignUpMode;
            updateUI();
        });

        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        toggleModeText.setOnClickListener(v -> {
            isSignUpMode = !isSignUpMode;
            updateUI();
        });
    }

    private void updateUI() {
        if (isSignUpMode) {
            // Sign Up Mode
            modeTitle.setText("Create Account");
            modeSubtitle.setText("Join thousands of boxers improving their skills");
            signInButton.setText("Create Account");
            signUpButton.setText("Sign In Instead");
            toggleModeText.setText("Already have an account? Sign in");
        } else {
            // Sign In Mode
            modeTitle.setText("Welcome Back");
            modeSubtitle.setText("Sign in to continue your boxing journey");
            signInButton.setText("Sign In");
            signUpButton.setText("Create Account");
            toggleModeText.setText("Don't have an account? Sign up");
        }
        
        // Clear any previous errors
        emailEditText.setError(null);
        passwordEditText.setError(null);
    }

    private void signInWithEmail() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            navigateToDashboard();
                        } else {
                            Toast.makeText(SignInActivity.this, "Authentication failed: " + 
                                    task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void signUpWithEmail() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            createUserProfile(user);
                        } else {
                            Toast.makeText(SignInActivity.this, "Registration failed: " + 
                                    task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                String errorMessage = "Google sign in failed";
                switch (e.getStatusCode()) {
                    case 10:
                        errorMessage = "Developer error - check SHA-1 fingerprint";
                        break;
                    case 12500:
                        errorMessage = "Sign in cancelled";
                        break;
                    case 7:
                        errorMessage = "Network error - check internet connection";
                        break;
                    case 12502:
                        errorMessage = "Sign in currently disabled for this app";
                        break;
                    default:
                        errorMessage = "Google sign in failed (Code: " + e.getStatusCode() + ")";
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        setLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            createUserProfile(user);
                        } else {
                            Toast.makeText(SignInActivity.this, "Authentication failed.", 
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void createUserProfile(FirebaseUser user) {
        if (user == null) return;

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("email", user.getEmail());
        userProfile.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "");
        userProfile.put("createdAt", System.currentTimeMillis());
        userProfile.put("totalSessions", 0);
        userProfile.put("totalWorkoutTime", 0);

        db.collection("users").document(user.getUid())
                .set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    navigateToDashboard();
                })
                .addOnFailureListener(e -> {
                    // Still navigate to dashboard even if profile creation fails
                    navigateToDashboard();
                });
    }

    private void navigateToDashboard() {
        // Trigger session sync for the newly signed-in user
        SessionTracker sessionTracker = SessionTracker.getInstance(this);
        sessionTracker.refreshUserSession();
        
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        signInButton.setEnabled(!loading);
        signUpButton.setEnabled(!loading);
        googleSignInButton.setEnabled(!loading);
        emailEditText.setEnabled(!loading);
        passwordEditText.setEnabled(!loading);
    }
} 