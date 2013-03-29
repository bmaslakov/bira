package org.bmaslakov.bira;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.content.pm.PackageInfo;

public class IssueReporter {
    public final static int MODE_BUG      = 0;
    public final static int MODE_PROPOSAL = 1;

    private final static String NEW_LINE  = "  \n";

    private final AccountData accountData;

    public IssueReporter(AccountData accountData) {
        this.accountData = accountData;
    }

    public IssueReporter(String accountName, String repoSlug, String consumerKey, String consumerSecret) {
        this.accountData = new AccountData(accountName, repoSlug, consumerKey, consumerSecret);
    }

    @SuppressWarnings("unused")
    public int report(String title, String text, int mode, PackageInfo app) {
        CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(accountData.getConsumerKey(), accountData.getConsumerSecret());
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(accountData.getTargetUrl());
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("title", title));
        StringBuilder contentBuilder = new StringBuilder()
            .append("##Date\n")
            .append(java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()))
            .append(NEW_LINE)
            .append("##Application info\n")
            .append(app.packageName)
            .append(" ")
            .append(app.versionName)
            .append(" (")
            .append(app.versionCode)
            .append(")")
            .append(NEW_LINE)
            .append("##Device info\n")
            .append(android.os.Build.PRODUCT)
            .append(" ")
            .append(android.os.Build.BRAND)
            .append(" ")
            .append(android.os.Build.MODEL)
            .append(" android ")
            .append(android.os.Build.VERSION.RELEASE)
            .append(NEW_LINE)
            .append("##User comment\n")
            .append(text);
        String content = contentBuilder.toString();
        nameValuePairs.add(new BasicNameValuePair("content", content));
        if (mode == MODE_BUG)
            nameValuePairs.add(new BasicNameValuePair("kind", "bug"));
        else if (mode == MODE_PROPOSAL)
            nameValuePairs.add(new BasicNameValuePair("kind", "proposal"));
        else
            throw new IllegalArgumentException("Mode=" + mode + " is not permitted");
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            consumer.sign(httpPost);
            HttpResponse response = httpClient.execute(httpPost);
            int responseCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);
            return responseCode;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OAuthMessageSignerException e) {
            e.printStackTrace();
        } catch (OAuthExpectationFailedException e) {
            e.printStackTrace();
        } catch (OAuthCommunicationException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
