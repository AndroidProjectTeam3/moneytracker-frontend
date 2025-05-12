package kr.ac.uc.money_tracker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;

import okhttp3.*;

public class BudgetSetupActivity extends AppCompatActivity {

    private ImageView imageProfile;
    private EditText editName, editAge, editLimit;
    private Button btnSave;
    private Uri selectedImageUri;

    private final OkHttpClient client = new OkHttpClient();

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    imageProfile.setImageURI(selectedImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_setup);
        FirebaseApp.initializeApp(this); // 생략해도 되지만 명시적 초기화 안전

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
        imagePickerLauncher.launch(intent);
    }

    private void saveUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = editName.getText().toString().trim();
        String age = editAge.getText().toString().trim();
        String limit = editLimit.getText().toString().trim();

        if (name.isEmpty() || age.isEmpty() || limit.isEmpty()) {
            Toast.makeText(this, "모든 항목을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        user.getIdToken(true).addOnSuccessListener(result -> {
            String idToken = result.getToken();

            if (selectedImageUri != null) {
                uploadImageToFirebase(user.getUid(), selectedImageUri, imageUrl -> {
                    if (imageUrl != null) {
                        sendUserDataToServer(idToken, name, age, limit, imageUrl);
                    } else {
                        Toast.makeText(this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                sendUserDataToServer(idToken, name, age, limit, null);
            }
        }).addOnFailureListener(e -> {
            Log.e("BudgetSetup", "ID Token 가져오기 실패", e);
        });
    }

    private void uploadImageToFirebase(String userId, Uri imageUri, OnImageUploadComplete listener) {
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("profileImages/" + userId + ".jpg");

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();

        UploadTask uploadTask = storageRef.putFile(imageUri, metadata);

        uploadTask
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    listener.onComplete(imageUrl);
                }))
                .addOnFailureListener(e -> {
                    Log.e("FirebaseUpload", "이미지 업로드 실패: " + e.getMessage(), e);
                    listener.onComplete(null);
                });
    }

    private void sendUserDataToServer(String idToken, String name, String age, String limit, @Nullable String imageUrl) {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("name", name)
                .addFormDataPart("age", age)
                .addFormDataPart("monthlyLimit", limit);

        if (imageUrl != null) {
            builder.addFormDataPart("profileImageUrl", imageUrl);
        }

        RequestBody requestBody = builder.build();

        Request request = new Request.Builder()
                .url("http://172.30.1.78:8090/users")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + idToken)
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
                    Log.e("BudgetSetup", "서버 응답 오류: " + response.code());
                }
            }
        });
    }

    public interface OnImageUploadComplete {
        void onComplete(@Nullable String imageUrl);
    }
}
