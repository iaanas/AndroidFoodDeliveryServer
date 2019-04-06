package ru.androidtestapp.androidfooddeliveryserver;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import com.google.android.gms.location.LocationListener;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.androidtestapp.androidfooddeliveryserver.Common.Common;
import ru.androidtestapp.androidfooddeliveryserver.Common.DirectionJSONParser;
import ru.androidtestapp.androidfooddeliveryserver.Remote.IGeoCoordinates;

public class TreckingOrder extends FragmentActivity implements OnMapReadyCallback,
                                                               GoogleApiClient.ConnectionCallbacks,
                                                               GoogleApiClient.OnConnectionFailedListener,
                                                               LocationListener {
	
	private GoogleMap mMap;
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST =1000;
	private final static int LOCATION_PERMITION_REQUEST =1001;
	
	private Location mLastLocation;
	
	private GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;
	
	private static int UPDATE_INTERVAL=1000;
	private static int FATEST_INTERVAL=5000;
	private static int DISPLACEMENT=10;
	
	private IGeoCoordinates mService;
	
	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_trecking_order );
		
		mService = Common.getGeoCodeService();
		
		if( ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED
		&& ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
			requestRuntimePermission();
		} else {
			if(checkPlayServices()){
				buildGoogleApiClient();
				createLocationRequest();
			}
		}
		
		displayLocation();
		
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = ( SupportMapFragment ) getSupportFragmentManager( )
				.findFragmentById( R.id.map );
		mapFragment.getMapAsync( this );
	}
	
	private void displayLocation( ) {
		if( ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED
				&& ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
			requestRuntimePermission();
		} else {
			mLastLocation = LocationServices.FusedLocationApi.getLastLocation( mGoogleApiClient );
			if(mLastLocation != null){
				double latitude = mLastLocation.getLatitude();
				double longitude = mLastLocation.getLongitude();
				
				LatLng youtLocation = new LatLng( latitude, longitude );
				mMap.addMarker( new MarkerOptions().position( youtLocation ).title( "Your Location" ) );
				mMap.moveCamera( CameraUpdateFactory.newLatLng( youtLocation ) );
				mMap.animateCamera( CameraUpdateFactory.zoomTo( 17.0f ) );
				
				drawRoute(youtLocation, Common.currentRequest.getAddress());
				
			} else {
				Toast.makeText( this, "Couldn't get the location", Toast.LENGTH_SHORT ).show();
				Log.d("Debug", "My Location");
			}
		}
	}
	
	private void drawRoute( LatLng youtLocation , String address ) {
		mService.getGeoCode( address ).enqueue( new Callback < String >( ) {
			@Override
			public void onResponse( Call< String > call , Response< String > response ) {
				try{
					JSONObject jsonObject = new JSONObject( response.body().toString() );
					
					String lat = (( JSONArray ) jsonObject.get( "results" ))
							.getJSONObject( 0 )
							.getJSONObject( "geometry" )
							.getJSONObject( "location" )
							.get("lat").toString();
					
					String lng = (( JSONArray ) jsonObject.get( "results" ))
							.getJSONObject( 0 )
							.getJSONObject( "geometry" )
							.getJSONObject( "location" )
							.get("lng").toString();
					
					LatLng orderLocation = new LatLng( Double.parseDouble( lat ), Double.parseDouble( lng ) );
					Bitmap bitmap = BitmapFactory.decodeResource( getResources(), R.drawable.dostavka_i_oplata );
					bitmap = Common.scaleBitmap( bitmap, 70, 70 );
					
					MarkerOptions marker = new MarkerOptions().icon( BitmapDescriptorFactory.fromBitmap( bitmap ) )
							.title( "Order of " + Common.currentRequest.getPhone() )
							.position( orderLocation );
					
					mMap.addMarker( marker );
					
					mService.getDirections( youtLocation.latitude+" ,"+youtLocation.longitude,
							orderLocation.latitude+" , "+orderLocation.longitude)
							.enqueue( new Callback < String >( ) {
								@Override
								public void onResponse( Call < String > call , Response < String > response ) {
									new ParserTask().execute(response.body().toString());
								}
								
								@Override
								public void onFailure( Call < String > call , Throwable t ) {
								
								}
							} );
					
				} catch ( JSONException e ) {
					e.printStackTrace( );
				}
			}
			
			@Override
			public void onFailure( Call < String > call , Throwable t ) {
			
			}
		} );
	}
	
	private void createLocationRequest( ) {
		mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval( UPDATE_INTERVAL );
		mLocationRequest.setFastestInterval( FATEST_INTERVAL );
		mLocationRequest.setPriority( LocationRequest.PRIORITY_HIGH_ACCURACY );
		mLocationRequest.setSmallestDisplacement( DISPLACEMENT );
	}
	
	protected synchronized void buildGoogleApiClient( ) {
		mGoogleApiClient = new GoogleApiClient.Builder( this )
				.addConnectionCallbacks( this )
				.addOnConnectionFailedListener( this )
				.addApi( LocationServices.API ).build();
		
		mGoogleApiClient.connect();
	}
	
	private boolean checkPlayServices( ) {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable( this );
		if(resultCode != ConnectionResult.SUCCESS){
			if(GooglePlayServicesUtil.isUserRecoverableError( resultCode )){
				GooglePlayServicesUtil.getErrorDialog( resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST ).show();
			} else {
				Toast.makeText( this, "This device is not support", Toast.LENGTH_SHORT ).show();
				finish();
		    }
		    return false;
		}
		return true;
	}
	
	private void requestRuntimePermission( ) {
		ActivityCompat.requestPermissions( this, new String[] {
				Manifest.permission.ACCESS_COARSE_LOCATION,
				Manifest.permission.ACCESS_FINE_LOCATION
		}, LOCATION_PERMITION_REQUEST );
	}
	
	@Override
	public void onRequestPermissionsResult( int requestCode , @NonNull String[] permissions , @NonNull int[] grantResults ) {
		super.onRequestPermissionsResult( requestCode , permissions , grantResults );
		switch (requestCode){
			case LOCATION_PERMITION_REQUEST:
				if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
					if(checkPlayServices()){
						buildGoogleApiClient();
						createLocationRequest();
						
						displayLocation();
					}
				}
				break;
		}
	}
	
	@Override
	public void onMapReady( GoogleMap googleMap ) {
		mMap = googleMap;
		
		
	}
	
	@Override
	public void onConnected( @Nullable Bundle bundle ) {
		displayLocation();
		startLocationUpdates();
	
	}
	
	private void startLocationUpdates( ) {
		if( ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED
				&& ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
			return;
		}
		LocationServices.FusedLocationApi.requestLocationUpdates( mGoogleApiClient, mLocationRequest, ( com.google.android.gms.location.LocationListener ) this );
	}
	
	@Override
	public void onConnectionSuspended( int i ) {
		mGoogleApiClient.connect();
	}
	
	@Override
	public void onConnectionFailed( @NonNull ConnectionResult connectionResult ) {
	
	}
	
	@Override
	public void onLocationChanged( Location location ) {
		mLastLocation = location;
		displayLocation();
	}
	
	@Override
	protected void onResume( ) {
		super.onResume( );
		checkPlayServices();
	}
	
	@Override
	protected void onStart( ) {
		super.onStart( );
		if(mGoogleApiClient != null){
			mGoogleApiClient.connect();
		}
	}
	
	private class ParserTask extends AsyncTask<String, Integer, List<List< HashMap<String, String> >> > {
		ProgressDialog mDialog = new ProgressDialog( TreckingOrder.this );
		
		@Override
		protected void onPreExecute( ) {
			super.onPreExecute( );
			mDialog.setMessage( "Please waiting... " );
			mDialog.show();
			
		}
		
		@Override
		protected List < List < HashMap < String, String > > > doInBackground( String... strings ) {
			JSONObject jsonObject;
			List<List<HashMap<String, String>>> routes=null;
			
			try{
				jsonObject = new JSONObject( strings[0] );
				DirectionJSONParser parser = new DirectionJSONParser();
				
				routes = parser.parse( jsonObject );
				
			} catch ( JSONException e ) {
				e.printStackTrace( );
			}
			return routes;
		}
		
		@Override
		protected void onPostExecute( List < List < HashMap < String, String > > > lists ) {
			mDialog.dismiss();
			
			ArrayList points = null;
			PolylineOptions lineOptions = null;
			
			for(int i =0; i<lists.size(); i++){
				points = new ArrayList(  );
				lineOptions = new PolylineOptions();
				List<HashMap<String, String>> path = lists.get( i );
				
				for(int j = 0; j<path.size(); j++){
					HashMap<String, String> point = path.get( j );
					
					double lat = Double.parseDouble( point.get( "lat" ) );
					double lng = Double.parseDouble( point.get( "lng" ) );
					LatLng position = new LatLng( lat, lng );
					
					points.add( position );
				}
				lineOptions.addAll( points );
				lineOptions.width( 12 );
				lineOptions.color( Color.RED );
				lineOptions.geodesic( true );
			}
			mMap.addPolyline( lineOptions );
		}
	}
	
}
