package com.example.myapplication3.priceon.ui.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication3.priceon.ProductDetailActivity;
import com.example.myapplication3.priceon.R;
import com.example.myapplication3.priceon.data.model.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder>{
    private List<Product> products;
    private OnProductClickListener listener;


    public ProductAdapter(List<Product> productos, OnProductClickListener listener) {
        this.products = productos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);

        holder.nameText.setText(product.getName());

        //String brandName = (product.getBrandName() != null) ? product.getBrandName() : "Desconocida";
        holder.brandText.setText(
                product.getBrandName()!=null ? product.getBrandName() : "Desconocida"
        );

        //String precioTexto = product.getMinPrice() > 0 ? product.getMinPrice() + " €" : "Precio no disponible";
        holder.priceText.setText(
                product.getMinPrice()>0
                        ? product.getMinPrice()+" €"
                        : ""
        );

        //holder.pricePerUnitText.setText(product.getPricePerUnit() + " € / " + product.getUnit());

        String pricePerUnit = product.getPricePerUnit();
        if (!pricePerUnit.equals("-")) {
            holder.pricePerUnitText.setText(pricePerUnit + " € / " + product.getUnit());
        } else {
            holder.pricePerUnitText.setText("");
        }

        String qty = (product.getQuantityPack()>0 && product.getQuantityUnity()>0)
                ? product.getQuantityPack()+" x "+product.getQuantityUnity()+" "+product.getUnit()
                : "";
        holder.quantityText.setText(qty);

        Glide.with(holder.itemView.getContext())
                .load(product.getPhotoUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(holder.imageView);

        // Mostrar logos de supermercados
        holder.supermarketLogoContainer.removeAllViews();
        for (String logoUrl : product.getSupermarketLogoUrls()) {
            ImageView logo = new ImageView(holder.itemView.getContext());
            int dp = (int)(38 * holder.itemView.getContext()
                    .getResources().getDisplayMetrics().density + .5f);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp, dp);
            int m = (int)(8 * holder.itemView.getContext()
                    .getResources().getDisplayMetrics().density + .5f);
            p.setMargins(m,0,m,0);
            logo.setLayoutParams(p);
            Glide.with(holder.itemView.getContext()).load(logoUrl).into(logo);
            holder.supermarketLogoContainer.addView(logo);
        }

        holder.itemView.setOnClickListener(v -> listener.onProductClick(product));

    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, brandText, priceText, pricePerUnitText, quantityText;
        ImageView imageView;
        LinearLayout supermarketLogoContainer;

        ProductViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.productName);
            brandText = itemView.findViewById(R.id.productBrand);
            priceText = itemView.findViewById(R.id.productPrice);
            pricePerUnitText = itemView.findViewById(R.id.productPricePerUnit);
            imageView = itemView.findViewById(R.id.productImage);
            supermarketLogoContainer = itemView.findViewById(R.id.supermarketLogoContainer);
            quantityText = itemView.findViewById(R.id.productQuantity);

        }
    }

    public interface OnProductClickListener {
        void onProductClick(Product p);
    }
}
