package com.cookandroid.teamproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ClosetActivity extends AppCompatActivity {


    private final String[] NAMES = {"빨간 리본", "분홍 리본", "초록 리본",
            "빨간 장갑", "분홍 장갑", "초록 장갑",
            "산타 모자", "초록 산타 모자", "왕관"};
    private final int[] IMAGES = {
            R.drawable.ribbon_red, R.drawable.ribbon_pink, R.drawable.ribbon_green,
            R.drawable.red_gloves, R.drawable.pink_gloves, R.drawable.green_gloves,
            R.drawable.santa_hat, R.drawable.santa_hat_green, R.drawable.crown
    };
    private final long[] NEED_XP = {10000, 50000, 80000, 130000, 200000, 300000, 500000, 800000, 1000000};


    static final int[] GLOVE_POS = {140, 120, 28, 2};

    static final int[] HAT_POS = {75, 75, -55, 8};


    static boolean isHat(int idx) {
        return idx == 6 || idx == 7;
    }


    static void applyHatPos(ImageView iv, float scale, float density) {
        android.widget.FrameLayout.LayoutParams lp =
                (android.widget.FrameLayout.LayoutParams) iv.getLayoutParams();
        lp.width = (int) (HAT_POS[0] * scale * density);
        lp.height = (int) (HAT_POS[1] * scale * density);
        lp.topMargin = (int) (HAT_POS[2] * scale * density);
        lp.setMarginStart((int) (HAT_POS[3] * scale * density));
        lp.gravity = android.view.Gravity.CENTER;
        iv.setLayoutParams(lp);
    }

    static final long STAGE2_XP = 100000;
    static final long STAGE3_XP = 500000;

    static int petImageFor(long xp) {
        return (xp >= STAGE3_XP) ? R.drawable.pet3
                : (xp >= STAGE2_XP) ? R.drawable.pet2
                : R.drawable.pet1;
    }

    private FirebaseFirestore db;
    private String uid;
    private ImageView ivPet, ivItem;
    private TextView tvInfo;
    private GridView gvItems;
    private ItemAdapter adapter;

    private long xp = 0;
    private String equipped = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_closet);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ivPet = findViewById(R.id.ivClosetPet);
        ivItem = findViewById(R.id.ivClosetItem);
        tvInfo = findViewById(R.id.tvClosetInfo);
        gvItems = findViewById(R.id.gvItems);
        Button btnBack = findViewById(R.id.btnClosetBack);
        btnBack.setOnClickListener(v -> finish());

        adapter = new ItemAdapter();
        gvItems.setAdapter(adapter);
        gvItems.setOnItemClickListener((parent, view, position, id) -> onItemTouched(position));

        loadPet();
    }


    private void loadPet() {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            Long x = doc.getLong("pet.xp");
            String item = doc.getString("pet.item");
            xp = (x == null) ? 0 : x;
            equipped = (item == null) ? "" : item;
            refresh();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


    private void onItemTouched(int pos) {
        if (xp < NEED_XP[pos]) {
            Toast.makeText(this, "아직 잠겨 있어요! XP " + NEED_XP[pos] + " 이상 필요해요 🔒",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        equipped = equipped.equals(NAMES[pos]) ? "" : NAMES[pos];
        db.collection("users").document(uid).update("pet.item", equipped)
                .addOnSuccessListener(a -> refresh())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


    private int indexOfItem(String name) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) return i;
        }
        return -1;
    }


    static boolean isGlove(int idx) {
        return idx >= 3 && idx <= 5;
    }


    static void applyGlovePos(ImageView iv, float scale, float density) {
        android.widget.FrameLayout.LayoutParams lp =
                (android.widget.FrameLayout.LayoutParams) iv.getLayoutParams();
        lp.width = (int) (GLOVE_POS[0] * scale * density);
        lp.height = (int) (GLOVE_POS[1] * scale * density);
        lp.topMargin = (int) (GLOVE_POS[2] * scale * density);
        lp.setMarginStart((int) (GLOVE_POS[3] * scale * density));
        lp.gravity = android.view.Gravity.CENTER;
        iv.setLayoutParams(lp);
    }


    private void refresh() {
        int petImage = (xp >= 1000000) ? R.drawable.pet3
                : (xp >= 100000) ? R.drawable.pet2
                : R.drawable.pet1;
        ivPet.setImageResource(petImage);
        View framePet = findViewById(R.id.frameClosetPet);
        android.widget.LinearLayout.LayoutParams flp =
                (android.widget.LinearLayout.LayoutParams) framePet.getLayoutParams();
        float dd = getResources().getDisplayMetrics().density;
        flp.topMargin = (int) ((xp >= 1000000 ? 40 : 12) * dd);
        framePet.setLayoutParams(flp);

        int idx = indexOfItem(equipped);
        if (idx >= 0) {
            ivItem.setImageResource(IMAGES[idx]);
            float d = getResources().getDisplayMetrics().density;
            if (isGlove(idx)) {
                applyGlovePos(ivItem, 1.0f, d);
            } else if (isHat(idx)) {
                applyHatPos(ivItem, 1.0f, d);
            } else if (isCrown(idx)) {
                applyCrownPos(ivItem, 1.0f, d);
            } else {
                android.widget.FrameLayout.LayoutParams lp =
                        (android.widget.FrameLayout.LayoutParams) ivItem.getLayoutParams();
                lp.width = (int) (50 * d);
                lp.height = (int) (120 * d);
                lp.topMargin = (int) (-40 * d);
                lp.setMarginStart((int) (25 * d));
                lp.gravity = android.view.Gravity.CENTER;
                ivItem.setLayoutParams(lp);
            }
            ivItem.setVisibility(View.VISIBLE);
        } else {
            ivItem.setVisibility(View.INVISIBLE);
        }

        tvInfo.setText("현재 XP: " + xp + " · 아이템을 터치해 장착/해제");
        adapter.notifyDataSetChanged();
    }

    static final int[] CROWN_POS = {90, 80, -65, 0};


    static boolean isCrown(int idx) {
        return idx == 8;
    }


    static void applyCrownPos(ImageView iv, float scale, float density) {
        android.widget.FrameLayout.LayoutParams lp =
                (android.widget.FrameLayout.LayoutParams) iv.getLayoutParams();
        lp.width = (int) (CROWN_POS[0] * scale * density);
        lp.height = (int) (CROWN_POS[1] * scale * density);
        lp.topMargin = (int) (CROWN_POS[2] * scale * density);
        lp.setMarginStart((int) (CROWN_POS[3] * scale * density));
        lp.gravity = android.view.Gravity.CENTER;
        iv.setLayoutParams(lp);
    }

    private class ItemAdapter extends BaseAdapter {
        @Override public int getCount() { return NAMES.length; }
        @Override public Object getItem(int position) { return NAMES[position]; }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(ClosetActivity.this)
                        .inflate(R.layout.item_closet, parent, false);
            }
            ImageView ivImg = v.findViewById(R.id.ivItemImage);
            TextView tvName = v.findViewById(R.id.tvItemName);
            TextView tvStatus = v.findViewById(R.id.tvItemStatus);

            tvName.setText(NAMES[position]);
            ivImg.setImageResource(IMAGES[position]);

            if (xp < NEED_XP[position]) {
                ivImg.setAlpha(0.25f);
                tvStatus.setText("🔒 XP " + NEED_XP[position] + " 필요");
                v.setBackgroundColor(0xFFEEEEEE);
            } else if (equipped.equals(NAMES[position])) {
                ivImg.setAlpha(1f);
                tvStatus.setText("✅ 장착 중");
                v.setBackgroundColor(0xFFFFF3C4);
            } else {
                ivImg.setAlpha(1f);
                tvStatus.setText("장착 가능");
                v.setBackgroundColor(0xFFFFFFFF);
            }
            return v;
        }
    }
}