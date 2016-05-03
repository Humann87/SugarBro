package com.faltenreich.diaguard.ui.activity;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.faltenreich.diaguard.R;
import com.faltenreich.diaguard.data.PreferenceHelper;
import com.faltenreich.diaguard.data.dao.EntryDao;
import com.faltenreich.diaguard.data.dao.MeasurementDao;
import com.faltenreich.diaguard.data.entity.Measurement;
import com.faltenreich.diaguard.util.ChartHelper;
import com.faltenreich.diaguard.util.Helper;
import com.faltenreich.diaguard.util.TimeSpan;
import com.faltenreich.diaguard.util.thread.BaseAsyncTask;
import com.faltenreich.diaguard.util.thread.UpdateBloodSugarPieChartTask;
import com.faltenreich.diaguard.util.thread.UpdateLineChartTask;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * Created by Filip on 15.11.13.
 */

public class StatisticsActivity extends BaseActivity {

    private static final int MIN_MAX_Y_VALUE = 3;

    @BindView(R.id.statistics_image_category) ImageView imageViewCategory;
    @BindView(R.id.statistics_categories) Spinner spinnerCategories;
    @BindView(R.id.statistics_interval) Spinner spinnerInterval;
    @BindView(R.id.statistics_measurement_count_avg) TextView textViewMeasurementCountAvg;
    @BindView(R.id.statistics_layout_hyper) ViewGroup layoutAvgHyper;
    @BindView(R.id.statistics_avg_hyper) TextView textViewAvgHyper;
    @BindView(R.id.statistics_layout_hypo) ViewGroup layoutAvgHypo;
    @BindView(R.id.statistics_avg_hypo) TextView textViewAvgHypo;
    @BindView(R.id.statistics_avg_unit) TextView textViewAvgUnit;
    @BindView(R.id.statistics_avg_value) TextView textViewAvgValue;
    @BindView(R.id.statistics_chart_trend) LineChart chartTrend;
    @BindView(R.id.layout_distribution) ViewGroup layoutDistribution;
    @BindView(R.id.statistics_chart_distribution) PieChart chartDistribution;

    private TimeSpan timeSpan;
    private Measurement.Category category;

    public StatisticsActivity() {
        super(R.layout.activity_statistics);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateContent();
    }

    private void init() {
        this.timeSpan = TimeSpan.WEEK;
        this.category = Measurement.Category.BLOODSUGAR;
        initLayout();
    }

    private void initLayout() {
        initializeCharts();
        initSpinners();
    }

