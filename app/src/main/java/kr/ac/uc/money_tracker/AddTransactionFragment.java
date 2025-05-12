package kr.ac.uc.money_tracker;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.*;

public class AddTransactionFragment extends Fragment {

    private EditText editDate, editAmount, editDescription;
    private MaterialAutoCompleteTextView dropdownCategory;
    private TextInputLayout layoutCategory;
    private String selectedEmotion = "😊";
    private String transactionType = "EXPENSE";
    private final OkHttpClient client = new OkHttpClient();
    private ArrayAdapter<CategoryItem> categoryAdapter;
    private List<CategoryItem> categories = new ArrayList<>();
    private static final String BASE_URL = "http://172.30.1.78:8090";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_transaction, container, false);

        editDate = view.findViewById(R.id.editDate);
        layoutCategory = view.findViewById(R.id.layoutCategory);
        dropdownCategory = view.findViewById(R.id.dropdownCategory);
        editAmount = view.findViewById(R.id.editAmount);
        editDescription = view.findViewById(R.id.editDescription);
        Button buttonIncome = view.findViewById(R.id.buttonIncome);
        Button buttonExpense = view.findViewById(R.id.buttonExpense);
        Button buttonSave = view.findViewById(R.id.buttonSave);

        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        dropdownCategory.setAdapter(categoryAdapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result ->
                    fetchCategories(transactionType, result.getToken()));
        }

        editDate.setOnClickListener(v -> showDatePicker());

        buttonIncome.setOnClickListener(v -> {
            transactionType = "INCOME";
            buttonIncome.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_blue_light));
            buttonExpense.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                currentUser.getIdToken(true).addOnSuccessListener(result ->
                        fetchCategories("INCOME", result.getToken()));
            }
        });

        buttonExpense.setOnClickListener(v -> {
            transactionType = "EXPENSE";
            buttonExpense.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_light));
            buttonIncome.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                currentUser.getIdToken(true).addOnSuccessListener(result ->
                        fetchCategories("EXPENSE", result.getToken()));
            }
        });

        MaterialButtonToggleGroup emotionGroup = view.findViewById(R.id.emotionToggleGroup);
        setupEmotionSelection(emotionGroup);

        buttonSave.setOnClickListener(v -> saveTransaction());

        return view;
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(getContext(), (view, year, month, day) -> {
            String date = String.format("%04d-%02d-%02d", year, month + 1, day);
            editDate.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void saveTransaction() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String date = editDate.getText().toString().trim();
        String category = dropdownCategory.getText().toString().trim();
        String amount = editAmount.getText().toString().trim();
        String description = editDescription.getText().toString().trim();

        if (date.isEmpty() || category.isEmpty() || amount.isEmpty()) {
            Toast.makeText(getContext(), "필수 항목을 모두 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        user.getIdToken(true).addOnSuccessListener(result -> {
            String idToken = result.getToken();

            RequestBody formBody = new FormBody.Builder()
                    .add("amount", amount)
                    .add("category", category)
                    .add("description", description)
                    .add("expenseDate", date)
                    .add("emotion", selectedEmotion)
                    .add("type", transactionType)
                    .build();

            Request request = new Request.Builder()
                    .url(BASE_URL + "/transactions")
                    .post(formBody)
                    .addHeader("Authorization", "Bearer " + idToken)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "저장 완료!", Toast.LENGTH_SHORT).show();
                            requireActivity().onBackPressed();
                        });
                    }
                }
            });
        });
    }

    private void fetchCategories(String type, String idToken) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/categories?type=" + type)
                .addHeader("Authorization", "Bearer " + idToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e("FETCH_CATEGORY", "요청 실패", e);
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String json = response.body().string();
                        Log.d("FETCH_CATEGORY", "서버 응답 데이터: " + json);

                        JSONArray array = new JSONArray(json);
                        categories.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            int id = obj.getInt("categoryId");
                            String name = obj.getString("name");
                            categories.add(new CategoryItem(id, name));
                        }

                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                categoryAdapter.clear();
                                categoryAdapter.addAll(categories);
                                categoryAdapter.notifyDataSetChanged();
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("FETCH_CATEGORY", "파싱 오류", e);
                    }
                } else {
                    Log.e("FETCH_CATEGORY", "서버 응답 실패: " + response.code());
                }
            }
        });
    }

    private void setupEmotionSelection(MaterialButtonToggleGroup group) {
        group.addOnButtonCheckedListener((toggleGroup, checkedId, isChecked) -> {
            if (isChecked) {
                MaterialButton selectedButton = group.findViewById(checkedId);
                selectedEmotion = selectedButton.getText().toString();
            }
        });
    }

    public static class CategoryItem {
        private final int id;
        private final String name;

        public CategoryItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() { return id; }
        public String getName() { return name; }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }
}