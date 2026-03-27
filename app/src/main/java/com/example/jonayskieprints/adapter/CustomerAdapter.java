package com.example.jonayskieprints.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.model.Customer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.VH> {

    private List<Customer> list;

    public CustomerAdapter(List<Customer> list) { this.list = list; }

    public void update(List<Customer> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Customer c = list.get(pos);
        h.tvName.setText(c.getFullName().trim());
        h.tvEmail.setText(c.email);
        h.tvPhone.setText(c.phone != null && !c.phone.isEmpty() ? c.phone : "No phone");
        h.tvOrders.setText(c.totalOrders + " orders");
        h.tvJoined.setText("Joined " + fmtDate(c.createdAt));
    }

    @Override public int getItemCount() { return list.size(); }

    private String fmtDate(String iso) {
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            Date d = in.parse(iso);
            return d != null ? out.format(d) : iso;
        } catch (Exception e) { return iso != null ? iso : ""; }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvPhone, tvOrders, tvJoined;
        VH(View v) {
            super(v);
            tvName   = v.findViewById(R.id.tv_contact_name);
            tvEmail  = v.findViewById(R.id.tv_contact_email);
            tvPhone  = v.findViewById(R.id.tv_contact_phone);
            tvOrders = v.findViewById(R.id.tv_contact_orders);
            tvJoined = v.findViewById(R.id.tv_contact_joined);
        }
    }
}
