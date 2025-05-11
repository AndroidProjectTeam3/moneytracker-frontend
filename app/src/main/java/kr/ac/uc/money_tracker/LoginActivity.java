package kr.ac.uc.money_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Response;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class LoginActivity extends AppCompatActivity {

    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    private EditText editTextEmail, editTextPassword;
    private Button buttonEmailLogin, buttonGoogleLogin, buttonSignUp;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonEmailLogin = findViewById(R.id.buttonEmailLogin);
        buttonGoogleLogin = findViewById(R.id.buttonGoogleLogin);
        buttonSignUp = findViewById(R.id.buttonSignUp);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))  // 🔥 여기 중요!
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);


        auth = FirebaseAuth.getInstance();

        buttonEmailLogin.setOnClickListener(v -> {
            // 이메일 로그인 처리
            emailLogin();
        });

        buttonGoogleLogin.setOnClickListener(v -> {
            // 구글 로그인 처리
            googleLogin();
        });



        buttonSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            com.google.android.gms.tasks.Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount> task =
                    com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data);

            if (task.isSuccessful()) {
                com.google.android.gms.auth.api.signin.GoogleSignInAccount account = task.getResult();
                firebaseAuthWithGoogle(account);
            } else {
                Toast.makeText(this, "구글 로그인 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(com.google.android.gms.auth.api.signin.GoogleSignInAccount account) {
        com.google.firebase.auth.AuthCredential credential =
                com.google.firebase.auth.GoogleAuthProvider.getCredential(account.getIdToken(), null);

        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "구글 로그인 성공!", Toast.LENGTH_SHORT).show();

                        // ✅ 여기서 FirebaseUser 가져오기
                        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                        if (user != null) {
                            user.getIdToken(true)
                                    .addOnCompleteListener(tokenTask -> {
                                        if (tokenTask.isSuccessful()) {
                                            String idToken = tokenTask.getResult().getToken();
                                            sendIdTokenToServer(idToken); // 🔐 서버 전송
                                        } else {
                                            Log.e("LoginActivity", "ID Token 가져오기 실패", tokenTask.getException());
                                        }
                                    });
                        }

                        // ✅ BudgetSetup 화면으로 이동
                        Intent intent = new Intent(LoginActivity.this, BudgetSetupActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "구글 로그인 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("LoginActivity", "구글 로그인 실패", task.getException());
                    }
                });
    }



    private void emailLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "이메일과 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "이메일 로그인 성공!", Toast.LENGTH_SHORT).show();
                        // 로그인 성공 → 홈(MainActivity)으로 이동
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "로그인 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("LoginActivity", "이메일 로그인 실패", task.getException());
                    }
                });
    }

    private void googleLogin() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void sendIdTokenToServer(String idToken) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://192.168.31.172:8090/users")
                .post(RequestBody.create("", null))
                .addHeader("Authorization", "Bearer " + idToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("LoginActivity", "서버 저장 성공");
                } else {
                    Log.e("LoginActivity", "서버 저장 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                Log.e("LoginActivity", "서버 요청 실패", e);
            }
        });
    }

}
