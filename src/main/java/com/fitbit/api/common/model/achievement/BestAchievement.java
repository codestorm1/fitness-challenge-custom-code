package com.fitbit.api.common.model.achievement;

import com.fitbit.api.common.service.FitbitApiService;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: Kiryl
 * Date: 10/18/11
 * Time: 12:56 AM
 */
public class BestAchievement {

    private BestAchievementItem caloriesOut;
    private BestAchievementItem floors;
    private BestAchievementItem distance;
    private BestAchievementItem steps;
    private BestAchievementItem activeScore;

    public BestAchievement(BestAchievementItem caloriesOut, BestAchievementItem floors, BestAchievementItem distance, BestAchievementItem steps, BestAchievementItem activeScore) {
        this.caloriesOut = caloriesOut;
        this.floors = floors;
        this.distance = distance;
        this.steps = steps;
        this.activeScore = activeScore;
    }

    public BestAchievement(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has("caloriesOut")) {
            this.caloriesOut = new BestAchievementItem(jsonObject.getJSONObject("caloriesOut").getDouble("value"), FitbitApiService.LOCAL_DATE_FORMATTER.parseDateTime(jsonObject.getJSONObject("caloriesOut").getString("date")).toLocalDate());
        }
        if (jsonObject.has("floors")) {
            this.floors = new BestAchievementItem(jsonObject.getJSONObject("floors").getDouble("value"), FitbitApiService.LOCAL_DATE_FORMATTER.parseDateTime(jsonObject.getJSONObject("floors").getString("date")).toLocalDate());
        }
        if (jsonObject.has("distance")) {
            this.distance = new BestAchievementItem(jsonObject.getJSONObject("distance").getDouble("value"), FitbitApiService.LOCAL_DATE_FORMATTER.parseDateTime(jsonObject.getJSONObject("distance").getString("date")).toLocalDate());
        }
        if (jsonObject.has("steps")) {
            this.steps = new BestAchievementItem(jsonObject.getJSONObject("steps").getDouble("value"), FitbitApiService.LOCAL_DATE_FORMATTER.parseDateTime(jsonObject.getJSONObject("steps").getString("date")).toLocalDate());
        }
        if (jsonObject.has("activeScore")) {
            this.activeScore = new BestAchievementItem(jsonObject.getJSONObject("activeScore").getDouble("value"), FitbitApiService.LOCAL_DATE_FORMATTER.parseDateTime(jsonObject.getJSONObject("activeScore").getString("date")).toLocalDate());
        }
    }

    public BestAchievementItem getCaloriesOut() {
        return caloriesOut;
    }

    public BestAchievementItem getFloors() {
        return floors;
    }

    public BestAchievementItem getDistance() {
        return distance;
    }

    public BestAchievementItem getSteps() {
        return steps;
    }

    public BestAchievementItem getActiveScore() {
        return activeScore;
    }
}
