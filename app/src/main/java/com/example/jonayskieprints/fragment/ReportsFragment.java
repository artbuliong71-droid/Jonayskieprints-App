package com.example.jonayskieprints.fragment;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.jonayskieprints.R;
import com.example.jonayskieprints.api.ApiClient;
import com.example.jonayskieprints.model.AdminStats;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportsFragment extends Fragment {

    private TextView tvSelectedDate;
    private Button btnGenerate, btnPickDate;
    private Spinner spinnerPeriod;
    private LineChart lineChart;
    private View llChartEmpty;

    private String selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    private String selectedPeriod = "daily";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvSelectedDate   = view.findViewById(R.id.tv_selected_date);
        btnGenerate      = view.findViewById(R.id.btn_generate_report);
        btnPickDate      = view.findViewById(R.id.btn_pick_date);
        spinnerPeriod    = view.findViewById(R.id.spinner_period);
        lineChart        = view.findViewById(R.id.line_chart);
        llChartEmpty     = view.findViewById(R.id.ll_chart_empty);

        tvSelectedDate.setText(selectedDate);

        // Period spinner
        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String[] periods = {"daily", "weekly", "monthly"};
                selectedPeriod = periods[pos];
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Date picker
        btnPickDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
                selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
                tvSelectedDate.setText(selectedDate);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Generate report
        btnGenerate.setOnClickListener(v -> generateReport());

        setupLineChart();
    }

    private void generateReport() {
        btnGenerate.setEnabled(false);
        btnGenerate.setText("Loading…");

        ApiClient.get().fetchStats(stats -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                btnGenerate.setEnabled(true);
                btnGenerate.setText("Generate Report");
                updateStatCards(stats);
                buildChartData(stats);
            });
        });
    }

    private void updateStatCards(AdminStats s) {
        View root = getView();
        if (root == null) return;

        int rate = s.totalOrders > 0 ? Math.round((s.completedOrders * 100f) / s.totalOrders) : 0;

        setStatCard("rstat_orders",  String.valueOf(s.totalOrders));
        setStatCard("rstat_revenue",       "₱" + s.totalSales);
        setStatCard("rstat_customers",     String.valueOf(s.totalCustomers));
        setStatCard("rstat_completion",    rate + "%");

        // Update Sales label in report card if needed
        View card = root.findViewWithTag("rstat_revenue");
        if (card != null) {
            TextView tvLabel = card.findViewById(R.id.tv_rstat_label);
            if (tvLabel != null) tvLabel.setText("Total Sales");
        }
    }

    private void setStatCard(String tag, String value) {
        View root = getView();
        if (root != null) {
            View card = root.findViewWithTag(tag);
            if (card != null) {
                TextView tv = card.findViewById(R.id.tv_rstat_value);
                if (tv != null) tv.setText(value);
            }
        }
    }

    private void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(true);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.parseColor("#9CA3AF"));
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.parseColor("#9CA3AF"));
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F3F4F6"));

        lineChart.getAxisRight().setEnabled(false);
        lineChart.setNoDataText("Generate a report to see the chart");
        lineChart.setNoDataTextColor(Color.parseColor("#9CA3AF"));
    }

    private void buildChartData(AdminStats stats) {
        int slots = "daily".equals(selectedPeriod) ? 24 : "weekly".equals(selectedPeriod) ? 7 : 30;

        List<Entry> salesEntries = new ArrayList<>();
        List<Entry> orderEntries   = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < slots; i++) {
            salesEntries.add(new Entry(i, 0));
            orderEntries.add(new Entry(i, 0));

            if ("daily".equals(selectedPeriod)) {
                labels.add(String.format(Locale.getDefault(), "%02d:00", i));
            } else if ("weekly".equals(selectedPeriod)) {
                labels.add(new String[]{"Sun","Mon","Tue","Wed","Thu","Fri","Sat"}[i % 7]);
            } else {
                labels.add("Day " + (i + 1));
            }
        }

        if (stats.totalOrders > 0) {
            int mid = slots / 2;
            double rev = 0;
            try { rev = Double.parseDouble(stats.totalSales); } catch (Exception ignored) {}
            salesEntries.set(mid, new Entry(mid, (float) rev));
            orderEntries.set(mid, new Entry(mid, stats.totalOrders));
        }

        LineDataSet salesSet = new LineDataSet(salesEntries, "Sales (₱)");
        salesSet.setColor(Color.parseColor("#3B82F6"));
        salesSet.setCircleColor(Color.parseColor("#3B82F6"));
        salesSet.setLineWidth(2.5f);
        salesSet.setCircleRadius(3f);
        salesSet.setDrawValues(false);
        salesSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineDataSet ordSet = new LineDataSet(orderEntries, "Orders");
        ordSet.setColor(Color.parseColor("#F43F5E"));
        ordSet.setCircleColor(Color.parseColor("#F43F5E"));
        ordSet.setLineWidth(2.5f);
        ordSet.setCircleRadius(3f);
        ordSet.setDrawValues(false);
        ordSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ordSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        lineChart.getAxisRight().setEnabled(true);

        final List<String> finalLabels = labels;
        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                int idx = (int) value;
                return idx >= 0 && idx < finalLabels.size() ? finalLabels.get(idx) : "";
            }
        });

        lineChart.setData(new LineData(salesSet, ordSet));
        lineChart.invalidate();

        lineChart.setVisibility(View.VISIBLE);
        llChartEmpty.setVisibility(View.GONE);
    }
}