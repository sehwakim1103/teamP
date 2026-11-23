package com.cookandroid.teamproject;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoalActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String uid;

    private TextView tvGoalGuide;
    private ListView lvGoals;
    private EditText etGoalName, etGoalAmount;
    private Button btnSetGoal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        tvGoalGuide = findViewById(R.id.tvGoalGuide);
        lvGoals = findViewById(R.id.lvGoals);
        etGoalName = findViewById(R.id.etGoalName);
        etGoalAmount = findViewById(R.id.etGoalAmount);
        btnSetGoal = findViewById(R.id.btnSetGoal);
        Button btnBack = findViewById(R.id.btnBack);

        btnSetGoal.setOnClickListener(v -> addGoal());
        btnBack.setOnClickListener(v -> finish());

        loadGoals();
    }


    private void addGoal() {
        String name = etGoalName.getText().toString().trim();
        String amountStr = etGoalAmount.getText().toString().trim();
        if (name.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "이름과 금액을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        long target = Long.parseLong(amountStr);
        if (target <= 0) {
            Toast.makeText(this, "금액은 0보다 커야 해요", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> goal = new HashMap<>();
        goal.put("name", name);
        goal.put("target", target);
        goal.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(uid).collection("goals").add(goal)
                .addOnSuccessListener(r -> {
                    etGoalName.setText("");
                    etGoalAmount.setText("");
                    Toast.makeText(this, "목표가 추가됐어요! 🎯", Toast.LENGTH_SHORT).show();
                    loadGoals();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


    private void loadGoals() {
        db.collection("users").document(uid).collection("records")
                .whereEqualTo("type", "saving").get()
                .addOnSuccessListener(recSnap -> {
                    long sum = 0;
                    for (DocumentSnapshot d : recSnap) {
                        Long a = d.getLong("amount");
                        if (a != null) sum += a;
                    }
                    final long saved = sum;

                    db.collection("users").document(uid).collection("goals").get()
                            .addOnSuccessListener(goalSnap -> showGoals(goalSnap.getDocuments(), saved));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showGoals(List<DocumentSnapshot> docs, long saved) {
        List<String> lines = new ArrayList<>();
        for (DocumentSnapshot d : docs) {
            String name = d.getString("name");
            Long target = d.getLong("target");
            if (name == null || target == null || target == 0) continue;
            int percent = (int) Math.min(100, saved * 100 / target);
            String mark = percent >= 100 ? "🎉 " : "🎯 ";
            lines.add(mark + name + "\n" + saved + "원 / " + target + "원 (" + percent + "%)");
        }

        lvGoals.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, lines));

        if (lines.isEmpty()) {
            tvGoalGuide.setText("첫 목표를 설정해 주세요");
            btnSetGoal.setText("목표 설정하기");
            lvGoals.setVisibility(View.GONE);
        } else {
            tvGoalGuide.setText("목표 " + lines.size() + "개 진행 중 · 추가 목표를 설정할 수 있어요");
            btnSetGoal.setText("추가 목표 설정하기");
            lvGoals.setVisibility(View.VISIBLE);
        }
    }
}