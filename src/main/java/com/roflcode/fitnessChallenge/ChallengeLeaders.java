package com.roflcode.fitnessChallenge;

import com.stackmob.core.DatastoreException;
import com.stackmob.core.InvalidSchemaException;
import com.stackmob.sdkapi.*;
import org.joda.time.DateMidnight;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChallengeLeaders {

    static final long millisecondsInADay = 1000 * 60 * 60 * 24;
    static final int minUpdatesToFinalizeOlderActivities = 20; // if 5 activity updates have occurred today, consider all past activity dates final
                                                              // after 5 updates today, probably no staggling updates from previous days will come in

    public String UpdateLeaders(String stackmobUserID, SDKServiceProvider sdkServiceProvider) {

        LoggerService logger = sdkServiceProvider.getLoggerService(UpdateChallengeLeaders.class);
        logger.debug("in update leaders ------------------------------");

        DataService ds = sdkServiceProvider.getDataService();
        List<SMCondition> query = new ArrayList<SMCondition>();
        query.add(new SMEquals("user", new SMString(stackmobUserID)));
        query.add(new SMEquals("is_final", new SMBoolean(false)));

        List<SMObject> leaders = null;
        BulkResult result;
        try {
//            result = ds.readObjects("user",query, 1);
//            result = ds.readObjects("user", query, 1);
            leaders = ds.readObjects("leader", query, 1);
        } catch (InvalidSchemaException e) {
            logger.debug("invalid schema exception: " + e.getLocalizedMessage());
            e.printStackTrace();
        } catch (DatastoreException e) {
            logger.debug("datastore exception: " + e.getLocalizedMessage());
            e.printStackTrace();
        }

        if (leaders == null || leaders.isEmpty()) {
            logger.debug("no leaders returned");
            return "no leaders returned";
        }

        logger.debug("got leaders");
        Integer count = 0;
        int totalSteps = 0;
        for (SMObject leader: leaders) {
            SMValue smLeaderID = leader.getValue().get("leader_id");
            String leaderID = ((SMString)smLeaderID).getValue();
            logger.debug("Leader: " + leaderID);
            SMValue smChallenge = leader.getValue().get("challenge");
            HashMap<String, Object>challengeObject = (HashMap<String,Object>)smChallenge.getValue();
            if (challengeObject == null || challengeObject.isEmpty()) {
                return "empty challenge found for leader " + leaderID;
            }
            SMValue smStartDate = (SMValue)challengeObject.get("startdate");
            Long startDateInt = (Long)smStartDate.getValue();
            SMValue smEndDate = (SMValue)challengeObject.get("enddate");
            Long endDateInt = (Long)smEndDate.getValue();
            count++;
            logger.debug("got challenge: " + startDateInt.toString() + "-" + endDateInt.toString());

            query = new ArrayList<SMCondition>();
            query.add(new SMEquals("user", new SMString(stackmobUserID)));
            query.add(new SMGreaterOrEqual("activity_date", smStartDate));
            query.add(new SMLessOrEqual("activity_date", smEndDate));

            List<SMObject> activities = null;
            try {
                activities = ds.readObjects("activity", query);
            } catch (InvalidSchemaException e) {
                logger.debug("invalid schema exception: " + e.getLocalizedMessage());
                e.printStackTrace();
                continue;
            } catch (DatastoreException e) {
                logger.debug("datastore exception: " + e.getLocalizedMessage());
                e.printStackTrace();
                continue;
            }

            if (activities == null || activities.isEmpty()) {
                logger.debug("no activities returned");
                continue;
                //return "no activities returned";
            }

            logger.debug("got activities");
            long steps = 0;
            int activityCount = 0;
            for (SMObject activity: activities) {

                //SMValue smLeaderID = leader.getValue().get("leader_id");
                //String leaderID = ((SMString)smLeaderID).getValue();
                SMValue activitySteps = activity.getValue().get("steps");
                if (activitySteps != null) {
                    steps += ((SMInt)activitySteps).getValue();
                    activityCount++;
                }
            }

            List<SMUpdate> update = new ArrayList<SMUpdate>();
            update.add(new SMSet("value_int", new SMInt(steps)));

            try {
                ds.updateObject("leader", leaderID, update);
            } catch (InvalidSchemaException e) {
                e.printStackTrace();
                //return "schema exception on leader update";
                continue;

            } catch (DatastoreException e) {
                e.printStackTrace();
                //return "datastore exception on leader update";
                continue;
            }
            totalSteps += steps;
            //dataService.updateObject("activity", activityId, update);
//            SMValue challenge = leader.getValue().get("challenge");
//            activityObject = result.get(0);
//            activityId = activityObject.getValue().get("activity_id");
//            logger.debug("update object");
//            dataService.updateObject("activity", activityId, update);
        }
        return "updated " + count.toString() + " leaders with a total of " + totalSteps + " steps";

//        List<SMUpdate> update = new ArrayList<SMUpdate>();
//        update.add(new SMSet("age", new SMInt(26L)));
//        try {
//            SMObject result = ds.updateObject("user", new SMString("johndoe"), update);
//        } catch (InvalidSchemaException e) {
//            e.printStackTrace();
//        } catch (DatastoreException e) {
//            e.printStackTrace();
//        }
    }

    public void FinalizeCompletedChallenges(String stackmobUserID, SDKServiceProvider sdkServiceProvider) {

    }

    public void FinalizeCompletedLeaders(String stackmobUserID, SDKServiceProvider sdkServiceProvider) {

        LoggerService logger = sdkServiceProvider.getLoggerService(ChallengeLeaders.class);
        logger.debug("check finished challenges ------------------------------");

        DataService ds = sdkServiceProvider.getDataService();

        LocalDate nowLocal = new LocalDate();
        DateMidnight dateMidnight = nowLocal.toDateMidnight();
        long milliMidnight = dateMidnight.getMillis();


        List<SMCondition> query = new ArrayList<SMCondition>();
        query.add(new SMEquals("user", new SMString(stackmobUserID)));
        query.add(new SMEquals("is_final", new SMBoolean(false)));

        List<SMObject> leaders = null;
        try {
//            result = ds.readObjects("user",query, 1);
//            result = ds.readObjects("user", query, 1);
            leaders = ds.readObjects("leader", query, 1);
        } catch (InvalidSchemaException e) {
            logger.debug("invalid schema exception: " + e.getLocalizedMessage());
            e.printStackTrace();
        } catch (DatastoreException e) {
            logger.debug("datastore exception: " + e.getLocalizedMessage());
            e.printStackTrace();
        }

        if (leaders == null || leaders.isEmpty()) {
            logger.debug("no leaders returned");
            return;
        }
        logger.debug("got leaders");

        int count = 0;
        for (SMObject leader: leaders) {
            SMValue smLeaderID = leader.getValue().get("leader_id");
            String leaderID = ((SMString)smLeaderID).getValue();
            logger.debug("Leader: " + leaderID);
            SMValue smChallenge = leader.getValue().get("challenge");
            HashMap<String, Object>challengeObject = (HashMap<String,Object>)smChallenge.getValue();
            if (challengeObject == null || challengeObject.isEmpty()) {
                logger.debug("empty challenge found for leader " + leaderID);
                return;
            }
//            SMValue smStartDate = (SMValue)challengeObject.get("startdate");
//            Long startDateInt = (Long)smStartDate.getValue();
            SMValue smEndDate = (SMValue)challengeObject.get("enddate");
            Long endDateInt = (Long)smEndDate.getValue();
            if (endDateInt < milliMidnight) { // leader board entry is in the past and still not finalized
                query = new ArrayList<SMCondition>();
                query.add(new SMEquals("user", new SMString(stackmobUserID)));
                query.add(new SMEquals("is_final", new SMBoolean(false)));
                query.add(new SMLess("activity_date", new SMInt(milliMidnight)));

                List<SMObject> activities = null;
                try {
                    activities = ds.readObjects("activity", query);
                } catch (InvalidSchemaException e) {
                    logger.debug("invalid schema exception: " + e.getLocalizedMessage());
                    e.printStackTrace();
                } catch (DatastoreException e) {
                    logger.debug("datastore exception: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }

                if (activities == null || activities.isEmpty()) { // no results is good, we can finalize the leader board entry
                    logger.debug("no unfinalized activities for this leaderboard entry: finalizing this leader");
                    List<SMUpdate> update = new ArrayList<SMUpdate>();
                    update.add(new SMSet("is_final", new SMBoolean(true)));
                    try {
                        ds.updateObject("leader", leaderID, update);
                    } catch (InvalidSchemaException e) {
                        e.printStackTrace();
                        //return "schema exception on leader update";
                        continue;

                    } catch (DatastoreException e) {
                        e.printStackTrace();
                        //return "datastore exception on leader update";
                        continue;
                    }
                    logger.debug("finalize completed");
                    FinalizeCompletedChallenges(stackmobUserID, sdkServiceProvider);
                }
                else {
                    logger.debug("no past unfinalized activties found");
                }

            }
            count++;
//            logger.debug("got challenge: " + startDateInt.toString() + "-" + endDateInt.toString());
        }
    }

    // finalize any activity days where no more updates are expected.  The main criteria for this is that activities are a couple days old,
    // or one day old and at least a few updates came in today
    public void FinalizeCompletedActivities(String stackmobUserID, String activityID, SDKServiceProvider sdkServiceProvider) {

        LoggerService logger = sdkServiceProvider.getLoggerService(ChallengeLeaders.class);
        logger.debug("check finished activities ------------------------------");

        DataService ds = sdkServiceProvider.getDataService();

        List<SMCondition> query = new ArrayList<SMCondition>();
        query.add(new SMEquals("activity_id", new SMString(activityID)));

        List<SMObject> activities = null;
        try {
            activities = ds.readObjects("activity", query);
        } catch (InvalidSchemaException e) {
            logger.debug("invalid schema exception: " + e.getLocalizedMessage());
            e.printStackTrace();
        } catch (DatastoreException e) {
            logger.debug("datastore exception: " + e.getLocalizedMessage());
            e.printStackTrace();
        }

        if (activities == null || activities.isEmpty()) {
            logger.debug("no activities found");
            return;
        }
        SMObject activityObject = activities.get(0);
        if (activityObject == null) {
            logger.debug("no activity found");
            return;
        }

        SMInt activityDateValue = (SMInt)activityObject.getValue().get("activity_date");
        //SMInt currentActivityDateValue = (SMInt)activityDateValue.getValue();
        long currentActivityDate = activityDateValue.getValue();
        SMInt updateCountSMInt = (SMInt)activityObject.getValue().get("update_count");
        //SMInt updateCountSMInt = (SMInt)updateCountValue.getValue();
        long updateCount = updateCountSMInt.getValue();
        query = new ArrayList<SMCondition>();
        query.add(new SMEquals("user", new SMString(stackmobUserID)));
        query.add(new SMEquals("is_final", new SMBoolean(false)));
        query.add(new SMLess("activity_date", activityDateValue));

        activities = null;
        try {
            activities = ds.readObjects("activity", query);
        } catch (InvalidSchemaException e) {
            logger.debug("invalid schema exception: " + e.getLocalizedMessage());
            e.printStackTrace();
        } catch (DatastoreException e) {
            logger.debug("datastore exception: " + e.getLocalizedMessage());
            e.printStackTrace();
        }

        if (activities == null || activities.isEmpty()) {
            logger.debug("no past activities returned");
            return;
        }
        logger.debug("got past activities");

        int count = 0;
        for (SMObject activity: activities) {
            //activityDateValue = (SMInt)activity.getValue().get("activity_date");
            SMInt activityDateSMInt = (SMInt)activity.getValue().get("activity_date");
            long activityDate = activityDateSMInt.getValue();
            if (currentActivityDate - activityDate > millisecondsInADay || updateCount >= minUpdatesToFinalizeOlderActivities) {
                // today's activity has had updates, so finalize any activities that are at least two days old,
                // or if we'd have a few updates today, finalize yesterday's as well.  This assumes that if a few updates came in today,
                // that probably no more will come in for yesterday
                List<SMUpdate> update = new ArrayList<SMUpdate>();
                update.add(new SMSet("is_final", new SMBoolean(true)));
                try {
                    ds.updateObject("activity", activity, update);
                    logger.debug("finalized activity for " + activityDate);
                } catch (InvalidSchemaException e) {
                    logger.debug("invalid schema exception: " + e.getLocalizedMessage());
                    e.printStackTrace();
                } catch (DatastoreException e) {
                    logger.debug("datastore exception: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
                FinalizeCompletedLeaders(stackmobUserID, sdkServiceProvider);
            }
            logger.debug("completed finalizing activities");
        }
    }
}
