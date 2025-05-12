package kr.ac.uc.money_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.io.IOException;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private Button buttonEmailLogin, buttonGoogleLogin, buttonSignUp;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        buttonEmailLogin = findViewById(R.id.buttonEmailLogin);
        buttonGoogleLogin = findViewById(R.id.buttonGoogleLogin);
        buttonSignUp = findViewById(R.id.buttonSignUp);

        auth = FirebaseAuth.getInstance();

        // Google 로그인 옵션 설정
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // 이메일 로그인 버튼 → 이메일 로그인 전용 화면으로 이동
        buttonEmailLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, EmailLoginActivity.class);
            startActivity(intent);
        });

        // 구글 로그인 버튼
        buttonGoogleLogin.setOnClickListener(v -> googleLogin());

        // 회원가입 화면으로 이동
        buttonSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void googleLogin() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                    .addOnSuccessListener(account -> firebaseAuthWithGoogle(account))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "구글 로그인 실패", Toast.LENGTH_SHORT).show();
                        Log.e("LoginActivity", "구글 로그인 실패", e);
                    });
        }
    }

    private void firebaseAuthWithGoogle(com.google.android.gms.auth.api.signin.GoogleSignInAccount account) {
        var credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            user.getIdToken(true)
                                    .addOnSuccessListener(result -> {
                                        String idToken = result.getToken();
                                        sendIdTokenToServer(idToken);
                                    })
                                    .addOnFailureListener(e -> Log.e("LoginActivity", "ID Token 실패", e));
                        }

                        Toast.makeText(this, "구글 로그인 성공!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, BudgetSetupActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "구글 로그인 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("LoginActivity", "구글 로그인 실패", task.getException());
                    }
                });
    }

    private void sendIdTokenToServer(String idToken) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://172.30.1.78:8090/users")
                .post(RequestBody.create("", null))
                .addHeader("Authorization", "Bearer " + idToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
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