    private void initSpinners() {
        final Measurement.Category[] categories = PreferenceHelper.getInstance().getActiveCategories();
        List<String> categoryNames = new ArrayList<>();
        for (Measurement.Category category : categories) {
            categoryNames.add(category.toLocalizedString());
        }
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategories.setAdapter(categoryAdapter);
        spinnerCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                changeCategory(categories[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        final TimeSpan[] timeSpans = TimeSpan.values();
        List<String> timeSpanNames = new ArrayList<>();
        for (TimeSpan timeSpan : timeSpans) {
            timeSpanNames.add(timeSpan.toIntervalLabel());
        }
        ArrayAdapter<String> timeSpanAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timeSpanNames);
        timeSpanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerInterval.setAdapter(timeSpanAdapter);
        spinnerInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                changeTimeInterval(timeSpans[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateContent() {
        updateViews();
        updateCharts();
    }

    private void changeTimeInterval(TimeSpan timeSpan) {
        this.timeSpan = timeSpan;
        updateContent();
    }

    private void changeCategory(Measurement.Category category) {
        this.category = category;
        updateContent();
    }

    private void updateViews() {
        Interval interval = timeSpan.getPastInterval(DateTime.now());
        long days = interval.toDuration().getStandardDays();

        imageViewCategory.setImageDrawable(ContextCompat.getDrawable(this,
                PreferenceHelper.getInstance().getCategoryImageResourceId(category)));

        Measurement avgMeasurement = MeasurementDao.getInstance(category.toClass()).getAvgMeasurement(category, interval);
        textViewAvgUnit.setText(category.stackValues() ?
                String.format("%s %s", PreferenceHelper.getInstance().getUnitName(category), getString(R.string.per_day)) :
                PreferenceHelper.getInstance().getUnitName(category));

        textViewAvgValue.setText(avgMeasurement.toString());

        long count = EntryDao.getInstance().count(category, interval.getStart(), interval.getEnd());
        float avgCountPerDay = (float) count / (float) days;
        textViewMeasurementCountAvg.setText(Helper.parseFloat(avgCountPerDay));

        if (category == Measurement.Category.BLOODSUGAR) {
            layoutAvgHyper.setVisibility(View.VISIBLE);
            layoutAvgHypo.setVisibility(View.VISIBLE);
            long hyperCount = EntryDao.getInstance().countAbove(interval.getStart(), interval.getEnd(), PreferenceHelper.getInstance().getLimitHyperglycemia());
            long hypoCount = EntryDao.getInstance().countBelow(interval.getStart(), interval.getEnd(), PreferenceHelper.getInstance().getLimitHypoglycemia());
            float avgHypersPerDay = (float) hyperCount / (float) days;
            float avgHyposPerDay = (float) hypoCount / (float) days;
            textViewAvgHyper.setText(Helper.parseFloat(avgHypersPerDay));
            textViewAvgHypo.setText(Helper.parseFloat(avgHyposPerDay));
        } else {
            layoutAvgHyper.setVisibility(View.GONE);
            layoutAvgHypo.setVisibility(View.GONE);
        }
    }

    // region Charting

    private void initializeCharts() {
        ChartHelper.setChartDefaultStyle(chartTrend, category);
        chartTrend.setTouchEnabled(false);
        chartTrend.getXAxis().setLabelsToSkip(0);
        chartTrend.getXAxis().setDrawAxisLine(true);
        chartTrend.getAxisLeft().setDrawAxisLine(false);
        chartTrend.getAxisLeft().setLabelCount(5, false);
        chartTrend.getLegend().setEnabled(true);
        chartTrend.getLegend().setPosition(Legend.LegendPosition.BELOW_CHART_RIGHT);

        chartDistribution.setDrawHoleEnabled(false);
        chartDistribution.setUsePercentValues(true);
        chartDistribution.setDescription(null);
        chartDistribution.setDrawSliceText(false);
        chartDistribution.setNoDataText(getString(ChartHelper.NO_DATA_TEXT_RESOURCE_ID));
        chartDistribution.getPaint(Chart.PAINT_INFO).setColor(ContextCompat.getColor(this, ChartHelper.NO_DATA_COLOR_RESOURCE_ID));
        chartDistribution.getLegend().setPosition(Legend.LegendPosition.BELOW_CHART_RIGHT);
    }


    private void updateCharts() {

        new UpdateLineChartTask(this, new BaseAsyncTask.OnAsyncProgressListener<LineData>() {
            @Override
            public void onPostExecute(LineData lineData) {
                boolean hasData = false;
                for (ILineDataSet lineDataSet : lineData.getDataSets()) {
                    if (lineDataSet.getEntryCount() > 0) {
                        hasData = true;
                        break;
                    }
                }

                chartTrend.getAxisLeft().removeAllLimitLines();

                ViewGroup.LayoutParams params = chartTrend.getLayoutParams();
                params.height = hasData ? (int) getResources().getDimension(R.dimen.line_chart_height_detailed) : ViewGroup.LayoutParams.WRAP_CONTENT;
                chartTrend.setLayoutParams(params);

                if (hasData) {
                    float yAxisMinValue = PreferenceHelper.getInstance().getExtrema(category)[0] * .9f;
                    float yAxisMinCustomValue = PreferenceHelper.getInstance().formatDefaultToCustomUnit(category, yAxisMinValue);
                    chartTrend.getAxisLeft().setAxisMinValue(yAxisMinCustomValue);

                    float yAxisMaxCustomValue = lineData.getYMax();
                    yAxisMaxCustomValue = yAxisMaxCustomValue > MIN_MAX_Y_VALUE ? yAxisMaxCustomValue : MIN_MAX_Y_VALUE;
                    chartTrend.getAxisLeft().setAxisMaxValue(yAxisMaxCustomValue * 1.1f);

                    if (category == Measurement.Category.BLOODSUGAR) {
                        float targetValue = PreferenceHelper.getInstance().
                                formatDefaultToCustomUnit(Measurement.Category.BLOODSUGAR,
                                        PreferenceHelper.getInstance().getTargetValue());
                        chartTrend.getAxisLeft().addLimitLine(ChartHelper.getLimitLine(StatisticsActivity.this, targetValue, R.color.green));

                        if (PreferenceHelper.getInstance().limitsAreHighlighted()) {
                            float limitHypo = PreferenceHelper.getInstance().
                                    formatDefaultToCustomUnit(Measurement.Category.BLOODSUGAR,
                                            PreferenceHelper.getInstance().getLimitHypoglycemia());
                            chartTrend.getAxisLeft().addLimitLine(ChartHelper.getLimitLine(StatisticsActivity.this, limitHypo, R.color.blue));
                            float limitHyper = PreferenceHelper.getInstance().
                                    formatDefaultToCustomUnit(Measurement.Category.BLOODSUGAR,
                                            PreferenceHelper.getInstance().getLimitHyperglycemia());
                            chartTrend.getAxisLeft().addLimitLine(ChartHelper.getLimitLine(StatisticsActivity.this, limitHyper, R.color.red));
                        }
                    }

                    chartTrend.setData(lineData);
                    chartTrend.invalidate();
                } else {
                    chartTrend.clear();
                }
            }
        }, category, timeSpan, false, true).execute();

        if (category == Measurement.Category.BLOODSUGAR) {
            layoutDistribution.setVisibility(View.VISIBLE);

            new UpdateBloodSugarPieChartTask(this, new BaseAsyncTask.OnAsyncProgressListener<PieData>() {
                @Override
                public void onPostExecute(PieData pieData) {
                    boolean hasData = pieData.getDataSet().getEntryCount() > 0;
                    chartDistribution.setData(hasData ? pieData : null);
                    ViewGroup.LayoutParams params = chartDistribution.getLayoutParams();
                    params.height = hasData ? (int) getResources().getDimension(R.dimen.pie_chart_height) : ViewGroup.LayoutParams.WRAP_CONTENT;
                    chartDistribution.setLayoutParams(params);
                    chartDistribution.invalidate();
                }
            }, timeSpan).execute();
        } else {
            layoutDistribution.setVisibility(View.GONE);
        }
    }

    // endregion
}