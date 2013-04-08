package org.bmaslakov.bira;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.acra.ACRA;
import org.acra.collector.CrashReportDataFactory;
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
    private final static String NEW_LINE = "  \n";
    public final static int MODE_BUG      = 0;
    public final static int MODE_PROPOSAL = 1;

    private final AccountData accountData;

    public IssueReporter(AccountData accountData) {
        this.accountData = accountData;
    }

    public IssueReporter(String accountName, String repoSlug, String consumerKey, String consumerSecret) {
        this.accountData = new AccountData(accountName, repoSlug, consumerKey, consumerSecret);
    }

    @SuppressWarnings("unused")
    public int report(String title, String text, int mode, PackageInfo app)
            throws IOException, OAuthCommunicationException,
            OAuthMessageSignerException, OAuthExpectationFailedException {
        CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(accountData.getConsumerKey(), accountData.getConsumerSecret());
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(accountData.getTargetUrl());
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("title", title));

        String customInfoString = "";
        try {
            // Some black magic with accessing private fields in ACRA...
            Field field = ACRA.getErrorReporter().getClass().getDeclaredField("crashReportDataFactory");
            field.setAccessible(true);
            CrashReportDataFactory crashReportDataFactory = (CrashReportDataFactory) field.get(ACRA.getErrorReporter());
            Method method = CrashReportDataFactory.class.getDeclaredMethod("createCustomInfoString");
            method.setAccessible(true);
            customInfoString = (String) method.invoke(crashReportDataFactory);
        } catch (IllegalArgumentException e) {
            //Doing it silent
        } catch (IllegalAccessException e) {
            //Doing it silent
        } catch (NoSuchFieldException e) {
            //Doing it silent
        } catch (InvocationTargetException e) {
            //Doing it silent
        } catch (NoSuchMethodException e) {
            //Doing it silent
        }

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
            .append("##Custom data\n")
            .append(customInfoString)
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
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
        consumer.sign(httpPost);
        HttpResponse response = httpClient.execute(httpPost);
        int responseCode = response.getStatusLine().getStatusCode();
        String responseBody = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);
        return responseCode;
    }
}
