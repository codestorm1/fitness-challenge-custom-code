/**
 * Copyright 2012 StackMob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.roflcode.fitnessChallenge;

import com.fitbit.api.FitbitAPIException;
import com.fitbit.api.client.FitbitApiClientAgent;
import com.fitbit.api.client.LocalUserDetail;
import com.fitbit.api.common.model.activities.Activities;
import com.fitbit.api.common.model.activities.ActivitiesSummary;
import com.fitbit.api.model.APICollectionType;
import com.fitbit.api.model.FitbitUser;
import com.fitbit.api.model.SubscriptionDetail;
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

public class AddFitbitSubscription implements CustomCodeMethod {


    private static LoggerService logger;

    @Override
  public String getMethodName() {
    return "add_fitbit_subscription";
  }

  @Override
  public List<String> getParams() {
    return Arrays.asList("stackmob_user_id");
  }


  @Override
  public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
      logger = serviceProvider.getLoggerService(AddFitbitSubscription.class);
      logger.debug("add fitbit subscription ------------------------------");

      String stackmobUserID = request.getParams().get("stackmob_user_id");
      if (stackmobUserID == null || stackmobUserID.isEmpty()) {
          HashMap<String, String> errParams = new HashMap<String, String>();
          errParams.put("error", "stackmobUserID was empty or null");
          return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, errParams); // http 400 - bad request
      }

      FitbitApiClientAgent agent = AgentInitializer.GetInitializedAgent(serviceProvider, stackmobUserID);
      if (agent == null) {
          HashMap<String, String> errParams = new HashMap<String, String>();
          errParams.put("error", "could not initialize fitbit client agent");
          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errParams); // http 500 internal error
      }

      HashMap<String, String> credentials = AgentInitializer.GetStoredFitbitCredentials(serviceProvider, stackmobUserID);
      String fitbitUserID = credentials.get("fitbituserid");
      LocalUserDetail user = new LocalUserDetail(stackmobUserID);
      FitbitUser fitbitUser = new FitbitUser(fitbitUserID);
      HashMap<String, String> errParams = new HashMap<String, String>();

      SubscriptionDetail subscription = null;
      String subscriptionID = "1";
      String modID = stackmobUserID;
      logger.debug(String.format("making request with: %s %s %s %s", modID, user, fitbitUser, subscriptionID));

//      try {
//          agent.unsubscribe("1", user, fitbitUser, APICollectionType.activities, "SM_10074");
//      }
//      catch (FitbitAPIException ex) { // swallow, we don't know if there was a subscription to remove
//      }

      try {
          subscription = agent.nullSafeSubscribeLog(logger, "1", user, fitbitUser, APICollectionType.activities, modID);
      }
      catch (FitbitAPIException ex) {
          int code = ex.getStatusCode();
          logger.error("failed to add subscription", ex);
          if (code == 409) {
              errParams.put("error", "could not add subscriptions - may have already been created");
              return new ResponseToProcess(HttpURLConnection.HTTP_CONFLICT, errParams); // http 409 conflict
          }
          errParams.put("error", "could not add subscription");
          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errParams); // http 500 internal error
      }

      if (subscription == null) {
        errParams.put("error", "null sub");
        return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errParams); // http 500 internal error
      }

      Map<String, Object> returnMap = new HashMap<String, Object>();
      returnMap.put("subscriptionID", subscription.getSubscriptionId());
      returnMap.put("owner", subscription.getOwner().getId());
      returnMap.put("collectionType", subscription.getCollectionType().toString());
      returnMap.put("subscriberID", subscription.getSubscriberId());

      logger.debug("completed add subscription");
      return new ResponseToProcess(HttpURLConnection.HTTP_OK, returnMap);
  }
}