package ua.naiksoftware.j2meloader;

import java.io.*;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import com.android.dx.command.Main;
import filelog.Log;
import javax.microedition.shell.ConfScreen;
import ua.naiksoftware.util.FileUtils;

public class JarConverter extends AsyncTask<String, String, Boolean> {

	private static final String tag = "JarConverter";

	private final Context context;
	private String err = "Void error";
	private ProgressDialog dialog;

	private String pathToJar, appDir, pathConverted;
	private final File dirTmp, dirForJAssist;

	private static final String classRepl = "java.lang.Class";
	private static final String methodRepl = "getResourceAsStream";

	public JarConverter(MainActivity context) {
		this.context = context;
		dirTmp = new File(context.getApplicationInfo().dataDir + "/tmp");
		dirForJAssist = new File(dirTmp, "/_converted_classes");
	}

	@Override
	protected Boolean doInBackground(String... p1) {
		pathToJar = p1[0];
		pathConverted = p1[1];
		Log.d(tag, "doInBackground$ pathToJar=" + pathToJar + " pathConverted="
				+ pathConverted);
		FileUtils.deleteDirectory(dirTmp);
		dirTmp.mkdir();
		try {
			if (!FileUtils.unzip(new FileInputStream(new File(pathToJar)),
					dirTmp)) {
				err = "Brocken jar";
				return false;
			}
		} catch (FileNotFoundException e) {
			err = e.getMessage();
			return false;
		}

		appDir = FileUtils.loadManifest(
				new File(dirTmp, "/META-INF/MANIFEST.MF")).get("MIDlet-Name");
		File appConverted = new File(pathConverted + appDir);
		FileUtils.deleteDirectory(appConverted);
		appConverted.mkdirs();
		// dirForJAssist.mkdir();
		// workModifyClass(dirTmp.getPath()); // MODIFY
		// Log.d(tag, "-------\n\nreplace OK");
		// Convert to dex
		Log.d(tag, "appConverted=" + appConverted.getPath());
		Main.main(new String[] {
				"--dex",
				"--output=" + appConverted.getPath()
						+ ConfScreen.MIDLET_DEX_FILE,
				/* dirForJAssist.getPath() */pathToJar });
		File conf = new File(dirTmp, "/META-INF/MANIFEST.MF");
		if (!conf.exists()) {
			err = "Manifest not exists: " + conf.getPath();
			return false;
		}
		conf.renameTo(new File(appConverted, ConfScreen.MIDLET_CONF_FILE));
		// Extract other resources from jar.
		FileUtils.moveFiles(dirTmp.getPath(), pathConverted + appDir
				+ ConfScreen.MIDLET_RES_DIR, new FilenameFilter() {
			public boolean accept(File dir, String fname) {
				if (fname.equalsIgnoreCase("MANIFEST.MF")
						|| fname.endsWith(".class")) {
					return false;
				} else {
					return true;
				}
			}
		});
		FileUtils.deleteDirectory(dirTmp);
		return true;
	}

	@Override
	public void onProgressUpdate(String... s) {

	}

	@Override
	public void onPreExecute() {
		// Log.i(tag, "onPreExecute");
		dialog = new ProgressDialog(context);
		dialog.setIndeterminate(true);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setCancelable(false);
		dialog.setTitle(R.string.converting_wait);
		dialog.show();
	}

	@Override
	public void onPostExecute(Boolean result) {
		// Log.i(tag, "onPostExecute with: " + result);
		Toast t;
		if (result) {
			t = Toast.makeText(context,
					context.getResources().getString(R.string.convert_complete)
							+ " " + appDir, Toast.LENGTH_LONG);
			((MainActivity) context).updateApps();
		} else {
			t = Toast.makeText(context, err, Toast.LENGTH_LONG);

		}
		dialog.dismiss();
		t.show();
	}
}
