package com.faltenreich.diaguard.shared.data.repository;

import android.content.Context;

import androidx.annotation.Nullable;

import com.faltenreich.diaguard.feature.food.networking.OpenFoodFactsService;
import com.faltenreich.diaguard.feature.food.search.FoodSearchListItem;
import com.faltenreich.diaguard.feature.preference.data.PreferenceStore;
import com.faltenreich.diaguard.shared.data.async.DataCallback;
import com.faltenreich.diaguard.shared.data.database.dao.FoodDao;
import com.faltenreich.diaguard.shared.data.database.dao.FoodEatenDao;
import com.faltenreich.diaguard.shared.data.database.entity.Food;
import com.faltenreich.diaguard.shared.data.database.entity.FoodEaten;
import com.faltenreich.diaguard.shared.data.primitive.StringUtils;
import com.faltenreich.diaguard.shared.networking.NetworkingUtils;

import java.util.ArrayList;
import java.util.List;

public class FoodRepository {

    private static FoodRepository instance;

    public static FoodRepository getInstance() {
        if (instance == null) {
            instance = new FoodRepository();
        }
        return instance;
    }

    private FoodRepository() {

    }

    public void search(Context context, @Nullable String query, int page, DataCallback<List<FoodSearchListItem>> callback) {
        if (page == 0
            && !StringUtils.isBlank(query)
            && PreferenceStore.getInstance().showBrandedFood()
            && NetworkingUtils.isOnline(context)
        ) {
            // TODO: How to page efficiently through api?
            searchOnline(query, page, result -> searchOffline(query, page, callback));
        } else {
            searchOffline(query, page, callback);
        }
    }

    private void searchOnline(@Nullable String query, int page, DataCallback<List<Food>> callback) {
        OpenFoodFactsService.getInstance().search(query, page, (dto) -> {
            List<Food> foodList = FoodDao.getInstance().createOrUpdate(dto);
            callback.onResult(foodList);
        });
    }

    private void searchOffline(@Nullable String query, int page, DataCallback<List<FoodSearchListItem>> callback) {
        List<FoodSearchListItem> items = new ArrayList<>();

        boolean showCustomFood = PreferenceStore.getInstance().showCustomFood();
        boolean showCommonFood = PreferenceStore.getInstance().showCommonFood();

        boolean includeFoodEaten = page == 0 && !(query != null && query.length() > 0);
        if (includeFoodEaten) {
            List<FoodEaten> foodEatenList = FoodEatenDao.getInstance().getAllOrdered();
            for (FoodEaten foodEaten : foodEatenList) {
                FoodSearchListItem item = new FoodSearchListItem(foodEaten);
                // TODO: Filter distinct on database level
                if (!items.contains(item)) {
                    items.add(item);
                }
            }
        }

        List<Food> foodList = FoodDao.getInstance().search(query, page, showCustomFood, showCommonFood);
        for (Food food : foodList) {
            items.add(new FoodSearchListItem(food));
        }

        callback.onResult(items);
    }
}
