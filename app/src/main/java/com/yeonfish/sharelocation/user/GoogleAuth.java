package com.yeonfish.sharelocation.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.CancellationSignal;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetCustomCredentialOption;
import androidx.credentials.GetPasswordOption;
import androidx.credentials.PasswordCredential;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.yeonfish.sharelocation.MainActivity;
import com.yeonfish.sharelocation.R;
import com.yeonfish.sharelocation.util.HttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.Base64;

public class GoogleAuth {

    private Context context;

    private CredentialManager credentialManager;
    private SharedPreferences sp;

    public GoogleAuth(Context context, CredentialManager credentialManager) {
        this.context = context;
        this.credentialManager = credentialManager;
        this.sp = context.getSharedPreferences("sharelocation_user", 0);
    }

    public boolean checkLogin() {
        return sp.getString("id", null) == null;
    }

    public GoogleUser getUser() {
        GoogleUser userTmp = new GoogleUser();
        userTmp.setId(sp.getString("id", null));
        userTmp.setEmail(sp.getString("email", null));
        userTmp.setDisplayName(sp.getString("name", null));
        userTmp.setProfilePicture(sp.getString("profilePicture", null));

        return userTmp;
    }

    public void login() throws JSONException {
        SecureRandom random = new SecureRandom();
        byte[] challenge = new byte[32];
        random.nextBytes(challenge);
        String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challenge);
        JSONObject requestJson = new JSONObject();
        requestJson.put("challenge", challengeBase64);
        requestJson.put("rpId", "com.yeonfish.sharelocation");

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(new GetGoogleIdOption.Builder()
                        .setServerClientId(context.getString(R.string.web_client_id))
                        .setFilterByAuthorizedAccounts(false)
                        .setAutoSelectEnabled(true)
                        .build())
                .build();

        credentialManager.getCredentialAsync(context, request, new CancellationSignal(), context.getMainExecutor(), new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {

            @Override
            public void onResult(GetCredentialResponse getCredentialResponse) {
                Credential credential = getCredentialResponse.getCredential();

                if (credential instanceof PublicKeyCredential) {
                    String responseJson = ((PublicKeyCredential) credential).getAuthenticationResponseJson();
                    Log.d("Google login", responseJson);
                } else if (credential instanceof PasswordCredential) {
                    String username = ((PasswordCredential) credential).getId();
                    String password = ((PasswordCredential) credential).getPassword();
                    Log.d("Google login", username);
                    Log.d("Google login", password);
                } else if (credential instanceof CustomCredential) {
                    if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                        GoogleIdTokenCredential idTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                        String idToken = idTokenCredential.getIdToken();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    HttpUtil httpUtil = HttpUtil.getInstance();
                                    String result = httpUtil.post("https://oauth2.googleapis.com/tokeninfo", "id_token="+idToken, null);
                                    Log.d("Google Login", result);
                                    JSONObject resultJSON = new JSONObject(result);

                                    SharedPreferences sp = context.getApplicationContext().getSharedPreferences("sharelocation_user", 0);
                                    SharedPreferences.Editor spe = sp.edit();
                                    spe.putString("id", resultJSON.getString("sub"));
                                    spe.putString("email", idTokenCredential.getId());
                                    spe.putString("name", idTokenCredential.getDisplayName());
                                    spe.putString("profilePicture", String.valueOf(idTokenCredential.getProfilePictureUri()));
                                    spe.apply();

                                    Log.d("Google Login", sp.getString("id", "None"));

                                    new Handler(context.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((MainActivity)(context)).updateScreen();
                                        }
                                    });
                                }catch (Exception e) {}
                            }
                        }).start();
                    } else {
                        // Catch any unrecognized custom credential type here.
                        Log.e("Google Login", "Unexpected type of credential");
                    }
                } else {
                    Log.e("Google Login", "Unexpected type of credential");
                }
            }

            @Override
            public void onError(@NonNull GetCredentialException e) {
                Log.e("Google Login", "Error occured");
                Log.e("Google Login", e.getLocalizedMessage());
                e.printStackTrace();
            }
        });
    }

    public void logout() {
        context.getSharedPreferences("sharelocation_user", 0).edit().clear().apply();
    }
}
