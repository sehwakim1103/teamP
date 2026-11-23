package com.cookandroid.teamproject;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String uid;
    private TextView tvIncome, tvExpense, tvSaving, tvTopCategory, tvDiff, tvPetReview, tvReportTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        tvReportTitle = findViewById(R.id.tvReportTitle);
        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);
        tvSaving = findViewById(R.id.tvSaving);
        tvTopCategory = findViewById(R.id.tvTopCategory);
        tvDiff = findViewById(R.id.tvDiff);
        tvPetReview = findViewById(R.id.tvPetReview);

        Button btnIncome = findViewById(R.id.btnAddIncome);
        Button btnExpense = findViewById(R.id.btnAddExpense);
        Button btnBack = findViewById(R.id.btnReportBack);

        btnIncome.setOnClickListener(v -> showAddDialog("income"));
        btnExpense.setOnClickListener(v -> showAddDialog("expense"));
        btnBack.setOnClickListener(v -> finish());

        loadReport();
    }


    private void showAddDialog(final String type) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(60, 30, 60, 0);

        final EditText etAmount = new EditText(this);
        etAmount.setHint("금액 (원)");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        final EditText etCategory = new EditText(this);
        etCategory.setHint(type.equals("income") ? "항목 (예: 용돈, 알바)" : "항목 (예: 식비, 카페, 교통)");

        box.addView(etAmount);
        box.addView(etCategory);

        new AlertDialog.Builder(this)
                .setTitle(type.equals("income") ? "➕ 수입 기록" : "➖ 지출 기록")
                .setView(box)
                .setPositiveButton("저장", (dialog, which) -> {
                    String a = etAmount.getText().toString().trim();
                    if (a.isEmpty()) {
                        Toast.makeText(this, "금액을 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    long amount = Long.parseLong(a);
                    if (amount <= 0) return;
                    String cat = etCategory.getText().toString().trim();
                    if (cat.isEmpty()) cat = "기타";

                    Map<String, Object> record = new HashMap<>();
                    record.put("type", type);
                    record.put("amount", amount);
                    record.put("category", cat);
                    record.put("date", System.currentTimeMillis());

                    db.collection("users").document(uid).collection("records").add(record)
                            .addOnSuccessListener(r -> loadReport())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void loadReport() {

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long thisMonthStart = cal.getTimeInMillis();
        final int month = cal.get(Calendar.MONTH) + 1;
        cal.add(Calendar.MONTH, -1);
        final long lastMonthStart = cal.getTimeInMillis();

        tvReportTitle.setText("📊 " + month + "월 리포트");

        db.collection("users").document(uid).collection("records").get()
                .addOnSuccessListener(snap -> {
                    long income = 0, expense = 0, saving = 0, lastSaving = 0;
                    Map<String, Long> categoryMap = new HashMap<>();

                    for (DocumentSnapshot d : snap) {
                        String type = d.getString("type");
                        Long amount = d.getLong("amount");
                        Long date = d.getLong("date");
                        if (type == null || amount == null || date == null) continue;

                        if (date >= thisMonthStart) {
                            if (type.equals("income")) {
                                income += amount;
                            } else if (type.equals("saving")) {
                                saving += amount;
                            } else if (type.equals("expense")) {
                                expense += amount;
                                String cat = d.getString("category");
                                if (cat == null) cat = "기타";
                                Long old = categoryMap.get(cat);
                                categoryMap.put(cat, (old == null ? 0 : old) + amount);
                            }
                        } else if (date >= lastMonthStart && type.equals("saving")) {
                            lastSaving += amount;
                        }
                    }

                    String top = "-";
                    long topAmount = 0;
                    for (Map.Entry<String, Long> e : categoryMap.entrySet()) {
                        if (e.getValue() > topAmount) {
                            topAmount = e.getValue();
                            top = e.getKey();
                        }
                    }

                    tvIncome.setText("이번 달 총수입: " + income + "원");
                    tvExpense.setText("이번 달 총지출: " + expense + "원");
                    tvSaving.setText("이번 달 총저축: " + saving + "원");
                    tvTopCategory.setText("가장 많이 소비한 항목: "
                            + (topAmount == 0 ? "-" : top + " (" + topAmount + "원)"));

                    long diff = saving - lastSaving;
                    String diffText;
                    if (lastSaving == 0) diffText = "지난달 기록이 없어요";
                    else if (diff >= 0) diffText = "+" + diff + "원";
                    else diffText = diff + "원";
                    tvDiff.setText("지난달 대비 저축: " + diffText);

                    tvPetReview.setText("🐾 " + makeReview(saving, lastSaving, diff));
                })
                .addOnFailureListener(e ->
                        tvPetReview.setText("리포트를 불러오지 못했어요: " + e.getMessage()));
    }


    private String makeReview(long saving, long lastSaving, long diff) {
        if (saving == 0 && lastSaving == 0) {
            return "우리의 첫 한 달이 시작됐어! 오늘부터 하나씩 모아보자!";
        }
        if (lastSaving == 0) {
            return "우리의 첫 한 달이야! " + saving + "원이나 모았어, 앞으로가 더 기대돼!";
        }
        if (diff > 0) {
            return "이번 달에는 지난달보다 " + diff + "원을 더 모았어! 다음 달에도 함께 힘내보자!";
        }
        if (diff == 0) {
            return "꾸준히 모으고 있어! 이 페이스라면 목표도 금방이야!";
        }
        return "이번 달은 조금 힘들었지? 괜찮아, 다음 달엔 같이 다시 시작하자!";
    }
}