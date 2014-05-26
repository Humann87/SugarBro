package com.android.diaguard.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.diaguard.R;
import com.android.diaguard.database.Event;
import com.android.diaguard.helpers.Helper;
import com.android.diaguard.helpers.PreferenceHelper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Filip on 04.11.13.
 */
public class ListViewAdapterLog extends BaseAdapter {

    private static class ViewHolder {
        ImageView image;
        TextView time;
        TextView unit;
        TextView value;
        ImageView noteInfo;
    }

    Context context;
    public List<Event> events;
    PreferenceHelper preferenceHelper;
    HashMap<String, Integer> imageResources;

    public ListViewAdapterLog(Context context){
        this.context = context;
        this.events = new ArrayList<Event>();

        preferenceHelper = new PreferenceHelper((Activity)context);

        // Pre-load image resources
        imageResources = new HashMap<String, Integer>();
        for(Event.Category category : Event.Category.values()) {
            String name = category.name().toLowerCase();
            int resourceId = context.getResources().getIdentifier(name,
                    "drawable", context.getPackageName());
            imageResources.put(name, resourceId);
        }
    }

    public int getCount() {
        return events.size();
    }

    public Event getItem(int position) {
        return events.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null)
        {
            LayoutInflater inflate = (LayoutInflater) context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflate.inflate(R.layout.listview_row_log, parent, false);

            holder = new ViewHolder();
            holder.image = (ImageView) convertView.findViewById(R.id.image);
            holder.time = (TextView)convertView.findViewById(R.id.time);
            holder.unit = (TextView)convertView.findViewById(R.id.unit);
            holder.value = (TextView)convertView.findViewById(R.id.value);
            holder.noteInfo = (ImageView) convertView.findViewById(R.id.notes);

            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder) convertView.getTag();

        Event event = getItem(position);

        holder.image.setImageResource(imageResources.get(event.getCategory().name().toLowerCase()));

        holder.time.setText(preferenceHelper.getTimeFormat().format(event.getDate().getTime()));

        holder.unit.setText(preferenceHelper.getUnitAcronym(event.getCategory()));

        float valueFloat = preferenceHelper.formatDefaultToCustomUnit(
                event.getCategory(), event.getValue());
        DecimalFormat format = Helper.getDecimalFormat();
        holder.value.setText(format.format(valueFloat));
        holder.value.setTextColor(Color.BLACK);

        // Highlighting
        if(event.getCategory() == Event.Category.BloodSugar &&
                preferenceHelper.limitsAreHighlighted()) {
            if(valueFloat >= preferenceHelper.getLimitHyperglycemia())
                holder.value.setTextColor(Color.RED);
            else if(valueFloat <= preferenceHelper.getLimitHypoglycemia())
                holder.value.setTextColor(Color.BLUE);
        }

        if(event.getNotes().length() > 0)
            holder.noteInfo.setVisibility(View.VISIBLE);
        return convertView;
    }
}