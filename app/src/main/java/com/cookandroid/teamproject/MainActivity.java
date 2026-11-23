package com.cookandroid.teamproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String uid;

    private TextView tvStage,tvSpeech, tvXp, tvWeather;
    private android.widget.ImageView ivPet;

    private android.widget.ImageView ivPetItem;
    private ProgressBar pbXp;
    private EditText etAmount, etCategory;
    private RadioGroup rgType;


    private TextView tvHomeGoalName, tvHomeGoalStatus;
    private ProgressBar pbHomeGoal;
    private View layoutGoal;

    private long xp = 0;
    private long lastAccessAt = 0;

    private String equippedItem = "";
    private String currentGoalName = null;
    private long currentGoalTarget = 0;
    private String weatherText = "날씨 불러오는 중...";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            goLogin();
            return;
        }
        uid = auth.getCurrentUser().getUid();

        tvWeather = findViewById(R.id.tvWeather);
        tvStage = findViewById(R.id.tvStage);
        ivPet = findViewById(R.id.ivPet);
        ivPetItem = findViewById(R.id.ivPetItem);
        tvSpeech = findViewById(R.id.tvSpeech);
        tvXp = findViewById(R.id.tvXp);
        pbXp = findViewById(R.id.pbXp);
        etAmount = findViewById(R.id.etAmount);
        etCategory = findViewById(R.id.etCategory);
        rgType = findViewById(R.id.rgType);

        tvHomeGoalName = findViewById(R.id.tvHomeGoalName);
        tvHomeGoalStatus = findViewById(R.id.tvHomeGoalStatus);
        pbHomeGoal = findViewById(R.id.pbHomeGoal);
        layoutGoal = findViewById(R.id.layoutGoal);

        Button btnSave = findViewById(R.id.btnSave);
        Button btnGoal = findViewById(R.id.btnGoal);
        Button btnLogout = findViewById(R.id.btnLogout);

        Button btnCloset = findViewById(R.id.btnCloset);
        btnCloset.setOnClickListener(v -> startActivity(new Intent(this, ClosetActivity.class)));

        Button btnReport = findViewById(R.id.btnReport);
        btnReport.setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));
        Button btnHistory = findViewById(R.id.btnHistory);
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));

        btnSave.setOnClickListener(v -> saveRecord());
        btnGoal.setOnClickListener(v -> startActivity(new Intent(this, GoalActivity.class)));
        layoutGoal.setOnClickListener(v -> showGoalDialog());
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            goLogin();
        });

        loadUser();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (uid != null) {
            loadUser();
            loadGoalCard();
            tvWeather.setText(weatherText);
            loadWeather();
        }
    }


    private void loadUser() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long x = doc.getLong("pet.xp");
                        Long last = doc.getLong("lastAccessAt");
                        xp = (x == null) ? 0 : x;
                        lastAccessAt = (last == null) ? 0 : last;
                        String item = doc.getString("pet.item");
                        equippedItem = (item == null) ? "" : item;
                    }
                    updateUi(getAbsenceMessage());
                    db.collection("users").document(uid)
                            .update("lastAccessAt", System.currentTimeMillis());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


    private String getAbsenceMessage() {
        if (lastAccessAt == 0) return "오늘도 왔네!";
        long days = (System.currentTimeMillis() - lastAccessAt) / (1000L * 60 * 60 * 24);
        if (days >= 7) return "몸이 좀 안 좋아... 🤒";
        if (days >= 5) return "zzZ... 너무 졸려... 😪";
        if (days >= 3) return "어디 갔었어? 보고 싶었어... 🥺";
        return "오늘도 왔네!";
    }


    private void saveRecord() {
        String s = etAmount.getText().toString().trim();
        if (s.isEmpty()) {
            Toast.makeText(this, "금액을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        long amount = Long.parseLong(s);
        if (amount <= 0) return;

        String type;
        int checked = rgType.getCheckedRadioButtonId();
        if (checked == R.id.rbIncome) type = "income";
        else if (checked == R.id.rbExpense) type = "expense";
        else type = "saving";

        String category = etCategory.getText().toString().trim();
        if (category.isEmpty()) category = (type.equals("saving")) ? "저축" : "기타";

        Map<String, Object> record = new HashMap<>();
        record.put("type", type);
        record.put("amount", amount);
        record.put("category", category);
        record.put("date", System.currentTimeMillis());

        db.collection("users").document(uid).collection("records").add(record)
                .addOnSuccessListener(r -> loadGoalCard());

        String speech;
        if (type.equals("saving")) {
            xp += amount;
            db.collection("users").document(uid).update("pet.xp", xp)
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "XP 저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
            speech = amount >= 10000 ? "우와, 이렇게 많이?! 💖" : "냠냠! 고마워, 힘이 솟아! 🎉";
        } else if (type.equals("income")) {
            speech = "수입 기록 완료! 이제 저축도 해볼까? 💰";
        } else {
            speech = "기록해줘서 고마워! 다음엔 같이 아껴보자 🐾";
        }

        etAmount.setText("");
        etCategory.setText("");
        updateUi(speech);
    }


    private void updateUi(String speech) {
        String stage;
        int imageRes;
        long max;
        if (xp >= 1000000) {
            stage = "3단계 완전체 펫"; imageRes = R.drawable.pet3; max = 1000000;
        } else if (xp >= 100000) {
            stage = "2단계 성장 펫"; imageRes = R.drawable.pet2; max = 1000000;
        } else {
            stage = "1단계 아기 펫"; imageRes = R.drawable.pet1; max = 100000;
        }
        tvStage.setText(stage);
        ivPet.setImageResource(imageRes);
        View framePet = findViewById(R.id.frameHomePet);
        android.widget.LinearLayout.LayoutParams flp =
                (android.widget.LinearLayout.LayoutParams) framePet.getLayoutParams();
        float dd = getResources().getDisplayMetrics().density;
        flp.topMargin = (int) ((xp >= 1000000 ? 40 : 16) * dd);
        framePet.setLayoutParams(flp);

        int[] imgs = {
                R.drawable.ribbon_red, R.drawable.ribbon_pink, R.drawable.ribbon_green,
                R.drawable.red_gloves, R.drawable.pink_gloves, R.drawable.green_gloves,
                R.drawable.santa_hat, R.drawable.santa_hat_green, R.drawable.crown
        };
        String[] names = {"빨간 리본", "분홍 리본", "초록 리본",
                "빨간 장갑", "분홍 장갑", "초록 장갑",
                "산타 모자", "초록 산타 모자", "왕관"};
        int found = -1;
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(equippedItem)) found = i;
        }
        if (found >= 0) {
            ivPetItem.setImageResource(imgs[found]);
            float d = getResources().getDisplayMetrics().density;
            if (ClosetActivity.isGlove(found)) {
                ClosetActivity.applyGlovePos(ivPetItem, 1.15f, d);
            } else if (ClosetActivity.isHat(found)) {
                ClosetActivity.applyHatPos(ivPetItem, 1.15f, d);
            } else if (ClosetActivity.isCrown(found)) {
                ClosetActivity.applyCrownPos(ivPetItem, 1.15f, d);
            } else {
                android.widget.FrameLayout.LayoutParams lp =
                        (android.widget.FrameLayout.LayoutParams) ivPetItem.getLayoutParams();
                lp.width = (int) (60 * d);
                lp.height = (int) (140 * d);
                lp.topMargin = (int) (-46 * d);
                lp.setMarginStart((int) (30 * d));
                lp.gravity = android.view.Gravity.CENTER;
                ivPetItem.setLayoutParams(lp);
            }
            ivPetItem.setVisibility(View.VISIBLE);
        } else {
            ivPetItem.setVisibility(View.INVISIBLE);
        }

        tvSpeech.setText(speech);
        pbXp.setMax((int) max);
        pbXp.setProgress((int) Math.min(xp, max));
        tvXp.setText(xp + " / " + max + " XP");
    }


    private void loadGoalCard() {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String name = doc.getString("goal.name");
            Long target = doc.getLong("goal.target");
            if (name == null || target == null || target == 0) {
                currentGoalName = null;
                currentGoalTarget = 0;
                tvHomeGoalName.setText("🎯 목표를 설정해 주세요 👉");
                pbHomeGoal.setProgress(0);
                tvHomeGoalStatus.setText("");
                return;
            }
            currentGoalName = name;
            currentGoalTarget = target;
            db.collection("users").document(uid).collection("records")
                    .whereEqualTo("type", "saving").get()
                    .addOnSuccessListener(snap -> {
                        long sum = 0;
                        for (DocumentSnapshot d : snap) {
                            Long a = d.getLong("amount");
                            if (a != null) sum += a;
                        }
                        int percent = (int) Math.min(100, sum * 100 / target);
                        tvHomeGoalName.setText("🎯 " + name);
                        pbHomeGoal.setProgress(percent);
                        tvHomeGoalStatus.setText(sum + "원 / " + target + "원 (" + percent + "%) · 터치해서 수정");
                    });
        });
    }


    private void showGoalDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(60, 30, 60, 0);

        final EditText etName = new EditText(this);
        etName.setHint("목표 이름 (예: 여행 자금)");
        final EditText etTarget = new EditText(this);
        etTarget.setHint("목표 금액 (원)");
        etTarget.setInputType(InputType.TYPE_CLASS_NUMBER);

        if (currentGoalName != null) {
            etName.setText(currentGoalName);
            etTarget.setText(String.valueOf(currentGoalTarget));
        }

        box.addView(etName);
        box.addView(etTarget);

        new AlertDialog.Builder(this)
                .setTitle(currentGoalName == null ? "🎯 목표 설정" : "🎯 목표 수정")
                .setView(box)
                .setPositiveButton("저장", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String t = etTarget.getText().toString().trim();
                    if (name.isEmpty() || t.isEmpty()) {
                        Toast.makeText(this, "이름과 금액을 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    long target = Long.parseLong(t);
                    if (target <= 0) {
                        Toast.makeText(this, "금액은 0보다 커야 해요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> goal = new HashMap<>();
                    goal.put("name", name);
                    goal.put("target", target);
                    db.collection("users").document(uid).update("goal", goal)
                            .addOnSuccessListener(a -> loadGoalCard())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("취소", null)
                .show();
    }


    private void loadWeather() {
        WeatherHelper.fetch(new WeatherHelper.Callback() {
            @Override
            public void onResult(String temp, int pty) {
                String mission;
                String icon;
                if (pty == 1 || pty == 2 || pty == 5 || pty == 6) {
                    icon = "☔"; mission = "비 오는 날! 배달 대신 집밥 미션 도전!";
                } else if (pty == 3 || pty == 7) {
                    icon = "❄️"; mission = "눈 오는 날! 따뜻한 집 커피로 5,000원 아끼기!";
                } else {
                    double t = 0;
                    try { t = Double.parseDouble(temp); } catch (Exception ignored) {}
                    if (t >= 28) { icon = "☀️"; mission = "더운 날! 카페 대신 텀블러 음료 미션!"; }
                    else if (t <= 5) { icon = "🧣"; mission = "추운 날! 편의점 간식 하루 참기 미션!"; }
                    else { icon = "🌤️"; mission = "오늘은 대중교통·도보로 아껴볼까?"; }
                }
                weatherText = icon + " 서울 " + temp + "℃ · " + mission;
                runOnUiThread(() -> tvWeather.setText(weatherText));
            }

            @Override
            public void onError(String msg) {
                weatherText = "날씨 정보를 불러오지 못했어요 (" + msg + ")";
                runOnUiThread(() -> tvWeather.setText(weatherText));
            }
        });
    }
    private void goLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}