package com.faltenreich.diaguard.feature.preference.factor;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.faltenreich.diaguard.R;
import com.faltenreich.diaguard.feature.preference.data.TimeInterval;
import com.faltenreich.diaguard.shared.data.database.entity.Category;
import com.faltenreich.diaguard.shared.event.Events;
import com.faltenreich.diaguard.shared.event.preference.FactorChangedEvent;
import com.faltenreich.diaguard.shared.view.chart.ChartUtils;
import com.faltenreich.diaguard.shared.view.fragment.BaseFragment;
import com.faltenreich.diaguard.shared.view.recyclerview.decoration.LinearDividerItemDecoration;
import com.faltenreich.diaguard.shared.view.resource.ColorUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class FactorFragment extends BaseFragment {

    private static final int X_AXIS_MINIMUM = 0;
    private static final int X_AXIS_MAXIMUM = DateTimeConstants.HOURS_PER_DAY;

    @BindView(R.id.values_chart) LineChart valuesChart;
    @BindView(R.id.time_interval_spinner) Spinner timeIntervalSpinner;
    @BindView(R.id.values_list) RecyclerView valuesList;

    private FactorListAdapter valuesListAdapter;
    private Factor factor;

    private TimeInterval timeInterval;

    public FactorFragment() {
        super(R.layout.fragment_factor);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initLayout();
        invalidateLayout();
    }

    private void initLayout() {
        initFactor();
        initSpinner();
        initChart();
        initList();
    }

    private void initFactor() {
        Bundle arguments = getActivity() != null && getActivity().getIntent() != null
            ? getActivity().getIntent().getExtras()
            : null;
        if (arguments == null) {
            throw new IllegalStateException("Arguments must not be null");
        }

        String key = getString(R.string.argument_factor);
        if (arguments.containsKey(key)) {
            int factorArgument = arguments.getInt(key);
            if (factorArgument == getResources().getInteger(R.integer.argument_factor_correction)) {
                factor = new CorrectionFactor();
            } else if (factorArgument == getResources().getInteger(R.integer.argument_factor_meal)) {
                factor = new MealFactor();
            } else if (factorArgument == getResources().getInteger(R.integer.argument_factor_basal_rate)) {
                factor = new BasalRateFactor();
            }
        }

        if (factor == null) {
            throw new IllegalStateException("Factor must not be null");
        }

        setTitle(factor.getTitle());
        timeInterval = factor.getTimeInterval();
    }

    private void initChart() {
        ChartUtils.setChartDefaultStyle(valuesChart, Category.INSULIN);
        valuesChart.setTouchEnabled(false);

        Resources resources = requireContext().getResources();
        int textColor = ColorUtils.getTextColorPrimary(getContext());
        int gridColor = ColorUtils.getBackgroundTertiary(getContext());

        valuesChart.getAxisLeft().setTextColor(textColor);
        valuesChart.getAxisLeft().setGridColor(gridColor);
        valuesChart.getAxisLeft().setGridLineWidth(1f);
        valuesChart.getAxisLeft().setXOffset(resources.getDimension(R.dimen.padding));

        valuesChart.getXAxis().setGridLineWidth(1f);
        valuesChart.getXAxis().setGridColor(gridColor);
        valuesChart.getXAxis().setTextColor(textColor);
        valuesChart.getXAxis().setAxisMinimum(X_AXIS_MINIMUM);
        valuesChart.getXAxis().setAxisMaximum(X_AXIS_MAXIMUM);
        valuesChart.getXAxis().setLabelCount((X_AXIS_MAXIMUM / 2) + 1, false);
        valuesChart.getXAxis().setValueFormatter((value, axis) -> {
            boolean showValue = value < X_AXIS_MAXIMUM;
            return showValue ? Integer.toString((int) value) : "";
        });

        valuesChart.setViewPortOffsets(
            resources.getDimension(R.dimen.chart_offset_left),
            0,
            0,
            resources.getDimension(R.dimen.chart_offset_bottom)
        );
    }

    private void initSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.time_rhythm,
            android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeIntervalSpinner.setAdapter(adapter);

        if (timeInterval.ordinal() < timeIntervalSpinner.getCount()) {
            timeIntervalSpinner.setSelection(timeInterval.ordinal());
        }

        timeIntervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                timeInterval = TimeInterval.values()[position];
                invalidateLayout();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private void initList() {
        valuesListAdapter = new FactorListAdapter(getContext());
        valuesList.setAdapter(valuesListAdapter);
        valuesList.setLayoutManager(new LinearLayoutManager(getContext()));
        valuesList.addItemDecoration(new LinearDividerItemDecoration(getContext()));
    }

    private void invalidateLayout() {
        invalidateChart();
        invalidateList();
    }

    private void invalidateChart() {
        List<Entry> entries = new ArrayList<>();
        for (int hourOfDay = 0; hourOfDay < DateTimeConstants.HOURS_PER_DAY; hourOfDay++) {
            Entry entry = new Entry();
            entry.setX(hourOfDay);
            entry.setY(factor.getValueForHour(hourOfDay));
            entries.add(entry);
        }

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        LineDataSet dataSet = new LineDataSet(entries, getString(factor.getTitle()));
        dataSet.setColor(ColorUtils.getTextColorPrimary(getContext()));
        dataSet.setLineWidth(ChartUtils.LINE_WIDTH);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);

        dataSets.add(dataSet);
        LineData data = new LineData(dataSets);

        valuesChart.setData(data);
        valuesChart.invalidate();
    }

    private void invalidateList() {
        valuesListAdapter.clear();
        DateTime dateTime = DateTime.now().withHourOfDay(timeInterval.startHour);
        while (valuesListAdapter.getItemCount() < DateTimeConstants.HOURS_PER_DAY / timeInterval.interval) {
            int hourOfDay = dateTime.getHourOfDay();
            valuesListAdapter.addItem(new FactorListItem(timeInterval, hourOfDay, factor.getValueForHour(hourOfDay)));
            dateTime = dateTime.withHourOfDay((hourOfDay + timeInterval.interval) % DateTimeConstants.HOURS_PER_DAY);
        }
        valuesListAdapter.notifyDataSetChanged();
    }

    @OnClick(R.id.fab)
    void store() {
        factor.setTimeInterval(timeInterval);

        for (int pos = 0; pos < valuesListAdapter.getItemCount(); pos++) {
            FactorListItem item = valuesListAdapter.getItem(pos);
            int hoursIntoInterval = 0;

            while (hoursIntoInterval < item.getInterval().interval) {
                int hourOfDay = (item.getHourOfDay() + hoursIntoInterval) % DateTimeConstants.HOURS_PER_DAY;
                factor.setValueForHour(item.getValue(), hourOfDay);
                hoursIntoInterval++;
            }
        }

        Events.post(new FactorChangedEvent());

        finish();
    }
}