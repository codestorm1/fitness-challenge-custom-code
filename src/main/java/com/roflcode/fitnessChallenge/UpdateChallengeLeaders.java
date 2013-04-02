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

import com.roflcode.fitnessChallenge.ChallengeLeaders;


public class UpdateChallengeLeaders implements CustomCodeMethod {


    private static LoggerService logger;

    @Override
  public String getMethodName() {
    return "update_challenge_leaders";
  }

  @Override
  public List<String> getParams() {
    return Arrays.asList("stackmob_user_id");
  }


  @Override
  public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
      logger = serviceProvider.getLoggerService(UpdateChallengeLeaders.class);
      logger.debug("update challenge leaders ------------------------------");

      String stackmobUserID = request.getParams().get("stackmob_user_id");

      if (stackmobUserID == null || stackmobUserID.isEmpty()) {
          HashMap<String, String> errParams = new HashMap<String, String>();
          errParams.put("error", "stackmobUserID was empty or null");
          return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, errParams); // http 400 - bad request
      }

      ChallengeLeaders challengeLeaders = new ChallengeLeaders();
      String result = challengeLeaders.UpdateLeaders(stackmobUserID, serviceProvider);


      Map<String, Object> returnMap = new HashMap<String, Object>();
      returnMap.put("result", result);

      logger.debug("completed leader update");
      return new ResponseToProcess(HttpURLConnection.HTTP_OK, returnMap);
  }
}