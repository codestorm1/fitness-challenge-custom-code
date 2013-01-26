/*
Copyright (c) 2007-2009, Yusuke Yamamoto
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Yusuke Yamamoto nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY Yusuke Yamamoto ``AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Yusuke Yamamoto BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.fitbit.api.client.http;

import com.fitbit.api.FitbitAPIException;
import com.fitbit.api.client.Configuration;


import com.stackmob.core.ServiceNotActivatedException;
import com.stackmob.sdkapi.LoggerService;
import com.stackmob.sdkapi.SDKServiceProvider;
import com.stackmob.sdkapi.http.HttpService;
import com.stackmob.sdkapi.http.Header;
import com.stackmob.sdkapi.http.request.GetRequest;
import com.stackmob.sdkapi.http.request.PostRequest;
import com.stackmob.sdkapi.http.response.HttpResponse;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
//import java.net.*;
import java.net.URLEncoder;
import java.net.Proxy.Type;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * A utility class to handle HTTP request/response.
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
public class HttpClientStackMob implements Serializable {

//	protected static Log log = LogFactory.getLog(HttpClientStackMob.class);
    protected LoggerService log;

	protected static enum HttpMethod {
		GET,
		POST,
		DELETE,
		PUT
	}

    private static final int OK = 200;// OK: Success!
    private static final int NOT_MODIFIED = 304;// Not Modified: There was no new data to return.
    private static final int BAD_REQUEST = 400;// Bad Request: The request was invalid.  An accompanying error message will explain why. This status code will be returned during rate limiting.
    public static final int NOT_AUTHORIZED = 401;// Not Authorized: Authentication credentials were missing or incorrect.
    private static final int FORBIDDEN = 403;// Forbidden: The request is understood, but it has been refused.  An accompanying error message will explain why.
    private static final int NOT_FOUND = 404;// Not Found: The URI requested is invalid or the resource requested, such as a user, does not exists.
    private static final int NOT_ACCEPTABLE = 406;// Not Acceptable: Returned by the Search API when an invalid format is specified in the request.
    public static final int CONFLICT = 409; // Conflict: Returned by the Subscription API when trying to create a new subscription with a subscription ID which already exists.
    private static final int INTERNAL_SERVER_ERROR = 500;// Internal Server Error: Something is broken.  Please post to the group so the Fitbit team can investigate.
    private static final int BAD_GATEWAY = 502;// Bad Gateway: Fitbit is down or being upgraded.
    private static final int SERVICE_UNAVAILABLE = 503;// Service Unavailable: The Fitbit servers are up, but overloaded with requests. Try again later. The search and trend methods use this to indicate when you are being rate limited.

    private String basic;
    private int retryCount = 1;//Configuration.getRetryCount();
    private int retryIntervalMillis = 2000;//Configuration.getRetryIntervalSecs() * 1000;
    private String userId = Configuration.getUser();
    private SDKServiceProvider serviceProvider;
    private HttpService httpService;

    private String password = Configuration.getPassword();
    private String proxyHost = Configuration.getProxyHost();
    private int proxyPort = Configuration.getProxyPort();
    private String proxyAuthUser = Configuration.getProxyUser();
    private String proxyAuthPassword = Configuration.getProxyPassword();
    private int connectionTimeout = 2000;//Configuration.getConnectionTimeout();
    private int readTimeout = 2000;//Configuration.getReadTimeout();
    private static final long serialVersionUID = 808018030183407996L;
    private static boolean isJDK14orEarlier;
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private OAuth oauth;
    private String requestTokenURL = Configuration.getScheme() + "fitbit.com/oauth/request_token";
    private String authorizationURL = Configuration.getScheme() + "fitbit.com/oauth/authorize";
    private String authenticationURL = Configuration.getScheme() + "fitbit.com/oauth/authenticate";
    private String accessTokenURL = Configuration.getScheme() + "fitbit.com/oauth/access_token";
    private OAuthToken oauthToken;

    static {
        try {
            String versionStr = System.getProperty("java.specification.version");
            if (null != versionStr) {
                isJDK14orEarlier = 1.5d > Double.parseDouble(versionStr);
            }
        } catch (AccessControlException ace) {
            isJDK14orEarlier = true;
        }
    }

//    public HttpClientStackMob(String userId, String password) {
//        this();
//        setUserId(userId);
//        setPassword(password);
//    }

    public HttpClientStackMob(SDKServiceProvider serviceProvider) {
        basic = null;
        setUserAgent(null);
        setOAuthConsumer(null, null);
        setRequestHeader("Accept-Encoding","gzip");
        setStackMobService(serviceProvider);
        log = serviceProvider.getLoggerService(HttpClientStackMob.class);
        try {
            httpService = serviceProvider.getHttpService();
        }
        catch (ServiceNotActivatedException ex) {
            log.error("Service not activated", ex);
        }
    }

    public void setStackMobService(SDKServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public SDKServiceProvider getStackMobService() {
        return this.serviceProvider;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        encodeBasicAuthenticationString();
    }

    public void setPassword(String password) {
        this.password = password;
        encodeBasicAuthenticationString();
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAuthenticationEnabled(){
        return null != basic || null != oauth;
    }

    /**
     * Sets the consumer key and consumer secret.<br>
     * System property -DFitbit4j.oauth.consumerKey and -Dhttp.oauth.consumerSecret override this attribute.
     * @param consumerKey consumer key
     * @param consumerSecret consumer secret
     */
    public void setOAuthConsumer(String consumerKey, String consumerSecret) {
        consumerKey = Configuration.getOAuthConsumerKey(consumerKey);
        consumerSecret = Configuration.getOAuthConsumerSecret(consumerSecret);
        if (null != consumerKey && null != consumerSecret
                && 0 != consumerKey.length() && 0 != consumerSecret.length()) {
            oauth = new OAuth(consumerKey, consumerSecret);
        }
    }

    /**
     *
     * @return request token
     * @throws com.fitbit.api.FitbitAPIException
     */
    public TempCredentials getOAuthRequestToken() throws FitbitAPIException {
        log.debug("get request token");
        oauthToken = new TempCredentials(httpRequest(HttpMethod.POST, requestTokenURL, PostParameter.EMPTY_ARRAY, true), this);
        return (TempCredentials) oauthToken;
    }

    /**
     * @param callback_url callback url
     * @return request token
     * @throws com.fitbit.api.FitbitAPIException
     */
    public TempCredentials getOauthRequestToken(String callback_url) throws FitbitAPIException {
        Response resp = httpRequest(HttpMethod.POST, requestTokenURL,
                new PostParameter[]{new PostParameter("oauth_callback", callback_url)}, true);
        if (resp == null) {
            log.warn("null response from httpRequest");
        }
        oauthToken = new TempCredentials(resp, this);
        return (TempCredentials) oauthToken;
    }

    /**
     *
     * @param token request token
     * @return access token
     * @throws
     */
    public AccessToken getOAuthAccessToken(TempCredentials token) throws FitbitAPIException {
        try {
            oauthToken = token;
            Response response = httpRequest(HttpMethod.POST, accessTokenURL, PostParameter.EMPTY_ARRAY, true);
            oauthToken = new AccessToken(response);
        } catch (FitbitAPIException te) {
            throw new FitbitAPIException("The user has not given access to the account.", te, te.getStatusCode());
        }
        return (AccessToken) oauthToken;
    }

    /**
     *
     * @param token request token
     * @return access token
     * @throws com.fitbit.api.FitbitAPIException
     */
    public AccessToken getOAuthAccessToken(TempCredentials token, String pin) throws FitbitAPIException {
        try {
            oauthToken = token;
            Response response = httpRequest(HttpMethod.POST, accessTokenURL, new PostParameter[]{new PostParameter("oauth_verifier", pin)}, true);
            oauthToken = new AccessToken(response);
        } catch (FitbitAPIException te) {
            throw new FitbitAPIException("The user has not given access to the account.", te, te.getStatusCode());
        }
        return (AccessToken) oauthToken;
    }

    /**
     *
     * @param token request token
     * @param tokenSecret request token secret
     * @param oauth_verifier oauth_verifier or pin
     * @return access token
     * @throws com.fitbit.api.FitbitAPIException
     */
    public AccessToken getOAuthAccessToken(String token, String tokenSecret, String oauth_verifier) throws FitbitAPIException {
        try {
            oauthToken = new OAuthToken(token, tokenSecret) {};
            Response response = httpRequest(HttpMethod.POST, accessTokenURL,
                    new PostParameter[]{new PostParameter("oauth_verifier", oauth_verifier)}, true);
            oauthToken = new AccessToken(response);
        } catch (FitbitAPIException te) {
            throw new FitbitAPIException("The user has not given access to the account.", te, te.getStatusCode());
        }
        return (AccessToken) oauthToken;
    }

    /**
     * Sets the authorized access token
     * @param token authorized access token
     */

    public void setOAuthAccessToken(AccessToken token){
        oauthToken = token;
    }

    public void setRequestTokenURL(String requestTokenURL) {
        this.requestTokenURL = requestTokenURL;
    }

    public String getRequestTokenURL() {
        return requestTokenURL;
    }


    public void setAuthorizationURL(String authorizationURL) {
        this.authorizationURL = authorizationURL;
    }

    public String getAuthorizationURL() {
        return authorizationURL;
    }

    public String getAuthenticationRL() {
        return authenticationURL;
    }

    public void setAccessTokenURL(String accessTokenURL) {
        this.accessTokenURL = accessTokenURL;
    }

    public String getAccessTokenURL() {
        return accessTokenURL;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * Sets proxy host.
     * System property -DFitbit4j.http.proxyHost or http.proxyHost overrides this attribute.
     * @param proxyHost
     */
    public void setProxyHost(String proxyHost) {
        this.proxyHost = Configuration.getProxyHost(proxyHost);
    }

    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * Sets proxy port.
     * System property -DFitbit4j.http.proxyPort or -Dhttp.proxyPort overrides this attribute.
     * @param proxyPort
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = Configuration.getProxyPort(proxyPort);
    }

    public String getProxyAuthUser() {
        return proxyAuthUser;
    }

    /**
     * Sets proxy authentication user.
     * System property -DFitbit4j.http.proxyUser overrides this attribute.
     * @param proxyAuthUser
     */
    public void setProxyAuthUser(String proxyAuthUser) {
        this.proxyAuthUser = Configuration.getProxyUser(proxyAuthUser);
    }

    public String getProxyAuthPassword() {
        return proxyAuthPassword;
    }

    /**
     * Sets proxy authentication password.
     * System property -DFitbit4j.http.proxyPassword overrides this attribute.
     * @param proxyAuthPassword
     */
    public void setProxyAuthPassword(String proxyAuthPassword) {
        this.proxyAuthPassword = Configuration.getProxyPassword(proxyAuthPassword);
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Sets a specified timeout value, in milliseconds, to be used when opening a communications link to the resource referenced by this URLConnection.
     * System property -DFitbit4j.http.connectionTimeout overrides this attribute.
     * @param connectionTimeout - an int that specifies the connect timeout value in milliseconds
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = Configuration.getConnectionTimeout(connectionTimeout);

    }
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the read timeout to a specified timeout, in milliseconds. System property -DFitbit4j.http.readTimeout overrides this attribute.
     * @param readTimeout - an int that specifies the timeout value to be used in milliseconds
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = Configuration.getReadTimeout(readTimeout);
    }

    private void encodeBasicAuthenticationString() {
        if (null != userId && null != password) {
            basic = "Basic " + new BASE64Encoder().encode((userId + ':' + password).getBytes());
        }
    }

    public void setRetryCount(int retryCount) {
        if (retryCount >= 0) {
            this.retryCount = Configuration.getRetryCount(retryCount);
        } else {
            throw new IllegalArgumentException("RetryCount cannot be negative.");
        }
    }

    public void setUserAgent(String ua) {
        setRequestHeader("User-Agent", Configuration.getUserAgent(ua));
    }
    public String getUserAgent(){
        return getRequestHeader("User-Agent");
    }

    public void setRetryIntervalSecs(int retryIntervalSecs) {
        if (retryIntervalSecs >= 0) {
            retryIntervalMillis = Configuration.getRetryIntervalSecs(retryIntervalSecs) * 1000;
        } else {
            throw new IllegalArgumentException(
                    "RetryInterval cannot be negative.");
        }
    }

    public Response post(String url, PostParameter[] postParameters,
                         boolean authenticated) throws FitbitAPIException {
        return httpRequest(HttpMethod.POST, url, postParameters, authenticated);
    }

    public Response post(String url, boolean authenticated) throws FitbitAPIException {
    	return post(url, PostParameter.EMPTY_ARRAY, authenticated);
    }

    public Response post(String url, PostParameter[] postParameters) throws FitbitAPIException {
        return post(url, postParameters, false);
    }

    public Response post(String url) throws FitbitAPIException {
        return post(url, PostParameter.EMPTY_ARRAY, false);
    }
    
    public Response delete(String url) throws FitbitAPIException {
    	return delete(url, false);
    }
    
    public Response delete(String url, boolean authenticated) throws FitbitAPIException {
    	return httpRequest(HttpMethod.DELETE, url, null, authenticated);
    }	
    
    public Response get(String url, boolean authenticated) throws FitbitAPIException {
        return httpRequest(HttpMethod.GET, url, null, authenticated);
    }

    public Response get(String url) throws FitbitAPIException {
        return get(url, false);
    }

    protected Response httpRequest(HttpMethod method, String url, PostParameter[] postParams,
                                 boolean authenticated) throws FitbitAPIException {

//    	if (log.isDebugEnabled()) {
  		log.debug("in httpRequest: HTTP " + method + " " + url);
//    	}
    	
        int retriedCount;
        int retry = retryCount + 1;
        HttpResponse httpResponse = null;
        Response response = null;
        HttpService httpService;
        for (retriedCount = 0; retriedCount < retry; retriedCount++) {
            int responseCode = -1;
            try {
                httpService = this.serviceProvider.getHttpService();
            }
            catch (ServiceNotActivatedException ex) {
                log.error("failed to get http service", ex);
                return null;
            }
            try {
                HashSet<Header> headers = getHeaders(method, url, postParams, authenticated);
                headers.add(new Header("Cache-Control", "no-cache"));
                headers.add(new Header("Host", "api.fitbit.com"));
                headers.add(new Header("X-Fitbit-Client-URL", "http://wiki.fitbit.com/Fitbit-API-Java-Client /1"));
                headers.add(new Header("User-Agent", "fitbitAPIClient http://wiki.fitbit.com/Fitbit-API-Java-Client /1"));

                String postParam = null;
                if (postParams != null) {
                    Header header = new Header("Content-Type", "application/x-www-form-urlencoded");
                    headers.add(header);
                    postParam = encodeParameters(postParams);
                    byte[] bytes = postParam.getBytes("UTF-8");
                    header = new com.stackmob.sdkapi.http.Header("Content-Length", Integer.toString(bytes.length));
                    headers.add(header);
                    log.debug("HTTP Post Params: " + postParam);
                }

//                log.debug("Request headers ========================================");
//
//                for (Header header : headers) {
//                    if (header != null) {
//                        log.debug(header.getName() + " " + header.getValue());
//                    }
//                }
//                log.debug("------------------------------------------");
                //send the request. this method call will not return until the server at http://stackmob.com returns.
                //note that this method may throw AccessDeniedException if the URL is whitelisted or rate limited,
                //or TimeoutException if the server took too long to return

                try {
                    if (method == HttpMethod.POST) {
                        PostRequest req = new PostRequest(url, headers, postParam);
                        httpResponse = httpService.post(req);
                    } else
                    if (method == HttpMethod.GET) {
                        GetRequest getReq = new GetRequest(url, headers);
                        httpResponse = httpService.get(getReq);
                    }
                    if (httpResponse == null) {
                        throw new FitbitAPIException("null response wtf");
                    }
                }
                catch (Exception ex) {
                    log.error("access denied or timeout during http request", ex);
                }

//                    if (log.isDebugEnabled()){
//                        log.debug("HTTP Response Headers: ");
//                        Map<String, List<String>> responseHeaders = con.getHeaderFields();
//                        for (String key : responseHeaders.keySet()) {
//                            List<String> values = responseHeaders.get(key);
//                            for (String value : values) {
//                                if (null != key) {
//                                    log.debug("Header: '" + key + "' => '" + value + "'");
//                                } else{
//                                	log.debug("Header value: '" + value + "'");
//                                }
//                            }
//                        }
//                    }

                responseCode = httpResponse.getCode();
                if (responseCode >= 200 && responseCode < 300) {
                    log.debug("successful request");
                    log.debug("response: " + httpResponse.getBody() + " headers: " + httpResponse.getHeaders());
                    break;
                } else {
                    if (responseCode < INTERNAL_SERVER_ERROR || retriedCount == retryCount) {
                        log.warn("response code: " + responseCode + " retried: " + retriedCount);
                        throw new FitbitAPIException(getCause(responseCode), response);
                    }
                    // will retry if the status code is INTERNAL_SERVER_ERROR
                }
            } catch (IOException ioe) {
                // connection timeout or read timeout
                log.error("IO Exception", ioe);
                if (retriedCount == retryCount) {
                    throw new FitbitAPIException(ioe.getMessage(), ioe, responseCode);
                }
            }
            try {
                log.debug("Sleeping " + retryIntervalMillis + " millisecs for next retry.");
                Thread.sleep(retryIntervalMillis);
            } catch (InterruptedException ignore) {
                //nothing to do
            }
        }
        if (httpResponse != null) {
            response = new Response(httpResponse);
        }
        return response;
    }

    public static String encodeParameters(PostParameter[] postParams) {
        StringBuffer buf = new StringBuffer();
        for (int j = 0; j < postParams.length; j++) {
            if (j != 0) {
                buf.append('&');
            }
            try {
                buf.append(URLEncoder.encode(postParams[j].name, "UTF-8"))
                        .append('=').append(URLEncoder.encode(postParams[j].value, "UTF-8"));
            } catch (UnsupportedEncodingException neverHappen) {
            }
        }
        return buf.toString();

    }

    /**
     * creates and returns HTTP headers
     *
     * @param authenticated boolean
     */
    private HashSet<Header> getHeaders(HttpMethod method, String url, PostParameter[] params, boolean authenticated) {
//        if (log.isDebugEnabled()) {
            log.debug("Request: HTTP " + method.toString() + ' ' + url);
//        }

        HashSet<Header> headers = new HashSet<Header>();

        if (authenticated) {
            if (basic == null && oauth == null) {
                log.debug("no basic, no oauth");
            }
            String authorization;
            if (oauth != null) {
                // use OAuth
                authorization = oauth.generateAuthorizationHeader(method.toString(), url, params, oauthToken);
            } else if (basic != null) {
                // use Basic Auth
                authorization = basic;
            } else {
                log.error("no oauth, no basic auth");
                throw new IllegalStateException(
                        "Neither user ID/password combination nor OAuth consumer key/secret combination supplied");
            }
            headers.add(new Header("Authorization", authorization));
//            logger.debug("Authorization: " + authorization);
        }
        for (String key : requestHeaders.keySet()) {
            headers.add(new Header(key, requestHeaders.get(key)));
        }
        return headers;
    }

    public void setRequestHeader(String name, String value) {
        requestHeaders.put(name, value);
    }

    public String getRequestHeader(String name) {
        return requestHeaders.get(name);
    }

    public void removeRequestHeader(String name) {
        requestHeaders.remove(name);
    }


