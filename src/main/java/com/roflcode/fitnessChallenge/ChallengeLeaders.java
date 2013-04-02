package com.roflcode.fitnessChallenge;

import com.stackmob.core.DatastoreException;
import com.stackmob.core.InvalidSchemaException;
import com.stackmob.sdkapi.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChallengeLeaders {

    public String UpdateLeaders(String stackmobUserID, SDKServiceProvider sdkServiceProvider) {

        LoggerService logger = sdkServiceProvider.getLoggerService(UpdateChallengeLeaders.class);
        logger.debug("in update leaders ------------------------------");


        DataService ds = sdkServiceProvider.getDataService();
        List<SMCondition> query = new ArrayList<SMCondition>();
        query.add(new SMEquals("user", new SMString(stackmobUserID)));
        query.add(new SMEquals("is_active", new SMBoolean(true)));

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
}
