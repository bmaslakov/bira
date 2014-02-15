package org.bmaslakov.bira;

import java.io.IOException;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;

@SuppressLint("Registered")
public class ReportActivity extends Activity {
    public static final String EXTRA_NAME_ACCOUNT_DATA = "org.bmaslakov.bitbucketreporter.EXTRA_NAME_ACCOUNT_DATA";

    private EditText mEditTextIssueTitle, mEditTextIssueBody;
    private Spinner mSpinnerIssueType;

    private AccountData accountData;
    private IssueReporter issueReporter;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountData = getIntent().getParcelableExtra(EXTRA_NAME_ACCOUNT_DATA);
        if (accountData == null) {
            throw new IllegalArgumentException(
                    "EXTRA_NAME_ACCOUNT_DATA of type "
                            + "BitbucketAccountData must be passed to ReportActivity");
        }
        issueReporter = new IssueReporter(accountData);
        setContentView(R.layout.activity_report);
        mEditTextIssueTitle = (EditText) findViewById(R.id.editTextIssueTitle);
        mEditTextIssueBody = (EditText) findViewById(R.id.editTextIssueBody);
        mSpinnerIssueType = (Spinner) findViewById(R.id.spinnerIssueType);
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_save) {
            new PostIssueAsyncTask().execute();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private class PostIssueAsyncTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progressDialog;
        private int result;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(ReportActivity.this);
            progressDialog.setMessage(getResources().getString(
                    R.string.progressdialog_wait));
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            PackageInfo pInfo = null;
            try {
                pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            } catch (NameNotFoundException e) {
            }
            try {
                result = issueReporter.report(mEditTextIssueTitle.getText()
                        .toString(), mEditTextIssueBody.getText().toString(),
                        mSpinnerIssueType.getSelectedItemPosition(), pInfo);
            } catch (OAuthCommunicationException e) {
                result = -2;
            } catch (OAuthMessageSignerException e) {
                result = -2;
            } catch (OAuthExpectationFailedException e) {
                result = -2;
            } catch (IOException e) {
                result = -1;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void resultCode) {
            progressDialog.dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    ReportActivity.this);
            builder.setCancelable(true);
            builder.setInverseBackgroundForced(true);
            builder.setNegativeButton(
                    getResources().getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            if (result == 200) {
                builder.setTitle(getResources().getText(
                        R.string.dialog_result_ok_title));
                builder.setMessage(getResources().getText(
                        R.string.dialog_result_ok_text));
            } else if (result == -1) {
                builder.setTitle(getResources().getText(
                        R.string.dialog_result_exc_title));
                builder.setMessage(getResources().getText(
                        R.string.dialog_result_exc_text));
            } else if (result == -2) {
                builder.setTitle(getResources().getText(
                        R.string.dialog_result_exc_title));
                builder.setMessage(getResources().getText(
                        R.string.dialog_result_exc_auth_text));
            } else {
                builder.setTitle(getResources().getText(
                        R.string.dialog_result_exc_title));
                builder.setMessage("HTTP Response Code: " + result);
            }
            builder.create().show();
        }
    }
}
