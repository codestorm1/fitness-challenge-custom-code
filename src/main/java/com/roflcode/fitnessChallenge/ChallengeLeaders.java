package com.roflcode.fitnessChallenge;

import com.stackmob.core.DatastoreException;
import com.stackmob.core.InvalidSchemaException;
import com.stackmob.sdkapi.*;

import java.util.ArrayList;
import java.util.List;

public class ChallengeLeaders {

    public void UpdateLeaders(String stackmobUserID, SDKServiceProvider sdkServiceProvider) {

        LoggerService logger = sdkServiceProvider.getLoggerService(UpdateChallengeLeaders.class);
        logger.debug("in update leaders ------------------------------");

        DataService ds = sdkServiceProvider.getDataService();
        List<SMCondition> query = new ArrayList<SMCondition>();
        query.add(new SMEquals("username", new SMString(stackmobUserID)));
        query.add(new SMEquals("is_active", new SMBoolean(true)));

        List<SMObject> leaders = null;
        BulkResult result;
        try {
//            result = ds.readObjects("user",query, 1);
//            result = ds.readObjects("user", query, 1);
            leaders = ds.readObjects("leader", query, 1);
        } catch (InvalidSchemaException e) {
            e.printStackTrace();
        } catch (DatastoreException e) {
            e.printStackTrace();
        }
        if (leaders == null) {
            return;
        }

        for (SMObject leader: leaders) {

            SMValue smLeaderID = leader.getValue().get("leader_id");
            String leaderID = ((SMString)smLeaderID).getValue();
            logger.debug("Leader: " +leaderID);
//            SMValue challenge = leader.getValue().get("challenge");
//            activityObject = result.get(0);
//            activityId = activityObject.getValue().get("activity_id");
//            logger.debug("update object");
//            dataService.updateObject("activity", activityId, update);
        }

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
