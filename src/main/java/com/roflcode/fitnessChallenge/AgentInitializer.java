package com.roflcode.fitnessChallenge;

import com.fitbit.api.client.*;
import com.fitbit.api.client.service.FitbitAPIClientService;
import com.fitbit.api.model.APIResourceCredentials;
import com.fitbit.api.model.FitbitUser;
import com.stackmob.core.DatastoreException;
import com.stackmob.core.InvalidSchemaException;
import com.stackmob.sdkapi.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: bryan
 * Date: 1/5/13
 * Time: 5:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class AgentInitializer {

    private static final String fitbitSiteBaseUrl = "http://www.fitbit.com";
    private static final String apiBaseUrl = "api.fitbit.com";
    private static final String devFitnessChallengeBaseUrl = "http://localhost:4567";
    private static final String prodFitnessChallengeBaseUrl = "http://fitnesschallenge.twistedogregmailcom.stackmobapp.com";
    private static final String clientConsumerKey = "c472de74290e435caa6b6829ff68b9aa";
    private static final String clientSecret = "312f9d3f9d10418bb43938103a73d5fe";
    private static LoggerService logger;

    public static HashMap<String, String> GetStoredFitbitCredentials(SDKServiceProvider serviceProvider, String stackmobUserID) {
        HashMap<String, String> credentials = new HashMap<String, String>();

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
            }
            else {
                credentials.put("error", "no user found with given stackmobUserID");
                return credentials;
            }


            SMValue smAccess = userObject.getValue().get("accesstoken");
            credentials.put("accesstoken", ((SMString)smAccess).getValue());
            SMValue smSecret = userObject.getValue().get("accesstokensecret");
            credentials.put("accesstokensecret", ((SMString)smSecret).getValue());
            SMValue smFitbitUserID = userObject.getValue().get("fitbituserid");
            credentials.put("fitbituserid", ((SMString)smFitbitUserID).getValue());

        } catch (InvalidSchemaException e) {
            credentials.put("error", "invalid_schema");
            credentials.put("detail", e.toString());
        } catch (DatastoreException e) {
            credentials.put("error", "datastore_exception");
            credentials.put("detail", e.toString());
        } catch(Exception e) {
            credentials.put("error", "unknown");
            credentials.put("detail", e.toString());
        }

        return credentials;
    }



    public static FitbitApiClientAgent GetInitializedAgent(SDKServiceProvider serviceProvider, String stackmobUserID) {

        logger = serviceProvider.getLoggerService(UpdateFitbitActivities.class);
        FitbitAPIClientService service = null;
        FitbitApiClientAgent agent = null;
//        return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
        String accessToken;
        String tokenSecret;
        String fitbitUserID = null;

        try {
            FitbitApiCredentialsCache credCache = new FitbitApiCredentialsCacheMapImpl();
            FitbitApiEntityCacheMapImpl entityCache = new FitbitApiEntityCacheMapImpl();
            FitbitApiSubscriptionStorageInMemoryImpl subscriptionStore = new FitbitApiSubscriptionStorageInMemoryImpl();
            String fitnessCallback = serviceProvider.isSandbox() ? devFitnessChallengeBaseUrl : prodFitnessChallengeBaseUrl;
            Boolean isSand = serviceProvider.isSandbox();
            logger.debug("is sandbox? " + isSand.toString() + ".  Callback is " + fitnessCallback);
            agent = new FitbitApiClientAgent(apiBaseUrl, fitnessCallback, credCache, serviceProvider);
            service = new FitbitAPIClientService(agent, clientConsumerKey, clientSecret,
                    credCache, entityCache, subscriptionStore); // need this call to set oauth internally in the service

            LocalUserDetail user = new LocalUserDetail(stackmobUserID);
            APIResourceCredentials cred = credCache.getResourceCredentials(user);
            if (cred == null) {
                logger.warn("no existing creds");
                HashMap<String, String> credentials = GetStoredFitbitCredentials(serviceProvider, stackmobUserID);

                if (credentials.containsKey("error") ) {
                    logger.warn("error getting credentials: " + credentials.get("error"));
                    logger.warn(credentials.get("details"));
                    return null;
                }
                fitbitUserID = credentials.get("fitbituserid");
                accessToken = credentials.get("accesstoken");
                tokenSecret = credentials.get("accesstokensecret");
                logger.debug(String.format("returned creds: fitbit ID %s access token: %s secret: %s", fitbitUserID, accessToken, tokenSecret));

                cred = new APIResourceCredentials(stackmobUserID, "", "");
                cred.setAccessToken(accessToken);
                cred.setAccessTokenSecret(tokenSecret);
                credCache.saveResourceCredentials(user, cred);
                agent.getCredentialsCache().saveResourceCredentials(user, cred);
                logger.debug(String.format("APIResourceCredentials: access token: %s secret: %s", cred.getAccessToken(), cred.getAccessTokenSecret()));
            }
            else {
                logger.debug("loaded existing creds");
            }

            logger.debug(String.format("fitbit ID %s access token: %s secret: %s", fitbitUserID, cred.getAccessToken(), cred.getAccessTokenSecret()));
            //agent = new FitbitApiClientAgent(apiBaseUrl, fitnessChallengeBaseUrl, credCache);
            FitbitUser fitbitUser = new FitbitUser(fitbitUserID);
            return agent;
        }
        catch (Exception e) {
            logger.error("exception getting fitbit creds from user", e);
            return null;
        }

    }
}
