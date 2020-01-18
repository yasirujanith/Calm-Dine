package com.example.calmdine;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.calmdine.ServicesFire.BackendServices;
import com.example.calmdine.models.AdapterModel;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.makeramen.roundedimageview.RoundedTransformationBuilder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;
import java.util.ArrayList;
import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.MyViewHolder> {
    private List<AdapterModel> mRestaurants = new ArrayList<AdapterModel>();
    private Context mContext;

    public GeoDataClient mGeoDataClient;
    public BackendServices backendServices;
    public FirebaseStorage storage;

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView txtNoiseValue;
        public TextView txtLightValue;
        public TextView txtRestaurantName;
        public TextView txtRateValue;
        public RatingBar ratingBar;
        public ImageView imageView;

        public MyViewHolder(View view) {
            super(view);
            this.txtLightValue = view.findViewById(R.id.txtLightValue);
            this.txtNoiseValue = view.findViewById(R.id.txtNoiseValue);
            this.ratingBar = view.findViewById(R.id.ratingBar);
            this.txtRestaurantName = view.findViewById(R.id.txtRestaurantName);
            this.txtRateValue = view.findViewById(R.id.txtRateValue);
            this.imageView = view.findViewById(R.id.imgRestaurant);
        }

        @Override
        public void onClick(View view) {
//            Log.i("Click", "event triggered");
//            Log.i("values", txtRestaurantName.toString());
        }
    }

    public RestaurantAdapter(List<AdapterModel> restaurant) {
        mRestaurants = restaurant;
        backendServices = new BackendServices(mContext);
        storage = FirebaseStorage.getInstance();
        for (AdapterModel rest: restaurant) {
            Log.i("RestaurantNameIsAdapter", rest.getName());
            Log.i("RestaurantNameIsAdapter", String.valueOf(rest.getRating()));
        }
    }

    @Override
    public RestaurantAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        LinearLayout recommendationListLayoutLinearLayout = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_recommendation_item, parent, false);
        MyViewHolder vh = new MyViewHolder(recommendationListLayoutLinearLayout);
        return vh;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final AdapterModel currentRestaurant = mRestaurants.get(position);
        holder.txtRestaurantName.setText(currentRestaurant.getName());
        holder.txtLightValue.setText(String.format("%.3f",(currentRestaurant.getLight())));
        holder.txtNoiseValue.setText(String.format("%.3f",(currentRestaurant.getNoise())));
        holder.ratingBar.setRating(Float.parseFloat(String.valueOf(currentRestaurant.getRating())));
        holder.txtRateValue.setText("("+currentRestaurant.getRating()+")");


        Log.i("RestaurantNameIs", String.valueOf(mRestaurants.get(position).getRating()));
        StorageReference storageReference = storage.getReferenceFromUrl("gs://calmdine.appspot.com/images/" + currentRestaurant.getName());
        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Transformation transformation = new RoundedTransformationBuilder()
                        .borderColor(Color.BLACK)
                        .borderWidthDp(1)
                        .cornerRadiusDp(10)
                        .oval(false)
                        .build();
                Picasso.get().load(uri).fit().transform(transformation).into(holder.imageView);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });
        LayerDrawable stars = (LayerDrawable) holder.ratingBar.getProgressDrawable();
        stars.getDrawable(2).setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
        stars.getDrawable(0).setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
        stars.getDrawable(1).setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("geo:"+currentRestaurant.getLatitude()+","+currentRestaurant.getLongitude() + "?q=" + Uri.encode(currentRestaurant.getName()));
//                Log.i("uri", String.valueOf(uri));
                Uri gmmIntentUri = uri;
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                mContext.startActivity(mapIntent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mRestaurants.size();
    }
}
