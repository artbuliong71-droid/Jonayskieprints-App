package com.example.jonayskieprints.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.adapter.OrderAdapter;
import com.example.jonayskieprints.api.ApiClient;
import com.example.jonayskieprints.model.Order;             // ← Order na

import java.util.ArrayList;

public class DeletedOrdersFragment extends Fragment {

    private RecyclerView rvDeleted;
    private ProgressBar progress;
    private TextView tvEmpty, tvCount;
    private OrderAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_deleted_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvDeleted = view.findViewById(R.id.rv_deleted);
        progress  = view.findViewById(R.id.progress_deleted);
        tvEmpty   = view.findViewById(R.id.tv_empty_deleted);
        tvCount   = view.findViewById(R.id.tv_deleted_count);

        adapter = new OrderAdapter(new ArrayList<>(), null, false);
        rvDeleted.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDeleted.setAdapter(adapter);

        progress.setVisibility(View.VISIBLE);

        ApiClient.get().fetchDeletedOrders(orders -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                int n = orders.size();
                tvCount.setText(n + " record" + (n != 1 ? "s" : ""));
                if (orders.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    adapter.updateOrders(orders);            // ← walang cast, List<Order> na
                }
            });
        });
    }
}