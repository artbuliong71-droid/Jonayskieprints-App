package com.example.jonayskieprints.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.adapter.CustomerAdapter;
import com.example.jonayskieprints.api.ApiClient;
import com.example.jonayskieprints.model.Customer;

import java.util.ArrayList;

public class CustomersFragment extends Fragment {

    private RecyclerView rvCustomers;
    private TextView tvCount;
    private CustomerAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvCustomers = view.findViewById(R.id.rv_customers);
        tvCount     = view.findViewById(R.id.tv_customer_count);

        adapter = new CustomerAdapter(new ArrayList<>());
        rvCustomers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCustomers.setAdapter(adapter);

        ApiClient.get().fetchCustomers(customers -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                adapter.update(customers);
                int n = customers.size();
                tvCount.setText(n + " customer" + (n != 1 ? "s" : ""));
            });
        });
    }
}
