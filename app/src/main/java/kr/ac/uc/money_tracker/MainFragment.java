package kr.ac.uc.money_tracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainFragment extends Fragment {

    private TextView textUsage;
    private ProgressBar progressUsage;
    private ImageView imageCharacter;
    private Button buttonAddTransaction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        textUsage = view.findViewById(R.id.textUsage);
        progressUsage = view.findViewById(R.id.progressUsage);
        imageCharacter = view.findViewById(R.id.imageCharacter);

        // 👇 수입/지출 추가하기 버튼 연결
        Button buttonAddTransaction = view.findViewById(R.id.buttonAddTransaction);
        buttonAddTransaction.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AddTransactionFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Firebase 사용자 정보 받아오기
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();
                fetchMonthlySummary(idToken, user.getUid());
            });
        }

        return view;
    }


    private void fetchMonthlySummary(String idToken, String firebaseUid) {
        String currentMonth = getCurrentMonth();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://172.30.1.78:8090/summary?firebaseUid=" + firebaseUid + "&month=" + currentMonth)
                .addHeader("Authorization", "Bearer " + idToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    try {
                        JSONObject obj = new JSONObject(json);
                        int limit = obj.getInt("monthlyLimit");
                        int spent = obj.getInt("totalSpent");

                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> updateUI(limit, spent));
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
    }



    private String getCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        return sdf.format(calendar.getTime());
    }

    private void updateUI(int monthlyLimit, int totalSpent) {
        int percent = (int) ((totalSpent / (float) monthlyLimit) * 100);
        textUsage.setText("이번 달 사용량: " + percent + "%");
        progressUsage.setProgress(percent);

        if (percent > 100) {
            imageCharacter.setImageResource(R.drawable.character_poor);
        } else if (percent > 70) {
            //imageCharacter.setImageResource(R.drawable.character_normal);
        } else {
            imageCharacter.setImageResource(R.drawable.character_rich);
        }
    }
}
