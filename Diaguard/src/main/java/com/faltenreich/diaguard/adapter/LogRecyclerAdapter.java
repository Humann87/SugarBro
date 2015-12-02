package com.faltenreich.diaguard.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.faltenreich.diaguard.data.dao.EntryDao;
import com.faltenreich.diaguard.R;
import com.faltenreich.diaguard.data.entity.Entry;
import com.faltenreich.diaguard.data.entity.Measurement;
import com.faltenreich.diaguard.ui.viewholder.BaseViewHolder;
import com.faltenreich.diaguard.ui.viewholder.LogDayViewHolder;
import com.faltenreich.diaguard.ui.viewholder.LogEntryViewHolder;
import com.faltenreich.diaguard.ui.viewholder.LogMonthViewHolder;
import com.tonicartos.superslim.GridSLM;
import com.tonicartos.superslim.LinearSLM;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by Filip on 04.11.13.
 */
public class LogRecyclerAdapter extends BaseAdapter<ListItem, BaseViewHolder<ListItem>> {

    private static final String TAG = LogRecyclerAdapter.class.getSimpleName();

    private enum ViewType {
        MONTH,
        DAY,
        ENTRY,
        MEASUREMENT
    }

    private DateTime maxVisibleDate;
    private DateTime minVisibleDate;

    public LogRecyclerAdapter(Context context, DateTime firstVisibleDay) {
        super(context);

        minVisibleDate = firstVisibleDay.withDayOfMonth(1);
        maxVisibleDate = minVisibleDate;

        appendNextMonth();

        // Workaround to endless scrolling when after visible threshold
        if (firstVisibleDay.dayOfMonth().get() >= (firstVisibleDay.dayOfMonth().getMaximumValue() -
                EndlessScrollListener.VISIBLE_THRESHOLD) - 1) {
            appendNextMonth();
        }
    }

    public void appendRows(EndlessScrollListener.Direction direction) {
        if (direction == EndlessScrollListener.Direction.DOWN) {
            appendNextMonth();
        } else {
            appendPreviousMonth();
        }
    }

    private void appendNextMonth() {
        DateTime targetDate = maxVisibleDate.plusMonths(1);
        int sectionManager = targetDate.getMonthOfYear() % 2;
        int sectionFirstPosition = getItemCount();

        // Header
        addItem(new ListItemMonth(maxVisibleDate, sectionManager, sectionFirstPosition));
        notifyItemInserted(getItemCount() - 1);

        while (maxVisibleDate.isBefore(targetDate)) {
            addItem(new ListItemDay(maxVisibleDate, sectionManager, sectionFirstPosition));
            List<Entry> entries = fetchData(maxVisibleDate);
            for (Entry entry : entries) {
                addItem(new ListItemEntry(entry, sectionManager, sectionFirstPosition));
            }
            int insertCount = entries.size() + 1;
            int maxPosition = getItemCount() - 1;
            notifyItemRangeInserted(maxPosition - insertCount, maxPosition);
            maxVisibleDate = maxVisibleDate.plusDays(1);
        }
    }

    private void appendPreviousMonth() {
        DateTime targetDate = minVisibleDate.minusMonths(1);
        int sectionManager = targetDate.getMonthOfYear() % 2;
        int sectionFirstPosition = 0;

        while (minVisibleDate.isAfter(targetDate)) {
            minVisibleDate = minVisibleDate.minusDays(1);
            List<Entry> entries = fetchData(maxVisibleDate);
            for (Entry entry : entries) {
                addItem(0, new ListItemEntry(entry, sectionManager, 0));
            }
            addItem(0, new ListItemDay(minVisibleDate, sectionManager, 0));
            int insertCount = entries.size() + 1;
            notifyItemRangeInserted(0, insertCount);
        }

        // Header
        addItem(0, new ListItemMonth(minVisibleDate, sectionManager, sectionFirstPosition));
        notifyItemInserted(0);
    }

    private List<Entry> fetchData(DateTime day) {
        List<Entry> entriesOfDay = EntryDao.getInstance().getEntriesOfDay(day);
        // TODO: Get rid of measurementCache (Currently required to enable lazy loading for generic measurements
        for (Entry entry : entriesOfDay) {
            List<Measurement> measurements = EntryDao.getInstance().getMeasurements(entry);
            entry.setMeasurementCache(measurements);
        }
        return entriesOfDay;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItemCount() == 0) {
            return ViewType.ENTRY.ordinal();
        } else {
            ListItem item = getItem(position);
            if (item instanceof ListItemMonth) {
                return ViewType.MONTH.ordinal();
            } else if (item instanceof ListItemDay) {
                return ViewType.DAY.ordinal();
            } else if (item instanceof ListItemEntry) {
                return ViewType.ENTRY.ordinal();
            }
        }
        return super.getItemViewType(position);
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewTypeInt) {
        ViewType viewType = ViewType.values()[viewTypeInt];
        switch (viewType) {
            case MONTH:
                return new LogMonthViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.list_item_log_month, parent, false));
            case DAY:
                return new LogDayViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.list_item_log_day, parent, false));
            case ENTRY:
                return new LogEntryViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.list_item_log_entry, parent, false));
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        if (holder != null) {
            ListItem listItem = getItem(position);
            holder.bindData(listItem);

            GridSLM.LayoutParams params = GridSLM.LayoutParams.from(holder.itemView.getLayoutParams());
            params.setFirstPosition(listItem.getSectionFirstPosition());
            params.setSlm(LinearSLM.ID);
            holder.itemView.setLayoutParams(params);
            Log.i(TAG, String.format("Added row for section %d", listItem.getSectionFirstPosition()));
        }
    }

    @Override
    public void onViewRecycled(BaseViewHolder holder) {
        if (holder instanceof LogEntryViewHolder) {
            ((LogEntryViewHolder)holder).measurements.removeAllViews();
        }
        super.onViewRecycled(holder);
    }
}