// BG not sure if this is needed
//    private HttpURLConnection getConnection(String url) throws IOException {
//        HttpURLConnection con;
//        if (proxyHost != null && !proxyHost.equals("")) {
//            if (proxyAuthUser != null && !proxyAuthUser.equals("")) {
//                log.debug("Proxy AuthUser: " + proxyAuthUser);
//                log.debug("Proxy AuthPassword: " + proxyAuthPassword);
//                Authenticator.setDefault(new Authenticator() {
//                    @Override
//                    protected PasswordAuthentication
//                    getPasswordAuthentication() {
//                        //respond only to proxy auth requests
//                        if (getRequestorType().equals(RequestorType.PROXY)) {
//                            return new PasswordAuthentication(proxyAuthUser,
//                                    proxyAuthPassword
//                                            .toCharArray());
//                        } else {
//                            return null;
//                        }
//                    }
//                });
//            }
//            final Proxy proxy = new Proxy(Type.HTTP, InetSocketAddress
//                    .createUnresolved(proxyHost, proxyPort));
//            log.debug("Opening proxied connection (" + proxyHost + ':' + proxyPort + ')');
//            con = (HttpURLConnection) new URL(url).openConnection(proxy);
//        } else {
//            con = (HttpURLConnection) new URL(url).openConnection();
//        }
//        if (connectionTimeout > 0 && !isJDK14orEarlier) {
//            con.setConnectTimeout(connectionTimeout);
//        }
//        if (readTimeout > 0 && !isJDK14orEarlier) {
//            con.setReadTimeout(readTimeout);
//        }
//        return con;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HttpClientStackMob)) return false;

        HttpClientStackMob that = (HttpClientStackMob) o;

        if (connectionTimeout != that.connectionTimeout) return false;
        if (proxyPort != that.proxyPort) return false;
        if (readTimeout != that.readTimeout) return false;
        if (retryCount != that.retryCount) return false;
        if (retryIntervalMillis != that.retryIntervalMillis) return false;
        if (accessTokenURL != null ? !accessTokenURL.equals(that.accessTokenURL) : that.accessTokenURL != null)
            return false;
        if (!authenticationURL.equals(that.authenticationURL)) return false;
        if (!authorizationURL.equals(that.authorizationURL)) return false;
        if (basic != null ? !basic.equals(that.basic) : that.basic != null)
            return false;
        if (oauth != null ? !oauth.equals(that.oauth) : that.oauth != null)
            return false;
        if (oauthToken != null ? !oauthToken.equals(that.oauthToken) : that.oauthToken != null)
            return false;
        if (password != null ? !password.equals(that.password) : that.password != null)
            return false;
        if (proxyAuthPassword != null ? !proxyAuthPassword.equals(that.proxyAuthPassword) : that.proxyAuthPassword != null)
            return false;
        if (proxyAuthUser != null ? !proxyAuthUser.equals(that.proxyAuthUser) : that.proxyAuthUser != null)
            return false;
        if (proxyHost != null ? !proxyHost.equals(that.proxyHost) : that.proxyHost != null)
            return false;
        if (!requestHeaders.equals(that.requestHeaders)) return false;
        if (!requestTokenURL.equals(that.requestTokenURL)) return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = basic != null ? basic.hashCode() : 0;
        result = 31 * result + retryCount;
        result = 31 * result + retryIntervalMillis;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (proxyHost != null ? proxyHost.hashCode() : 0);
        result = 31 * result + proxyPort;
        result = 31 * result + (proxyAuthUser != null ? proxyAuthUser.hashCode() : 0);
        result = 31 * result + (proxyAuthPassword != null ? proxyAuthPassword.hashCode() : 0);
        result = 31 * result + connectionTimeout;
        result = 31 * result + readTimeout;
        result = 31 * result + requestHeaders.hashCode();
        result = 31 * result + (oauth != null ? oauth.hashCode() : 0);
        result = 31 * result + requestTokenURL.hashCode();
        result = 31 * result + authorizationURL.hashCode();
        result = 31 * result + authenticationURL.hashCode();
        result = 31 * result + (accessTokenURL != null ? accessTokenURL.hashCode() : 0);
        result = 31 * result + (oauthToken != null ? oauthToken.hashCode() : 0);
        return result;
    }

    private static String getCause(int statusCode){
        String cause = null;
        // https://wiki.fitbit.com/HTTP-Response-Codes-and-Errors
        switch(statusCode){
            case NOT_MODIFIED:
                break;
            case BAD_REQUEST:
                cause = "The request was invalid. An accompanying error message will explain why.";
                break;
            case NOT_AUTHORIZED:
                cause = "Authentication credentials were missing or incorrect.";
                break;
            case FORBIDDEN:
                cause = "The request was valid, but it has been refused. An accompanying error message will explain why.";
                break;
            case NOT_FOUND:
                cause = "The URI requested is invalid or the resource requested, such as a user, does not exist.";
                break;
            case NOT_ACCEPTABLE:
                cause = "Returned by the Search API when an invalid format is specified in the request.";
                break;
            case CONFLICT:
                cause = "Returned by the Subscription API when trying to create a new subscription with a subscription ID which already exists.";
                break;
            case INTERNAL_SERVER_ERROR:
                cause = "Something is broken. Please post to the group so the Fitbit team can investigate.";
                break;
            case BAD_GATEWAY:
                cause = "Fitbit is down or being upgraded.";
                break;
            case SERVICE_UNAVAILABLE:
                cause = "Service Unavailable: The Fitbit servers are up, but overloaded with requests. Try again later. The search call uses this status code to indicate that you are being rate limited.";
                break;
            default:
                cause = "";
        }
        return statusCode + ": " + cause;
    }

}