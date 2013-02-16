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
import com.fitbit.api.common.model.user.UserInfo;
import com.fitbit.api.model.APIResourceCredentials;
import com.fitbit.api.model.FitbitUser;
import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.sdkapi.LoggerService;
import com.stackmob.sdkapi.SDKServiceProvider;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FetchFitbitUser implements CustomCodeMethod {


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
    return "fetch_fitbit_user";
  }

  @Override
  public List<String> getParams() {
    return Arrays.asList("access_token", "access_token_secret", "fitbit_user_id", "stackmob_user_id");
  }


  @Override
  public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
      logger = serviceProvider.getLoggerService(FetchFitbitUser.class);
      logger.debug("create fitbit user ------------------------------");
      Map<String, Object> map = new HashMap<String, Object>();

      Map<String,String> params = request.getParams();
      String accessToken = params.get("access_token");
      String accessTokenSecret = params.get("access_token_secret");
      String fitbitUserID = params.get("fitbit_user_id");
      String stackmobUserID = request.getParams().get("stackmob_user_id");
      logger.debug(String.format("%s %s %s %s", accessToken, accessTokenSecret, fitbitUserID, stackmobUserID));
      //APIResourceCredentials resourceCredentials = context.getApiClientService().getResourceCredentialsByTempToken(tempTokenReceived);
      // The verifier is required in the request to get token credentials:
      String progress = null;
      FitbitAPIClientService service = null;
      FitbitApiClientAgent agent = null;

      try {
          FitbitApiCredentialsCache credCache = new FitbitApiCredentialsCacheMapImpl();
          FitbitApiEntityCacheMapImpl entityCache = new FitbitApiEntityCacheMapImpl();
          FitbitApiSubscriptionStorageInMemoryImpl subscriptionStore = new FitbitApiSubscriptionStorageInMemoryImpl();
          String fitnessCallback = serviceProvider.isSandbox() ? devFitnessChallengeBaseUrl : prodFitnessChallengeBaseUrl;
          agent = new FitbitApiClientAgent(apiBaseUrl, fitnessCallback, credCache, serviceProvider);
          service = new FitbitAPIClientService(agent, clientConsumerKey, clientSecret,
                  credCache, entityCache, subscriptionStore); // need this call to set oauth internally in the service

          LocalUserDetail user = new LocalUserDetail(stackmobUserID);
          APIResourceCredentials cred = credCache.getResourceCredentials(user);
          if (cred == null) {
              cred = new APIResourceCredentials(stackmobUserID, accessToken, accessTokenSecret);
              logger.warn("no existing creds");
          }
          else {
              logger.debug("loaded existing creds");
          }
          cred.setAccessToken(accessToken);
          cred.setAccessTokenSecret(accessTokenSecret);
          credCache.saveResourceCredentials(user, cred);

          // FitbitApiEntityCacheMapImpl entityCache = new FitbitApiEntityCacheMapImpl();
          // FitbitApiSubscriptionStorageInMemoryImpl subscriptionStore = new FitbitApiSubscriptionStorageInMemoryImpl();
          //cred = credCache.getResourceCredentials(user);
          //logger.debug(String.format("request token: %s secret: %s verifier: %s", savedCred.getTempToken(), savedCred.getTempTokenSecret(), savedCred.getTempTokenVerifier()));
          logger.debug(String.format("access token: %s secret: %s", cred.getAccessToken(), cred.getAccessTokenSecret()));
          //agent = new FitbitApiClientAgent(apiBaseUrl, fitnessChallengeBaseUrl, credCache);
          logger.debug("trying get user info");
          agent.getCredentialsCache().saveResourceCredentials(user, cred);
          FitbitUser fitbitUser = new FitbitUser(fitbitUserID);

          agent.setOAuthAccessToken(accessToken, accessTokenSecret);
          //String userInfoJson = agent.getUserInfoJsonSM(fitbitUser, new AccessToken(accessToken, accessTokenSecret), serviceProvider); TODO: PUT ME BACK
          String userInfoJson = agent.getUserInfoJSON(user, fitbitUser);

          logger.debug("completed get user info");
          map.put("userInfoJson", userInfoJson);
          return new ResponseToProcess(HttpURLConnection.HTTP_OK, map);
//          service = new FitbitAPIClientService(agent, clientConsumerKey, clientSecret,
//                  credCache, entityCache, subscriptionStore);
      }
      catch (FitbitAPIException e) {
          logger.debug("fitbit exception getting user info ", e);
          map.put("error", e.toString() + "---" + e.getMessage());
          map.put("progress", progress);
          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, map);
      }
      catch (Exception e) {
          logger.debug("exception getting user info ", e);
          map.put("error", e.toString() + "---" + e.getMessage());
          map.put("progress", progress);
          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, map);
      }
  }

}
