
package com.roflcode.fitnessChallenge;

import com.fitbit.api.FitbitAPIException;
import com.fitbit.api.client.*;
import com.fitbit.api.common.model.activities.Activities;
import com.fitbit.api.common.model.activities.ActivitiesSummary;
import com.fitbit.api.model.FitbitUser;
import com.stackmob.core.DatastoreException;
import com.stackmob.core.InvalidSchemaException;
import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.sdkapi.*;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.*;

public class UpdateFitbitActivities implements CustomCodeMethod {


    private static LoggerService logger;

    @Override
  public String getMethodName() {
    return "update_fitbit_activities";
  }

  @Override
  public List<String> getParams() {
    return Arrays.asList("stackmob_user_id", "activity_date");
  }

  @Override
  public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
      logger = serviceProvider.getLoggerService(UpdateFitbitActivities.class);
      logger.debug("update fitbit activities ------------------------------");

      String stackmobUserID = request.getParams().get("stackmob_user_id");
      String activityDateStr = request.getParams().get("activity_date");

      if (stackmobUserID == null || stackmobUserID.isEmpty()) {
          HashMap<String, String> errParams = new HashMap<String, String>();
          errParams.put("error", "stackmobUserID was empty or null");
          logger.error("bad request, mising stackmob user ID");
          return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, errParams); // http 400 - bad request
      }

      FitbitApiClientAgent agent = AgentInitializer.GetInitializedAgent(serviceProvider, stackmobUserID);
      if (agent == null) {
          HashMap<String, String> errParams = new HashMap<String, String>();
          errParams.put("error", "could not initialize fitbit client agent");
          logger.error("could not initialize fitbit client agent");
          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errParams); // http 500 internal error;
      }

      HashMap<String, String> credentials = AgentInitializer.GetStoredFitbitCredentials(serviceProvider, stackmobUserID);
      String fitbitUserID = credentials.get("fitbituserid");
      LocalUserDetail user = new LocalUserDetail(stackmobUserID);
      FitbitUser fitbitUser = new FitbitUser(fitbitUserID);

      DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
      DateTime dt = formatter.parseDateTime(activityDateStr);
      LocalDate activityDate = new LocalDate(dt);

      Boolean updatedExisting = null;

      logger.debug("date: " + activityDate.toString());
      Activities activities;
      try {
        activities = agent.getActivities(user, fitbitUser, activityDate);
      }
      catch (FitbitAPIException ex) {
          logger.error("failed to get activities", ex);
          HashMap<String, String> errParams = new HashMap<String, String>();
          errParams.put("error", "could not get activities");
          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errParams); // http 500 internal error
      }
      SMValue activityID = null;

      ActivitiesSummary summary = activities.getSummary();
      if (summary != null) {
    //          private Integer floors = null;
    //          private Double elevation = null;
    //          private List<ActivityDistance> distances;

          DataService dataService = serviceProvider.getDataService();

          DateMidnight dateMidnight = activityDate.toDateMidnight();
          long millis = dateMidnight.getMillis();

          List<SMCondition> query;
          List<SMObject> result;

          // build a query
          query = new ArrayList<SMCondition>();
          query.add(new SMEquals("user", new SMString(stackmobUserID)));
          query.add(new SMEquals("activity_date", new SMInt(millis)));

          // execute the query
          try {
              logger.debug("looking up activities");
              logger.debug("query= username: " + stackmobUserID + " activity date millis: " + millis);

              boolean newActivity = false;
              result = dataService.readObjects("activity", query);

              SMObject activityObject;

              logger.debug("readObjects completed");
              //activity was in the datastore, so update
              if (result != null && result.size() == 1) {
                  activityObject = result.get(0);
                  List<SMUpdate> update = new ArrayList<SMUpdate>();
                  update.add(new SMSet("active_score", new SMInt((long)summary.getActiveScore())));
                  update.add(new SMSet("steps", new SMInt((long)summary.getSteps())));
                  long floors = 0;
                  if (summary.getFloors() != null) { // uh, great work there fitbit api, floors can be null
                      floors = (long) summary.getFloors();
                  }
                  update.add(new SMSet("floors", new SMInt(floors)));
                  update.add(new SMSet("sedentary_minutes", new SMInt((long)summary.getSedentaryMinutes())));
                  update.add(new SMSet("lightly_active_minutes", new SMInt((long)summary.getLightlyActiveMinutes())));
                  update.add(new SMSet("fairly_active_minutes", new SMInt((long)summary.getFairlyActiveMinutes())));
                  update.add(new SMSet("very_active_minutes", new SMInt((long)summary.getVeryActiveMinutes())));
                  SMIncrement increment = new SMIncrement("update_count", 1);
                  update.add(increment);
                  activityID = activityObject.getValue().get("activity_id");
//                  SMValue<SMInt> updateCount = activityObject.getValue().get("update_count");
//                  SMInt count = updateCount.getValue();
//                  count.
                  dataService.updateObject("activity", activityID, update);
                  logger.debug("updated object");
                  updatedExisting = true;
              }
              else {
                  Map<String, SMValue> activityMap = new HashMap<String, SMValue>();
                  activityMap.put("user", new SMString(stackmobUserID));
                  activityMap.put("activity_date", new SMInt(millis));
                  activityMap.put("activity_date_str", new SMString(activityDate.toString()));
                  activityMap.put("active_score", new SMInt((long) summary.getActiveScore()));
                  activityMap.put("steps", new SMInt((long) summary.getSteps()));
                  long floors = 0;
                  if (summary.getFloors() != null) { // uh, great work there fitbit api, floors can be null
                      floors = (long) summary.getFloors();
                  }
                  activityMap.put("floors", new SMInt(floors));
                  activityMap.put("sedentary_minutes", new SMInt((long) summary.getSedentaryMinutes()));
                  activityMap.put("lightly_active_minutes", new SMInt((long) summary.getLightlyActiveMinutes()));
                  activityMap.put("fairly_active_minutes", new SMInt((long) summary.getFairlyActiveMinutes()));
                  activityMap.put("very_active_minutes", new SMInt((long) summary.getVeryActiveMinutes()));
                  activityMap.put("update_count", new SMInt(1L));
                  activityMap.put("is_final", new SMBoolean(false));

                  activityObject = new SMObject(activityMap);
                  logger.debug("create object");
                  activityObject = dataService.createObject("activity", activityObject);
                  logger.debug("created object");
                  activityID = activityObject.getValue().get("activity_id");
                  updatedExisting = false;
              }

          } catch (InvalidSchemaException e) {
              HashMap<String, String> errMap = new HashMap<String, String>();
              errMap.put("error", "invalid_schema");
              errMap.put("detail", e.toString());
              return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
          }
          catch (DatastoreException e) {
              HashMap<String, String> errMap = new HashMap<String, String>();
              errMap.put("error", "datastore_exception");
              errMap.put("detail", e.toString());
              return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
          }
          catch(Exception e) {
              HashMap<String, String> errMap = new HashMap<String, String>();
              errMap.put("error", "unknown");
              StringWriter errors = new StringWriter();
              e.printStackTrace(new PrintWriter(errors));

              errMap.put("detail", errors.toString());
              return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
          }
      }
      Map<String, Object> returnMap = new HashMap<String, Object>();
      returnMap.put("activityID", activityID.getValue().toString());
      returnMap.put("updatedExisting", updatedExisting.toString());

      ChallengeLeaders leaders = new ChallengeLeaders();
      leaders.UpdateLeaders(stackmobUserID, serviceProvider);

      leaders.FinalizeCompletedActivities(stackmobUserID, activityID.getValue().toString(), serviceProvider);

      logger.debug("completed get activities");
      return new ResponseToProcess(HttpURLConnection.HTTP_OK, returnMap);
  }
}