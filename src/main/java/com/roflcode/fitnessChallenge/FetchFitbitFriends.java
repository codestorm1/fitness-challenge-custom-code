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
import com.fitbit.api.client.*;
import com.fitbit.api.client.http.AccessToken;
import com.fitbit.api.client.service.FitbitAPIClientService;
import com.fitbit.api.model.APIResourceCredentials;
import com.fitbit.api.model.FitbitUser;

import com.stackmob.core.DatastoreException;
import com.stackmob.core.InvalidSchemaException;
import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.sdkapi.*;

import java.net.HttpURLConnection;
import java.util.*;


public class FetchFitbitFriends implements CustomCodeMethod {


    private static final String fitbitSiteBaseUrl = "http://www.fitbit.com";
    private static final String apiBaseUrl = "api.fitbit.com";
    private static final String devFitnessChallengeBaseUrl = "http://localhost:4567";
    private static final String prodFitnessChallengeBaseUrl = "http://fitnesschallenge.twistedogregmailcom.stackmobapp.com";
    private static final String clientConsumerKey = "c472de74290e435caa6b6829ff68b9aa";
    private static final String clientSecret = "312f9d3f9d10418bb43938103a73d5fe";
    private static LoggerService logger;
//    private static final FitbitAPIClientService<FitbitApiClientAgent> apiClientService = new FitbitAPIClientService<FitbitApiClientAgent>(
//            new FitbitApiClientAgent(apiBaseUrl, fitnessChallengeBaseUrl, credentialsCache),
//    clientConsumerKey,
//    clientSecret,
//    credentialsCache,
//    entityCache,
//    subscriptionStore
//    );


    @Override
  public String getMethodName() {
    return "fetch_fitbit_friends";
  }

  @Override
  public List<String> getParams() {
    return Arrays.asList("stackmob_user_id");
  }


  @Override
  public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
      logger = serviceProvider.getLoggerService(FetchFitbitFriends.class);
      logger.debug("get fitbit friends ------------------------------");

      Map<String, Object> map = new HashMap<String, Object>();

      Map<String,String> params = request.getParams();
      String stackmobUserID = request.getParams().get("stackmob_user_id");

      if (stackmobUserID == null || stackmobUserID.isEmpty()) {
          HashMap<String, String> errParams = new HashMap<String, String>();
          errParams.put("error", "stackmobUserID was empty or null");
          return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, errParams); // http 400 - bad request
      }

      String accessToken;
      String tokenSecret;
      String fitbitUserID;

      try {
          // get the datastore service and assemble the query
          DataService dataService = serviceProvider.getDataService();

          // build a query
          List<SMCondition> query = new ArrayList<SMCondition>();
          query.add(new SMEquals("username", new SMString(stackmobUserID)));

          // execute the query
          List<SMObject> result;

          result = dataService.readObjects("user", query);

          SMObject userObject;

          if (result != null && result.size() == 1) {
              userObject = result.get(0);
          } else {
              HashMap<String, String> errParams = new HashMap<String, String>();
              errParams.put("error", "no user found with given stackmobUserID");
              return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, errParams); // http 400 - bad request
          }


          SMValue smAccess = userObject.getValue().get("accesstoken");
          accessToken = ((SMString)smAccess).getValue();
          SMValue smSecret = userObject.getValue().get("accesstokensecret");
          tokenSecret = ((SMString)smSecret).getValue();
          SMValue smFitbitUserID = userObject.getValue().get("fitbituserid");
          fitbitUserID = ((SMString)smFitbitUserID).getValue();

      } catch (InvalidSchemaException e) {
          HashMap<String, String> errMap = new HashMap<String, String>();
          errMap.put("error", "invalid_schema");
          errMap.put("detail", e.toString());
          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
      } catch (DatastoreException e) {
          HashMap<String, String> errMap = new HashMap<String, String>();
          errMap.put("error", "datastore_exception");
          errMap.put("detail", e.toString());
          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
      } catch(Exception e) {
          HashMap<String, String> errMap = new HashMap<String, String>();
          errMap.put("error", "unknown");
          errMap.put("detail", e.toString());
          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
      }

      FitbitAPIClientService service = null;
      FitbitApiClientAgent agent = null;

      try {
          FitbitApiCredentialsCache credCache = new FitbitApiCredentialsCacheMapImpl();
          FitbitApiEntityCacheMapImpl entityCache = new FitbitApiEntityCacheMapImpl();
          FitbitApiSubscriptionStorageInMemoryImpl subscriptionStore = new FitbitApiSubscriptionStorageInMemoryImpl();
          String fitnessCallback = serviceProvider.isSandbox() ? devFitnessChallengeBaseUrl : prodFitnessChallengeBaseUrl;
          Boolean isSand = serviceProvider.isSandbox();
          logger.debug("is sandbox? " + isSand.toString() + "callback is " + fitnessCallback);
          agent = new FitbitApiClientAgent(apiBaseUrl, fitnessCallback, credCache, serviceProvider);
          service = new FitbitAPIClientService(agent, clientConsumerKey, clientSecret,
                  credCache, entityCache, subscriptionStore); // need this call to set oauth internally in the service

          LocalUserDetail user = new LocalUserDetail(stackmobUserID);
          APIResourceCredentials cred = credCache.getResourceCredentials(user);
          if (cred == null) {
              cred = new APIResourceCredentials(stackmobUserID, accessToken, tokenSecret);
              logger.warn("no existing creds");
          }
          else {
              logger.debug("loaded existing creds");
          }
          cred.setAccessToken(accessToken);
          cred.setAccessTokenSecret(tokenSecret);
          credCache.saveResourceCredentials(user, cred);

          logger.debug(String.format("fitbit ID %s access token: %s secret: %s", fitbitUserID, cred.getAccessToken(), cred.getAccessTokenSecret()));
          //agent = new FitbitApiClientAgent(apiBaseUrl, fitnessChallengeBaseUrl, credCache);
          logger.debug("trying get friends");
          agent.getCredentialsCache().saveResourceCredentials(user, cred);
          FitbitUser fitbitUser = new FitbitUser(fitbitUserID);

          agent.setOAuthAccessToken(accessToken, tokenSecret);
          String userFriendsJson = agent.getFriendsJson(fitbitUser);

          logger.debug("completed get friends json");
          map.put("friendsJson", userFriendsJson);
          return new ResponseToProcess(HttpURLConnection.HTTP_OK, map);
      }
      catch (FitbitAPIException e) {
          logger.debug("fitbit exception getting user info ", e);
          map.put("error", e.toString() + "---" + e.getMessage());
          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, map);
      }
      catch (Exception e) {
          logger.debug("exception getting user info ", e);
          map.put("error", e.toString() + "---" + e.getMessage());
          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, map);
      }

  }
}