package com.example.myapplication3.priceon.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication3.priceon.activities.ProductDetailActivity;
import com.example.myapplication3.priceon.R;
import com.example.myapplication3.priceon.data.model.Product;

import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {
    private final Context context;
    private List<Product> items;

    public FavoritesAdapter(Context context, List<Product> items) {
        this.context = context;
        this.items = items;
    }

    public void setItems(List<Product> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FavoritesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoritesAdapter.ViewHolder holder, int position) {
        Product product = items.get(position);

        holder.name.setText(product.getName());
        holder.brand.setText(product.getBrandName());
        holder.price.setText(String.format("%.2f €", product.getMinPrice()));
        Glide.with(context)
                .load(product.getPhotoUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("product", product);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, brand, price;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.productImage);
            name  = itemView.findViewById(R.id.productName);
            brand = itemView.findViewById(R.id.productBrand);
            price = itemView.findViewById(R.id.productPrice);
        }
    }
}
