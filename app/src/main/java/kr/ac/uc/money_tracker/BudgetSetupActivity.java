package kr.ac.uc.money_tracker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.IOException;

import okhttp3.*;

public class BudgetSetupActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView imageProfile;
    private EditText editName, editAge, editLimit;
    private Button btnSave;

    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_setup);

        imageProfile = findViewById(R.id.imageProfile);
        editName = findViewById(R.id.editName);
        editAge = findViewById(R.id.editAge);
        editLimit = findViewById(R.id.editLimit);
        btnSave = findViewById(R.id.btnSave);

        imageProfile.setOnClickListener(v -> openImagePicker());

        btnSave.setOnClickListener(v -> saveUserData());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imageProfile.setImageURI(selectedImageUri);
        }
    }

    private void saveUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        user.getIdToken(true).addOnSuccessListener(result -> {
            String idToken = result.getToken(); // 🔐 Firebase ID Token

            String name = editName.getText().toString().trim();
            String age = editAge.getText().toString().trim();
            String limit = editLimit.getText().toString().trim();

            if (name.isEmpty() || age.isEmpty() || limit.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            OkHttpClient client = new OkHttpClient();

            RequestBody formBody = new FormBody.Builder()
                    .add("name", name)
                    .add("age", age)
                    .add("monthlyLimit", limit)
                    .build();

            Request request = new Request.Builder()
                    .url("http://192.168.31.172:8090/users") // 🔁 정확한 URL
                    .post(formBody)
                    .addHeader("Authorization", "Bearer " + idToken) // ✅ 인증 추가
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("BudgetSetup", "서버 요청 실패", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(BudgetSetupActivity.this, "설정 저장 완료!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(BudgetSetupActivity.this, MainActivity.class));
                            finish();
                        });
                    } else {
                        Log.e("BudgetSetup", "서버 오류: " + response.code());
                    }
                }
            });
        }).addOnFailureListener(e -> {
            Log.e("BudgetSetup", "ID Token 가져오기 실패", e);
        });
    }

}
