package ru.androidtestapp.androidfooddeliveryserver.ViewHolder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ru.androidtestapp.androidfooddeliveryserver.Interface.ItemClickListener;
import ru.androidtestapp.androidfooddeliveryserver.R;

public class OrderViewHolder extends RecyclerView.ViewHolder{
	
	public TextView txtOrderId, txtOrderStatus, txtOrderPhone, txtOrderAddress;
	private ItemClickListener itemClickListener;
	
	public Button btnEdit, btnRemove, btnDetail, btnDirection;
	
	
	public OrderViewHolder( @NonNull View itemView ) {
		super( itemView );
		txtOrderAddress = (TextView) itemView.findViewById( R.id.order_address );
		txtOrderId = (TextView) itemView.findViewById( R.id.order_id );
		txtOrderStatus = (TextView) itemView.findViewById( R.id.order_status );
		txtOrderPhone = (TextView) itemView.findViewById( R.id.order_phone );
		
		btnEdit = ( Button )  itemView.findViewById( R.id.btnEdit );
		btnRemove = ( Button ) itemView.findViewById( R.id.btnRemove );
		btnDetail = ( Button ) itemView.findViewById( R.id.btnDetail );
		btnDirection = (Button ) itemView.findViewById( R.id.btnDirection );
		
		
	}
	
}

