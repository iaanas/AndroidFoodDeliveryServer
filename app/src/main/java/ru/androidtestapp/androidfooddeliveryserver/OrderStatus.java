package ru.androidtestapp.androidfooddeliveryserver;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jaredrummler.materialspinner.MaterialSpinner;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.androidtestapp.androidfooddeliveryserver.Common.Common;
import ru.androidtestapp.androidfooddeliveryserver.Interface.ItemClickListener;
import ru.androidtestapp.androidfooddeliveryserver.Model.MyResponse;
import ru.androidtestapp.androidfooddeliveryserver.Model.Notification;
import ru.androidtestapp.androidfooddeliveryserver.Model.Request;
import ru.androidtestapp.androidfooddeliveryserver.Model.Sender;
import ru.androidtestapp.androidfooddeliveryserver.Model.Token;
import ru.androidtestapp.androidfooddeliveryserver.Remote.APIService;
import ru.androidtestapp.androidfooddeliveryserver.ViewHolder.OrderViewHolder;

public class OrderStatus extends AppCompatActivity {
	
	RecyclerView recyclerView;
	RecyclerView.LayoutManager layoutManager;
	
	FirebaseRecyclerAdapter< Request, OrderViewHolder > adapter;
	FirebaseDatabase db;
	DatabaseReference request;
	
	MaterialSpinner spinner;
	
	APIService mService;
	
	
	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_order_status );
		
		//FireBase
		db = FirebaseDatabase.getInstance();
		request = db.getReference("Request");
		
		mService = Common.getFCMClient();
		
		recyclerView = (RecyclerView) findViewById( R.id.listOrders );
		recyclerView.setHasFixedSize( true );
		layoutManager = new LinearLayoutManager( this );
		recyclerView.setLayoutManager( layoutManager );
		
		loadOrders();
	}
	
	private void loadOrders( ) {
		adapter = new FirebaseRecyclerAdapter < Request, OrderViewHolder >(
				Request.class,
				R.layout.order_layout,
				OrderViewHolder.class,
				request
		) {
			@Override
			protected void populateViewHolder( OrderViewHolder viewHolder , final Request model , int position ) {
				viewHolder.txtOrderId.setText( adapter.getRef( position ).getKey() );
				viewHolder.txtOrderStatus.setText( Common.converCodeToStatus( model.getStatus() ) );
				viewHolder.txtOrderAddress.setText( model.getAddress() );
				viewHolder.txtOrderPhone.setText( model.getPhone() );
				
				viewHolder.btnEdit.setOnClickListener( new View.OnClickListener( ) {
					@Override
					public void onClick( View v ) {
						showUpdateDialog(adapter.getRef( position ).getKey(),
								adapter.getItem( position ));
					}
				} );
				
				viewHolder.btnRemove.setOnClickListener( new View.OnClickListener( ) {
					@Override
					public void onClick( View v ) {
						deleteOrder(adapter.getRef( position ).getKey());
					}
				} );
				
				viewHolder.btnDetail.setOnClickListener( new View.OnClickListener( ) {
					@Override
					public void onClick( View v ) {
						Intent orderDetail = new Intent( OrderStatus.this, OrderDetail.class );
						Common.currentRequest = model;
						orderDetail.putExtra( "OrderId", adapter.getRef( position ).getKey() );
						startActivity( orderDetail );
					}
				} );
				
				viewHolder.btnDirection.setOnClickListener( new View.OnClickListener( ) {
					@Override
					public void onClick( View v ) {
						Intent trackingOrder = new Intent( OrderStatus.this, TreckingOrder.class );
						Common.currentRequest = model;
						startActivity( trackingOrder );
					}
				} );
				
				
			}
		};
		adapter.notifyDataSetChanged();
		recyclerView.setAdapter( adapter );
	}
	
	private void deleteOrder( String key ) {
		request.child( key ).removeValue();
		adapter.notifyDataSetChanged();
	}
	
	private void showUpdateDialog( String key , final Request item ) {
		final AlertDialog.Builder alertDialog = new AlertDialog.Builder( OrderStatus.this );
		alertDialog.setTitle( "Update Order" );
		alertDialog.setMessage( "Please change status" );
		
		LayoutInflater inflater = this.getLayoutInflater();
		final View view = inflater.inflate( R.layout.update_order_layout, null );
		
		spinner = (MaterialSpinner) view.findViewById( R.id.statusSpinner );
		spinner.setItems( "Placed", "On my way", "Shipped" );
		
		alertDialog.setView( view );
		final String localKey = key;
		alertDialog.setPositiveButton( "YES" , new DialogInterface.OnClickListener( ) {
			@Override
			public void onClick( DialogInterface dialog , int which ) {
				dialog.dismiss();
				item.setStatus( String.valueOf( spinner.getSelectedIndex() ) );
				
				request.child( localKey ).setValue( item );
				adapter.notifyDataSetChanged();
				
				sendOrderStatusToUser(localKey, item);
			}
		} );
		alertDialog.setNegativeButton( "NO" , new DialogInterface.OnClickListener( ) {
			@Override
			public void onClick( DialogInterface dialog , int which ) {
				dialog.dismiss();
			}
		} );
		alertDialog.show();
		
		
	}
	
	private void sendOrderStatusToUser( final String key, final Request item ) {
		DatabaseReference tokens = db.getReference("Tokens");
		tokens.orderByKey().equalTo( item.getPhone() )
				.addValueEventListener( new ValueEventListener( ) {
					@Override
					public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
						for(DataSnapshot postSnapShot:dataSnapshot.getChildren()){
							Token token = postSnapShot.getValue(Token.class);
							
							Notification notification = new Notification( "Lyncmed Russia",
									"Ваш заказ "+key+" был обновлен");
							
							Sender content = new Sender( token.getToken(), notification );
							
							mService.sendNotification( content ).enqueue( new Callback < MyResponse >( ) {
								@Override
								public void onResponse( Call< MyResponse > call , Response< MyResponse > response ) {
									if(response.body().success == 1){
										Toast.makeText( OrderStatus.this, "Order status was update",
												Toast.LENGTH_SHORT).show();
									} else {
										Toast.makeText( OrderStatus.this, "Error",
												Toast.LENGTH_SHORT).show();
									}
								}
								
								@Override
								public void onFailure( Call < MyResponse > call , Throwable t ) {
									Log.e( "ERROR", t.getMessage() );
								}
							} );
						}
					}
					
					@Override
					public void onCancelled( @NonNull DatabaseError databaseError ) {
					
					}
				} );
	}
}
