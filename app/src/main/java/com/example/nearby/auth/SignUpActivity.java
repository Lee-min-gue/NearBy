package com.example.nearby.auth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.example.nearby.main.MainPageActivity;
import com.example.nearby.databinding.ActivitySignUpBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    ActivitySignUpBinding binding;
    private FirebaseAuth mAuth;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "SignUpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        mAuth = FirebaseAuth.getInstance();
        setContentView(binding.getRoot());

        //취소 버튼
        binding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //가입하기 버튼
        binding.signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = binding.emailText.getText().toString().trim();
                final String pwd = binding.passwordText.getText().toString().trim();

                // 입력값 검증
                if (email.isEmpty() || pwd.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "이메일 또는 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 이메일 형식 검증
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(SignUpActivity.this, "올바른 이메일 형식을 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, pwd)
                        .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                //가입 성공
                                if (task.isSuccessful()) {
                                    addUserInfoToFirestore(email);
                                    Intent intent = new Intent(SignUpActivity.this, ProfileSettingActivity.class);
                                    startActivity(intent);
                                    finish();
                                    //가입 실패
                                } else {
                                    Toast.makeText(SignUpActivity.this, "등록 에러", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                        });
            }
        });
    }

    //회원가입 성공시 유저 정보(이메일,uid)를 데이터베이스(users)에 저장하는 함수
    private void addUserInfoToFirestore(String email) {
        FirebaseUser user = mAuth.getCurrentUser();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String fcmToken = task.getResult();

                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("email", email);
                        userMap.put("uid", user.getUid());  // 사용자 ID를 추가
                        userMap.put("nickname", user.getUid());  // 사용자 nickname을 ID로 설정
                        userMap.put("fcmToken", fcmToken);  // FCM 토큰을 추가

                        db.collection("users")
                                .document(user.getUid())
                                .set(userMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "User profile created for ID: " + user.getUid());
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error adding user profile", e);
                                    }
                                });
                    }
                });
    }

    // 배경화면 눌렀을 때 키보드 내려가는 기능
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        return super.dispatchTouchEvent(ev);
    }
}

