package jp.rmitkt.xposed.fakedevicedata;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.afollestad.materialdialogs.*;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
	class AppInfo {
		private String appname = "";
		private String pname = "";
	}

	protected static final String prefFileName = "pref";
	SharedPreferences pref;  
	ListView appList;
	ArrayList<AppInfo> appinfos;
	private AsyncTask<Void, Void, ArrayList<AppInfo>> mAsyncTask;
	private ProgressDialog mProgressDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final FloatingActionsMenu menu = (FloatingActionsMenu) findViewById(R.id.multiple_actions);
		final FloatingActionButton actionSave = (FloatingActionButton) findViewById(R.id.action_save);
		actionSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				save(view);
				menu.collapse();
			}
		});
		final FloatingActionButton actionReset = (FloatingActionButton) findViewById(R.id.action_reset);
		actionReset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				deselectAll(view);
				menu.collapse();
				Toast.makeText(getApplicationContext(), R.string.reset_message, Toast.LENGTH_LONG).show();
			}
		});
		final FloatingActionButton actionFakeData = (FloatingActionButton) findViewById(R.id.action_fakedata);
		actionFakeData.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(getApplicationContext(), FakeDataSettingActivity.class);
				startActivity(intent);
				menu.collapse();
			}
		});

		pref = getSharedPreferences(prefFileName, Context.MODE_PRIVATE);
		appList = (ListView) findViewById(R.id.appList);
		setAppList();
}

	public void save(View v){
		Editor editor = pref.edit();
		for (int i = 0; i < appinfos.size(); i++){
			editor.putBoolean(appinfos.get(i).pname, appList.isItemChecked(i));
		}
		editor.apply();
		fixPreferencesPermissions(this, prefFileName);
		Toast.makeText(this, R.string.save_message, Toast.LENGTH_LONG).show();
		finish();
	}

	// https://github.com/GravityBox/GravityBox/commit/2a96528ec9f51964355d9c699b7b1cbe027d52f5
	// MODE_WORLD_READABLE is depricated, so use MODE_PRIVATE and manually set file & folder r+x permissions
	protected static void fixPreferencesPermissions(Context context, String prefFileName) {
		File prefsFile = new File(context.getApplicationContext().getFilesDir().getAbsoluteFile() + "/../shared_prefs/" + prefFileName + ".xml");
		File prefsDir = prefsFile.getParentFile();
		File appDir = prefsDir.getParentFile();

		prefsFile.setReadable(true, false);
		prefsDir.setExecutable(true, false);
		prefsDir.setReadable(true, false);
		appDir.setExecutable(true, false);
		appDir.setReadable(true, false);
	}

	public void selectAll(View v){
		for (int i = 0; i < appinfos.size(); i++)      
			appList.setItemChecked(i, true);
	}

	public void deselectAll(View v){
		for (int i = 0; i < appinfos.size(); i++)      
			appList.setItemChecked(i, false);
	}

	public void load(){
		for (int i = 0; i < appinfos.size(); i++){
			appList.setItemChecked(i, pref.getBoolean(appinfos.get(i).pname, false));
		}
	}

	@Override
	public void onBackPressed(){
		new MaterialDialog.Builder(this)
		.title(R.string.save_prompt_title)
		.content(R.string.save_prompt_message)
		.positiveText(R.string.save_prompt_positive)
		.positiveColorRes(R.color.bluegrey500)
		.negativeText(R.string.save_prompt_negative)
		.negativeColorRes(R.color.bluegrey500)
		.onNegative(new MaterialDialog.SingleButtonCallback() {
			@Override
			public void onClick(MaterialDialog dialog, DialogAction which) {
				finish();
			}
		})
		.build().show();
	}

	private void setAppList(){
		mAsyncTask = new AsyncTask<Void,Void,ArrayList<AppInfo>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showProgressDialog();
            }
            
            @Override
            protected ArrayList<AppInfo> doInBackground(Void... arg0) {
            	ArrayList<AppInfo> res = new ArrayList<AppInfo>();        
            	List<PackageInfo> packs = getPackageManager().getInstalledPackages(0);
            	for (int i = 0; i < packs.size(); i++){
            		PackageInfo p = packs.get(i);
            		if (p.versionName == null){
            			continue;
            		}
            		AppInfo newInfo = new AppInfo();
            		newInfo.appname = p.applicationInfo.loadLabel(getPackageManager()).toString();
            		newInfo.pname = p.packageName;
            		res.add(newInfo);
            	}
            	return res;
            }
        		
            @Override
            protected void onCancelled() {
            	dismissProgressDialog();
            }
            
            @Override
            protected void onPostExecute(ArrayList<AppInfo> result) {
                dismissProgressDialog();
                appinfos = result;
        		Collections.sort(appinfos, new Comparator<AppInfo>(){
        			@Override
        			public int compare(AppInfo lai, AppInfo rai){
        				return lai.appname.compareTo(rai.appname);
        			}
        		});

        		ArrayList<String> apps = new ArrayList<String>();
        		for (int i = 0; i < appinfos.size(); i++){
        			apps.add(appinfos.get(i).appname);
        		}

        		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
        				MainActivity.this, 
        				android.R.layout.simple_list_item_multiple_choice,
        				apps);
        		appList.setAdapter(adapter);

        		appList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

        		load();
            }

            
		}.execute();
	}
	
    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(MainActivity.this);
    	mProgressDialog.setMessage(getString(R.string.app_loading_message));
    	mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new OnCancelListener() {
        	@Override
        	public void onCancel(DialogInterface dialog) {
        		Toast.makeText(getApplicationContext(), R.string.app_loading_cancel, Toast.LENGTH_SHORT).show();
        		finish();
        	}
        });
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;        
    }

}
