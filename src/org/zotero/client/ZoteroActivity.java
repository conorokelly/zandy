package org.zotero.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ZoteroActivity extends Activity {
	private CommonsHttpOAuthConsumer httpOAuthConsumer;
	private OAuthProvider httpOAuthProvider;

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		Button oauthInit = (Button)findViewById(R.id.buttonInitOAuth);
        oauthInit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				startOAuth();
			}
		});
        
        updateBoxes();
    }
    
    /** Makes the OAuth call  */
    protected void startOAuth() {
    	try {
    		this.httpOAuthConsumer = new CommonsHttpOAuthConsumer(OAuthCredentials.CONSUMERKEY,
    													OAuthCredentials.CONSUMERSECRET);
    		this.httpOAuthProvider = new DefaultOAuthProvider("https://www.zotero.org/oauth/request",
    			"https://www.zotero.org/oauth/access", "https://www.zotero.org/oauth/authorize");
    	
    		String authUrl;
    		authUrl = httpOAuthProvider.retrieveRequestToken(httpOAuthConsumer, OAuthCredentials.CALLBACKURL);
    		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
    	} catch (OAuthMessageSignerException e) {
    		Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    	} catch (OAuthNotAuthorizedException e) {
    		Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    	} catch (OAuthExpectationFailedException e) {
    		Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    	} catch (OAuthCommunicationException e) {
    		Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    	}
    }
    
    protected void updateBoxes () {
		SharedPreferences settings = getBaseContext().getSharedPreferences("zotero_prefs", 0);
		String key = settings.getString("user_key", "no key");
		String id = settings.getString("user_id", "no user id");
		EditText keyBox = (EditText)findViewById(R.id.editText1);
		EditText idBox = (EditText)findViewById(R.id.editText2);
		keyBox.setText(key);
		idBox.setText(id);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
    	super.onNewIntent(intent);
    	
    	Uri uri = intent.getData();
    	
    	if (uri != null) {
    		String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
    		try {
				this.httpOAuthProvider.retrieveAccessToken(this.httpOAuthConsumer, verifier);
				String userKey = this.httpOAuthConsumer.getToken();
				String userSecret = this.httpOAuthConsumer.getTokenSecret();
				
				SharedPreferences settings = getBaseContext().getSharedPreferences("zotero_prefs", 0);
				SharedPreferences.Editor editor = settings.edit();
				// For Zotero, the key and secret are identical, it seems
				editor.putString("user_key", userKey);
				editor.putString("user_secret", userSecret);
				
				// Zotero also gives us the user ID, in the callback
				Pattern uid = Pattern.compile("userID=([0-9]+)");
				Matcher m = uid.matcher(uri.toString());
				try {
					String userID = m.group(1);
					editor.putString("user_id", userID);
				} catch (IllegalStateException e) {
					// no uid, it seems
				}				
				editor.putString("user_id", uri.toString());
				
				editor.commit();
				
				// Show this in the UI boxes
				updateBoxes();
			} catch (OAuthMessageSignerException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			} catch (OAuthNotAuthorizedException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			} catch (OAuthExpectationFailedException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			} catch (OAuthCommunicationException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
    	}
    }
}