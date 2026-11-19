package com.cookandroid.teamproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnSignUp = findViewById(R.id.btnSignUp);

        btnLogin.setOnClickListener(v -> login());
        btnSignUp.setOnClickListener(v -> signUp());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() != null) {
            goHome();
        }
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String pw = etPassword.getText().toString().trim();
        if (email.isEmpty() || pw.isEmpty()) {
            Toast.makeText(this, "이메일과 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        auth.signInWithEmailAndPassword(email, pw)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        goHome();
                    } else {
                        Toast.makeText(this, "로그인 실패: 이메일/비밀번호를 확인하세요", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signUp() {
        String email = etEmail.getText().toString().trim();
        String pw = etPassword.getText().toString().trim();
        if (email.isEmpty() || pw.length() < 6) {
            Toast.makeText(this, "이메일과 6자 이상 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        auth.createUserWithEmailAndPassword(email, pw)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        createUserData(auth.getCurrentUser());
                        goHome();
                    } else {
                        Toast.makeText(this, "가입 실패: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createUserData(FirebaseUser user) {
        if (user == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("lastAccessAt", System.currentTimeMillis());

        Map<String, Object> pet = new HashMap<>();
        pet.put("name", "내 펫");
        pet.put("xp", 0);
        pet.put("stage", 1);
        userData.put("pet", pet);

        db.collection("users").document(user.getUid()).set(userData);
    }

    private void goHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}