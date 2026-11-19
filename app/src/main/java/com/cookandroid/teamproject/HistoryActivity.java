package com.cookandroid.teamproject;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private ListView lvHistory;
    private TextView tvTotal, tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        lvHistory = findViewById(R.id.lvHistory);
        tvTotal = findViewById(R.id.tvHistoryTotal);
        tvEmpty = findViewById(R.id.tvHistoryEmpty);
        Button btnBack = findViewById(R.id.btnHistoryBack);
        btnBack.setOnClickListener(v -> finish());

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        loadHistory(uid);
    }

    private void loadHistory(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("records").whereEqualTo("type", "saving").get()
                .addOnSuccessListener(snap -> {
                    // 최신순 정렬을 위해 (날짜, 금액) 목록으로 모은 뒤 정렬
                    List<long[]> rows = new ArrayList<>();
                    long total = 0;
                    for (DocumentSnapshot d : snap) {
                        Long amount = d.getLong("amount");
                        Long date = d.getLong("date");
                        if (amount == null || date == null) continue;
                        rows.add(new long[]{date, amount});
                        total += amount;
                    }
                    Collections.sort(rows, new Comparator<long[]>() {
                        @Override
                        public int compare(long[] a, long[] b) {
                            return Long.compare(b[0], a[0]);   // 최신이 위로
                        }
                    });

                    SimpleDateFormat f = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);
                    List<String> lines = new ArrayList<>();
                    for (long[] r : rows) {
                        lines.add(f.format(new Date(r[0])) + "    +" + r[1] + "원");
                    }

                    tvTotal.setText("총 저축: " + total + "원 (" + rows.size() + "회)");
                    lvHistory.setAdapter(new ArrayAdapter<>(this,
                            android.R.layout.simple_list_item_1, lines));
                    tvEmpty.setVisibility(rows.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                })
                .addOnFailureListener(e -> tvTotal.setText("불러오기 실패: " + e.getMessage()));
    }
}