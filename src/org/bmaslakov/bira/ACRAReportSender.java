package org.bmaslakov.bira;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSenderException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

public class ACRAReportSender implements org.acra.sender.ReportSender {
    private final static String NEW_LINE = "  \n";

    private final AccountData accountData;

    public ACRAReportSender(AccountData accountData) {
        this.accountData = accountData;
    }

    public ACRAReportSender(String accountName, String repoSlug, String consumerKey, String consumerSecret) {
        this.accountData = new AccountData(accountName, repoSlug, consumerKey, consumerSecret);
    }

    @Override
    @SuppressWarnings("unused")
    public void send(CrashReportData errorContent) throws ReportSenderException {
        CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(accountData.getConsumerKey(), accountData.getConsumerSecret());
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(accountData.getTargetUrl());
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("title", "bug report " + errorContent.get(ReportField.REPORT_ID)));
        StringBuilder contentBuilder = new StringBuilder()
            .append("##Date\n")
            .append(errorContent.get(ReportField.USER_CRASH_DATE))
            .append(NEW_LINE)
            .append("##Application info\n")
            .append(errorContent.get(ReportField.PACKAGE_NAME))
            .append(" ")
            .append(errorContent.get(ReportField.APP_VERSION_NAME))
            .append(" (")
            .append(errorContent.get(ReportField.APP_VERSION_CODE))
            .append(")")
            .append(NEW_LINE)
            .append("##Device info\n")
            .append(errorContent.get(ReportField.PRODUCT))
            .append(" ")
            .append(errorContent.get(ReportField.BRAND))
            .append(" ")
            .append(errorContent.get(ReportField.PHONE_MODEL))
            .append(" android ")
            .append(errorContent.get(ReportField.ANDROID_VERSION))
            .append(NEW_LINE)
            .append(errorContent.get(ReportField.DISPLAY).replace("\n", "  \n"))
            .append(NEW_LINE)
            .append("##Custom data\n")
            .append(errorContent.get(ReportField.CUSTOM_DATA))
            .append(NEW_LINE)
            .append("##User comment\n")
            .append(errorContent.get(ReportField.USER_COMMENT))
            .append(NEW_LINE)
            .append("##Stack trace\n")
            .append("```\n").append(errorContent.get(ReportField.STACK_TRACE).replace("\t", "    ")).append("```\n")
            .append("##Logcat\n")
            .append("```\n").append(errorContent.get(ReportField.LOGCAT).replace("\t", "    ")).append("```\n");
        String content = contentBuilder.toString();
        nameValuePairs.add(new BasicNameValuePair("content", content));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            consumer.sign(httpPost);
            HttpResponse response = httpClient.execute(httpPost);
            int responseCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OAuthMessageSignerException e) {
            e.printStackTrace();
        } catch (OAuthExpectationFailedException e) {
            e.printStackTrace();
        } catch (OAuthCommunicationException e) {
            e.printStackTrace();
        }
    }
}
