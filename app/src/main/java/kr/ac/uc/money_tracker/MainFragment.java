package kr.ac.uc.money_tracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MainFragment extends Fragment {

    private TextView textUsage;
    private ProgressBar progressUsage;
    private ImageView imageCharacter;

    private int monthlyLimit = 1000000; // 임시 값 (예: 100만원)
    private int totalSpent = 320000;   // 임시 소비합계 (예: 32만원)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        textUsage = view.findViewById(R.id.textUsage);
        progressUsage = view.findViewById(R.id.progressUsage);
        imageCharacter = view.findViewById(R.id.imageCharacter);

        updateUI();

        return view;
    }

    private void updateUI() {
        int percent = (int) ((totalSpent / (float) monthlyLimit) * 100);

        // 사용량 텍스트
        textUsage.setText("5월 현재 사용량: " + percent + "%");

        // 진행바
        progressUsage.setProgress(percent);

        // 캐릭터 이미지 변경
        if (percent > 100) {
            imageCharacter.setImageResource(R.drawable.character_poor);
        } else if (percent > 70) {
            //imageCharacter.setImageResource(R.drawable.character_normal);
        } else {
            imageCharacter.setImageResource(R.drawable.character_rich);
        }
    }
}
