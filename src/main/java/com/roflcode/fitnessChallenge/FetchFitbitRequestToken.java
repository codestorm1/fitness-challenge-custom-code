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
import com.fitbit.api.client.http.TempCredentials;

import com.fitbit.api.client.service.FitbitAPIClientService;
import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.sdkapi.LoggerService;
import com.stackmob.sdkapi.SDKServiceProvider;

import java.net.HttpURLConnection;
import java.util.*;


public class FetchFitbitRequestToken implements CustomCodeMethod {


    private static final String fitbitSiteBaseUrl = "http://www.fitbit.com";
    private static final String apiBaseUrl = "api.fitbit.com";
    private static final String fitnessChallengeBaseUrl = "http://localhost:4567";
    //private static final String fitnessChallengeBaseUrl = "http://fitnesschallenge.twistedogregmailcom.stackmobapp.com";
    private static final String clientConsumerKey = "c472de74290e435caa6b6829ff68b9aa";
    private static final String clientSecret = "312f9d3f9d10418bb43938103a73d5fe";
    private static LoggerService logger;

    @Override
  public String getMethodName() {
    return "fetch_fitbit_request_token";
  }

  @Override
  public List<String> getParams() {
    return Arrays.asList();
  }

  @Override
  public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
      logger = serviceProvider.getLoggerService(FetchFitbitRequestToken.class);
      Map<String, Object> map = new HashMap<String, Object>();

      Map<String,String> params = request.getParams();
      FitbitApiClientAgent agent;
      try {
          FitbitApiCredentialsCache credCache = new FitbitApiCredentialsCacheMapImpl();
          FitbitApiEntityCacheMapImpl entityCache = new FitbitApiEntityCacheMapImpl();
          FitbitApiSubscriptionStorageInMemoryImpl subscriptionStore = new FitbitApiSubscriptionStorageInMemoryImpl();
          agent = new FitbitApiClientAgent(apiBaseUrl, fitnessChallengeBaseUrl, credCache, serviceProvider);
          FitbitAPIClientService service = new FitbitAPIClientService(agent, clientConsumerKey, clientSecret,
                  credCache, entityCache, subscriptionStore); // need this call to set oauth internally in the service
          logger.debug("get oauth temp token");

          TempCredentials credentials = agent.getOAuthTempToken(fitnessChallengeBaseUrl);
          map.put("oauth_token", credentials.getToken());
          map.put("oauth_token_secret", credentials.getTokenSecret());
      }
      catch (FitbitAPIException e) {
          logger.debug("FITBIT API exception");
          logger.error("failed to get token", e);
          map.put("error", e.toString() + "---" + e.getMessage());
          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, map);
      }
      catch (Exception e) {
          logger.error("failed to get token", e);
          map.put("error", e.toString() + "\n" + e.getStackTrace());
          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, map);
      }
      return new ResponseToProcess(HttpURLConnection.HTTP_OK, map);
  }

}
