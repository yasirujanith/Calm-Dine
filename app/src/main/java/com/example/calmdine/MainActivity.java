package com.example.calmdine;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    ProgressBar loginProgress;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private static boolean s_persistenceInitialized = false;
    private boolean loginBool;

    EditText username;
    EditText password;
    TextView loginOrSignUpSection;
    Button btnLoginSignup;
    TextView txtSignUpLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loginBool = true;

        mAuth = FirebaseAuth.getInstance();

        username = findViewById(R.id.txtUserName);
        password = findViewById(R.id.txtPassword);
        loginOrSignUpSection = findViewById(R.id.txtLoginOrSignUpSection);
        btnLoginSignup = findViewById(R.id.btnLoginSignup);
        txtSignUpLogin = findViewById(R.id.txtSignUpLogin);
        setupUI(findViewById(R.id.mainActivityLinearLayout));

        mDatabase = FirebaseDatabase.getInstance();

        if (!s_persistenceInitialized) {
            mDatabase.setPersistenceEnabled(true);
            s_persistenceInitialized = true;
        }
        mDatabase.setLogLevel(Logger.Level.DEBUG);
        loginProgress = findViewById(R.id.progressBar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void onLogin(View view) {
        loginProgress.setVisibility(View.VISIBLE);
        mAuth.signOut();
//        Log.i("Value", String.valueOf(loginBool));
        if (loginBool) {
            mAuth.signInWithEmailAndPassword(username.getText().toString(), password.getText().toString()).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    loginProgress.setVisibility(View.INVISIBLE);
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                        startActivity(intent);
//                        Log.i("Ok", "Login Done");
                    } else {
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidUserException e) {
                            Toast.makeText(MainActivity.this, "Login Error: There is no corresponding user record.", Toast.LENGTH_LONG).show();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            Toast.makeText(MainActivity.this, "Login Error: The password is invalid.", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
        } else {
            mAuth.createUserWithEmailAndPassword(username.getText().toString(), password.getText().toString()).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                loginProgress.setVisibility(View.INVISIBLE);
                if (task.isSuccessful()) {
//                    Log.i("Ok", "Create User done");
                    onSignUpLogin(getWindow().getDecorView());
                    Toast.makeText(MainActivity.this, "User Added.", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthWeakPasswordException e) {
                        Toast.makeText(MainActivity.this, "Weak Password", Toast.LENGTH_LONG).show();
                    } catch (FirebaseAuthInvalidCredentialsException e) {
                        Toast.makeText(MainActivity.this, "Invalid Email", Toast.LENGTH_LONG).show();
                    } catch (FirebaseAuthUserCollisionException e) {
                        Toast.makeText(MainActivity.this, "User Exists", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
                }
            });
        }
    }

    public void onSignUpLogin(View view) {
        username.setText("");
        password.setText("");
        Log.i("signUp", "pressed");
        if(!loginBool) {
            loginOrSignUpSection.setText("Sign Up");
            btnLoginSignup.setText("Login");
            txtSignUpLogin.setText("Don't have an account?");
        } else {
            loginOrSignUpSection.setText("Login");
            btnLoginSignup.setText("Sign Up");
            txtSignUpLogin.setText("Have an account?");
        }
        loginBool = !loginBool;
    }

    public void setupUI(View view) {
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(MainActivity.this);
                    return false;
                }
            });
        }

        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService( Activity.INPUT_METHOD_SERVICE);
        if (activity.getCurrentFocus().getWindowToken() != null && activity.getCurrentFocus() != null && activity != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }
}
