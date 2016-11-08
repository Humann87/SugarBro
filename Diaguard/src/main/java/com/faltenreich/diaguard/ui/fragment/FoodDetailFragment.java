package com.faltenreich.diaguard.ui.fragment;

import android.view.ViewGroup;
import android.widget.TextView;

import com.faltenreich.diaguard.R;
import com.faltenreich.diaguard.data.PreferenceHelper;
import com.faltenreich.diaguard.data.entity.Food;
import com.faltenreich.diaguard.data.entity.Measurement;
import com.faltenreich.diaguard.ui.view.FoodLabelView;
import com.faltenreich.diaguard.util.Helper;

import butterknife.BindView;

/**
 * Created by Faltenreich on 28.10.2016.
 */

public class FoodDetailFragment extends BaseFoodFragment {

    @BindView(R.id.food_brand) TextView brand;
    @BindView(R.id.food_ingredients) TextView ingredients;
    @BindView(R.id.food_value) TextView value;
    @BindView(R.id.food_labels) ViewGroup labels;

    public FoodDetailFragment() {
        super(R.layout.fragment_food_detail, R.string.info, R.drawable.ic_category_meal);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        Food food = getFood();
        if (food != null) {
            brand.setText(food.getBrand());
            ingredients.setText(food.getIngredients());

            float mealValue = PreferenceHelper.getInstance().formatDefaultToCustomUnit(
                    Measurement.Category.MEAL,
                    food.getCarbohydrates());
            String mealUnit = PreferenceHelper.getInstance().getUnitAcronym(Measurement.Category.MEAL);
            value.setText(String.format("%s %s %s 100g", Helper.parseFloat(mealValue), mealUnit, getString(R.string.per)));

            if (food.getSugarLevel() != null) {
                switch (food.getSugarLevel()) {
                    case MODERATE:
                        labels.addView(new FoodLabelView(
                                getContext(),
                                getString(R.string.sugar_level_moderate),
                                FoodLabelView.Type.WARNING,
                                R.drawable.ic_error));
                        break;
                    case HIGH:
                        labels.addView(new FoodLabelView(
                                getContext(),
                                getString(R.string.sugar_level_high),
                                FoodLabelView.Type.ERROR,
                                R.drawable.ic_error));
                        break;
                    default:
                }
            }

            if (food.getCarbohydrates() > 0) {
                // TODO: Determine quick- and slow-acting sugar
                if (false) {
                    labels.addView(new FoodLabelView(
                            getContext(),
                            getString(R.string.acting_quick),
                            FoodLabelView.Type.ERROR,
                            R.drawable.ic_error));
                } else if (false) {
                    labels.addView(new FoodLabelView(
                            getContext(),
                            getString(R.string.acting_slow),
                            FoodLabelView.Type.ERROR,
                            R.drawable.ic_error));
                }
            }

            if (food.getLabels() != null && food.getLabels().length() > 0) {
                for (String label : food.getLabels().split(",")) {
                    labels.addView(new FoodLabelView(getContext(), label));
                }
            }
        }
    }
}