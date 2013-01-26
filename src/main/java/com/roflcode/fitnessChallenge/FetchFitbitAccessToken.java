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


public class FetchFitbitAccessToken implements CustomCodeMethod {


    private static final String fitbitSiteBaseUrl = "http://www.fitbit.com";
    private static final String apiBaseUrl = "api.fitbit.com";
    private static final String fitnessChallengeBaseUrl = "http://localhost:4567";
    //private static final String fitnessChallengeBaseUrl = "http://fitnesschallenge.twistedogregmailcom.stackmobapp.com";
    private static final String clientConsumerKey = "c472de74290e435caa6b6829ff68b9aa";
    private static final String clientSecret = "312f9d3f9d10418bb43938103a73d5fe";
    private static LoggerService logger;

    @Override
  public String getMethodName() {
    return "fetch_fitbit_access_token";
  }

  @Override
  public List<String> getParams() {
      return Arrays.asList("request_token", "request_token_secret", "oauth_verifier");
  }

  @Override
  public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
      logger = serviceProvider.getLoggerService(FetchFitbitAccessToken.class);
      String tempTokenReceived = request.getParams().get("request_token");
      String tempTokenSecret = request.getParams().get("request_token_secret");
      String tempTokenVerifier = request.getParams().get("oauth_verifier");


      logger.debug(String.format("Fetching access token using token: %s secret: %s verifier: %s", tempTokenReceived, tempTokenSecret, tempTokenVerifier));

      Map<String, Object> map = new HashMap<String, Object>();
      if (tempTokenReceived == null) {
          map.put("error", "couldn't read request_token :(");
          return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, map);
      }

      for ( Map.Entry<String, String> entry : request.getParams().entrySet() ) {
          String key = entry.getKey();
          String value = entry.getValue();
          logger.debug(key + ": " + value);
      }

      FitbitApiClientAgent agent;
      try {
//          FitbitApiCredentialsCache credCache = new FitbitApiCredentialsCacheMapImpl();
//          FitbitApiEntityCacheMapImpl entityCache = new FitbitApiEntityCacheMapImpl();
//          FitbitApiSubscriptionStorageInMemoryImpl subscriptionStore = new FitbitApiSubscriptionStorageInMemoryImpl();
//          agent = new FitbitApiClientAgent(apiBaseUrl, fitnessChallengeBaseUrl, credCache);
//          FitbitAPIClientService service = new FitbitAPIClientService(agent, clientConsumerKey, clientSecret,
//                  credCache, entityCache, subscriptionStore); // need this call to set oauth internally in the service


          FitbitApiCredentialsCache credCache = new FitbitApiCredentialsCacheMapImpl();
          FitbitApiEntityCacheMapImpl entityCache = new FitbitApiEntityCacheMapImpl();
          FitbitApiSubscriptionStorageInMemoryImpl subscriptionStore = new FitbitApiSubscriptionStorageInMemoryImpl();
          agent = new FitbitApiClientAgent(apiBaseUrl, fitnessChallengeBaseUrl, credCache, serviceProvider);
          FitbitAPIClientService service = new FitbitAPIClientService(agent, clientConsumerKey, clientSecret,
                  credCache, entityCache, subscriptionStore); // need this call to set oauth internally in the service

          AccessToken accessToken = agent.getOAuthAccessToken(tempTokenReceived, tempTokenSecret, tempTokenVerifier);
          map.put("oauth_token", accessToken.getToken());
          map.put("oauth_token_secret", accessToken.getTokenSecret());
          map.put("fitbit_user_id", accessToken.getEncodedUserId());
          logger.debug("returning access tokens ==================================================");
          return new ResponseToProcess(HttpURLConnection.HTTP_OK, map);
      }
//      catch (FitbitAPIException e) {TODO: PUT ME BACK
//          logger.error("FITBIT API exception", e);
//          map.put("error", e.toString() + "---" + e.getMessage());
//          logger.debug("error ==================================================");
//          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, map);
//      }
      catch (Exception e) {
          logger.error("exception", e);
          map.put("error", e.toString() + "\n" + e.getStackTrace());
          logger.debug("error ==================================================");
          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, map);
      }
  }

}
