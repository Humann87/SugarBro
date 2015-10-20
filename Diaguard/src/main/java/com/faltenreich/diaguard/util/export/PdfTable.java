package com.faltenreich.diaguard.util.export;

import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.faltenreich.diaguard.DiaguardApplication;
import com.faltenreich.diaguard.R;
import com.faltenreich.diaguard.data.PreferenceHelper;
import com.faltenreich.diaguard.data.dao.EntryDao;
import com.faltenreich.diaguard.data.entity.Measurement;
import com.faltenreich.diaguard.util.DateTimeUtils;
import com.pdfjet.Align;
import com.pdfjet.Cell;
import com.pdfjet.Color;
import com.pdfjet.CoreFont;
import com.pdfjet.Font;
import com.pdfjet.Line;
import com.pdfjet.PDF;
import com.pdfjet.Point;
import com.pdfjet.Table;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Faltenreich on 19.10.2015.
 */
public class PdfTable extends Table {

    private static final String TAG = PdfTable.class.getSimpleName();

    private static final int ALTERNATING_ROW_COLOR = ContextCompat.getColor(DiaguardApplication.getContext(), R.color.gray_lighter);
    private static final float LABEL_WIDTH = 120;
    private static final int HOURS_TO_SKIP = 2;

    private PDF pdf;
    private PdfPage page;
    private DateTime day;

    private Font fontNormal;
    private Font fontBold;

    public PdfTable(PDF pdf, PdfPage page, DateTime day) {
        super();
        this.pdf = pdf;
        this.page = page;
        this.day = day;
        init();
    }

    private void init() {
        try {
            fontNormal = new Font(pdf, CoreFont.HELVETICA);
            fontBold = new Font(pdf, CoreFont.HELVETICA_BOLD);

            setData(getData());
            setNoCellBorders();
        } catch (Exception e) {
            Log.e(TAG, "Failed to init");
        }
    }

    public float getHeight() {
        float height = 0;
        for (int row = 0; row < getRowsRendered(); row++) {
            try {
                height += getRowAtIndex(row).get(0).getHeight();
            } catch (Exception e) {
                Log.e(TAG, "Failed to calculate height of row");
            }
        }
        return height;
    }

    private List<List<Cell>> getData() {

        List<List<Cell>> data = new ArrayList<>();

        // Header
        List<Cell> header = new ArrayList<>();
        String weekDay = DateTimeFormat.forPattern("E").print(day);
        String date = String.format("%s %s", weekDay, DateTimeUtils.dayMonth().print(day));
        Cell dateCell = new Cell(fontBold, date);
        dateCell.setWidth(LABEL_WIDTH);
        header.add(dateCell);
        for (int hour = 0; hour < DateTimeConstants.HOURS_PER_DAY; hour += HOURS_TO_SKIP) {
            Cell hourCell = new Cell(fontNormal, Integer.toString(hour));
            hourCell.setWidth(getCellWidth());
            hourCell.setFgColor(Color.gray);
            hourCell.setTextAlignment(Align.CENTER);
            header.add(hourCell);
        }
        data.add(header);

        // Data
        LinkedHashMap<Measurement.Category, float[]> values = EntryDao.getInstance().getAverageDataTable(day, PreferenceHelper.getInstance().getActiveCategories(), HOURS_TO_SKIP);
        for (Measurement.Category category : values.keySet()) {
            boolean rowIsAlternating = category.ordinal() % 2 == 0;
            int backgroundColor = rowIsAlternating ? ALTERNATING_ROW_COLOR : Color.white;
            List<Cell> cells = new ArrayList<>();

            Cell categoryCell = new Cell(fontNormal, category.toString());
            categoryCell.setBgColor(backgroundColor);
            categoryCell.setFgColor(Color.gray);
            categoryCell.setWidth(LABEL_WIDTH);
            cells.add(categoryCell);

            for (float value : values.get(category)) {
                int textColor = Color.black;
                if (category == Measurement.Category.BLOODSUGAR && PreferenceHelper.getInstance().limitsAreHighlighted()) {
                    if (value > PreferenceHelper.getInstance().getLimitHyperglycemia()) {
                        textColor = ContextCompat.getColor(DiaguardApplication.getContext(), R.color.red);
                    } else if (value < PreferenceHelper.getInstance().getLimitHypoglycemia()) {
                        textColor = ContextCompat.getColor(DiaguardApplication.getContext(), R.color.blue);
                    }
                }
                float customValue = PreferenceHelper.getInstance().formatDefaultToCustomUnit(category, value);
                String text = customValue > 0 ?
                        PreferenceHelper.getInstance().getDecimalFormat(category).format(customValue) :
                        "";
                Cell cell = new Cell(fontNormal, text);
                cell.setBgColor(backgroundColor);
                cell.setFgColor(textColor);
                cell.setWidth(getCellWidth());
                cell.setTextAlignment(Align.CENTER);
                cells.add(cell);
            }
            data.add(cells);
        }
        return data;
    }

    private float getCellWidth() {
        return (page.getWidth() - LABEL_WIDTH) / (DateTimeConstants.HOURS_PER_DAY / 2);
    }
}