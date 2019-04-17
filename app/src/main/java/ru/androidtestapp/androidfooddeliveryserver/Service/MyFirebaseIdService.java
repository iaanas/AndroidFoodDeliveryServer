package ru.androidtestapp.androidfooddeliveryserver.Service;

import android.content.Intent;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import ru.androidtestapp.androidfooddeliveryserver.Common.Common;
import ru.androidtestapp.androidfooddeliveryserver.Model.Token;

public class MyFirebaseIdService extends FirebaseInstanceIdService {
	
	@Override
	public void onTokenRefresh( ) {
		super.onTokenRefresh( );
		String  refreshedToken = FirebaseInstanceId.getInstance().getToken();
		updateToServer(refreshedToken);
	}
	
	private void updateToServer( String refreshedToken ) {
		FirebaseDatabase db = FirebaseDatabase.getInstance();
		DatabaseReference tokens = db.getReference("Tokens");
		Token data = new Token(refreshedToken, true);
		tokens.child( Common.currentUser.getPhone() ).setValue( data );
	}
}
