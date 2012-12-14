package jp.teluru.Teluru;

import jp.teluru.Teluru.Config;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import com.camobile.mediaapp.util.InstPrefs;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.HttpAuthHandler;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.ViewFlipper;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import jp.teluru.Teluru.Utils;
import jp.teluru.Teluru.AnimationHelper;
import jp.teluru.Teluru.StoreMapOverlay;
import com.google.android.gcm.GCMRegistrar;

public class TeluruActivity extends MapActivity {
	public static TeluruActivity singleton;
	WebView webView;
	MapView storeMapView;
	WebView loginView;
	TabHost tabHost;
	ViewFlipper viewFlipper;
	Drawable storeMapMaker;
	SelfSignedSslHttpClient httpClient;
	boolean tahHostChangeFirst = true;
	boolean tabChangeExec = true;
	boolean showingLogin = false;
	String affiliate_id;
	boolean logged_in = false;

	private static final Animation inFromLeft = AnimationHelper
			.inFromLeftAnimation();
	private static final Animation outToRight = AnimationHelper
			.outToRightAnimation();

	// private static final Animation inFromRight =
	// AnimationHelper.inFromRightAnimation();
	// private static final Animation outToLeft =
	// AnimationHelper.outToLeftAnimation();

	@Override
	public void onNewIntent(Intent newIntent) {
		Log.d("onNewIntent", "URI:" + newIntent.getData());
		Uri uri = newIntent.getData();
		if (uri != null) {
			String host = newIntent.getData().getHost();
			Log.d("host ", "host :" + host);
			if (host.equals("push-news")) {
				Log.d("push", "news");
				if (!showingLogin) {
					setTabIndex(2, true);
				}
			} else if (host.equals("push-coupon")) {
				Log.d("push", "coupon");
				if (!showingLogin) {
					setTabIndex(3, true);
				}
			}
		}
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		singleton = this;
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		try {
			httpClient = new SelfSignedSslHttpClient();
			httpClient.httpclient.getCredentialsProvider().setCredentials(
					new AuthScope(Config.HOST_NAME, 443),
					new UsernamePasswordCredentials(Config.USER_NAME,
							Config.USER_PASSWORD));
		} catch (Exception e) {
			e.printStackTrace();
		}

		viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
		setupWebView();
		setupStoreMapView();
		setupLoginView();
		setupTabHost();
		setTabIndex(1, true);

		// ////////////INST
		InstPrefs.setInputAppNum("34961");// 固定サイトIDの場合は入力
		InstPrefs
				.setThankyouPageUrl("http://inst.qw.to/pages/index/50bdb93d-1c7c-48ca-8f3c-6b770a021585");// サンキューページURL
		// ////////////
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (showingLogin) {
			loadURLWithPath(loginView, "login");
		} else {
			webView.reload();
		}
	}

	private void setupWebView() {
		webView = (WebView) findViewById(R.id.webkit);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
		// webView.clearCache(true);
		webView.setBackgroundColor(Color
				.parseColor(Config.WEB_VIEW_BACKGROUND_COLOR));
		// WebViewClientを設定する
		webView.setWebViewClient(new WebViewClient() {
			// 認証リクエストがあった場合の挙動
			@Override
			public void onReceivedHttpAuthRequest(WebView view,
					HttpAuthHandler handler, String host, String realm) {
				handler.proceed(Config.USER_NAME, Config.USER_PASSWORD);
			}

			public boolean shouldOverrideUrlLoading(WebView view,
					String urlString) {
				Log.d("Access URL", urlString);
				String path = "";
				try {
					path = new URL(urlString).getPath();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}

				if (path.equals("/" + Config.URL_PATH_SETUP)) {
					updateAffiliateId();
				} else if (urlString.startsWith("mailto:")) {
					Intent intent = new Intent(Intent.ACTION_SENDTO, Uri
							.parse(urlString));
					startActivity(intent);
					webView.reload();
					return true;
				} else if (urlString.startsWith("tel:")) {
					Intent intent = new Intent(Intent.ACTION_DIAL, Uri
							.parse(urlString));
					startActivity(intent);
					webView.reload();
					return true;
				} else if (urlString.equals(Config.APP_URL_PATH_LOGIN)) {
					showingLogin = true;
					logged_in = false;
					Log.d("Teluru URL", Config.APP_URL_PATH_LOGIN);
					loadURLWithPath(loginView, "login");
					viewFlipper.setInAnimation(inFromLeft);
					viewFlipper.setOutAnimation(outToRight);
					viewFlipper.showNext();
					return true;
				} else if (urlString.equals(Config.APP_URL_PATH_STORES_MAP)) {
					tabHost.setCurrentTabByTag("shop");
				}
				// URLによってタブ選択を変える
				class PathToTabIndex {
					public String path;
					public int tabIndex;

					PathToTabIndex(String s, int t) {
						path = s;
						tabIndex = t;
					}
				}
				PathToTabIndex[] pathToIndexs = {
						new PathToTabIndex("/home", 1),
						new PathToTabIndex("/news", 2),
						new PathToTabIndex("/coupon", 3),
						new PathToTabIndex("/store", 4),
						new PathToTabIndex("/aftercare", 5), };
				for (PathToTabIndex data : pathToIndexs) {
					if (path.startsWith(data.path)
							&& data.tabIndex != tabHost.getCurrentTab()) {
						setTabIndex(data.tabIndex, false);
						break;
					}
				}
				return super.shouldOverrideUrlLoading(view, urlString);
			}

			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				Log.d("WEB ERROR", "error code:" + errorCode + " " + failingUrl);
				super.onReceivedError(view, errorCode, description, failingUrl);
			}

			public void onPageFinished(WebView view, String url) {
				String cookie = CookieManager.getInstance().getCookie(url);
				if (cookie != null) {
					String[] oneCookie = cookie.split(";");
					String host = null;
					try {
						host = new URL(url).getHost();
						for (String namAndVal : oneCookie) {
							namAndVal = namAndVal.trim();
							String[] cookieSet = namAndVal.split("=", 2);
							BasicClientCookie bCookie = new BasicClientCookie(
									cookieSet[0], cookieSet[1]);
							bCookie.setDomain(host);
							bCookie.setPath("/");
							CookieStore store = httpClient.httpclient
									.getCookieStore();
							// CookieStore store = httpClient.getCookieStore();
							store.addCookie(bCookie);
						}
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
				if (logged_in && affiliate_id == null) {
					updateAffiliateId();
				}
			}

			public void onReceivedSslError(WebView view,
					SslErrorHandler handler, SslError error) {
				handler.proceed();
			}
		});
		loadURLWithPath(webView, Config.URL_PATH_HOME);
	}

	private void setupStoreMapView() {
		storeMapView = (MapView) findViewById(R.id.storeMap);
		storeMapView.setEnabled(true);
		storeMapView.setClickable(true);
		storeMapView.setBuiltInZoomControls(false);
		storeMapView.getController().setZoom(16);
		final MyLocationOverlay overlay = new MyLocationOverlay(
				getApplicationContext(), storeMapView);
		overlay.onProviderEnabled(LocationManager.GPS_PROVIDER);
		overlay.enableMyLocation();
		overlay.runOnFirstFix(new Runnable() {
			public void run() {
				storeMapView.getController().animateTo(overlay.getMyLocation()); // 現在位置を自動追尾する
			}
		});
		storeMapView.getOverlays().add(overlay);

		storeMapMaker = getResources().getDrawable(
				R.drawable.store_map_marker_min);
		storeMapMaker.setBounds(0, 0, storeMapMaker.getIntrinsicWidth(),
				storeMapMaker.getIntrinsicHeight());
		// storeMapView.getOverlays().add(new StoreMapOverlay(storeMapMaker,
		// this));

		storeMapView.invalidate();
	}

	private void setupLoginView() {
		loginView = (WebView) findViewById(R.id.loginView);
		loginView.getSettings().setJavaScriptEnabled(true);
		loginView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
		loginView.setBackgroundColor(Color
				.parseColor(Config.WEB_VIEW_BACKGROUND_COLOR));
		loginView.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading(WebView view,
					String urlString) {
				if (urlString.substring(0, 7).equals("mailto:")) {
					Uri uri = Uri.parse(urlString);
					Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
					startActivity(intent);
					webView.reload();
					return true;
				} else if (urlString.equals(Config.APP_URL_PATH_LOGIN_COMPLETE)) {
					showingLogin = false;
					logged_in = true;
					Log.d("Teluru URL", Config.APP_URL_PATH_LOGIN_COMPLETE);
					loadURLWithPath(webView, "home");
					setTabIndex(0, true);
					viewFlipper.showPrevious();
					setupGCM();
					// updateAffiliateId();
					return true;
				}
				return super.shouldOverrideUrlLoading(view, urlString);
			}

			// 認証リクエストがあった場合の挙動
			@Override
			public void onReceivedHttpAuthRequest(WebView view,
					HttpAuthHandler handler, String host, String realm) {
				handler.proceed(Config.USER_NAME, Config.USER_PASSWORD);
			}
		});
	}

	private void setupTabHost() {
		tabHost = (TabHost) findViewById(R.id.tabhost);
		tabHost.setup();
		tabHost.addTab(createTabItem(tabHost, "hide", "", 0, R.id.webkit));
		tabHost.getTabWidget().getChildAt(0).setVisibility(View.GONE);
		tabHost.addTab(createTabItem(tabHost, "home", "ホーム",
				R.drawable.tabbar_home, R.id.webkit));
		tabHost.addTab(createTabItem(tabHost, "news", "ニュース",
				R.drawable.tabbar_news, R.id.webkit));
		tabHost.addTab(createTabItem(tabHost, "coupon", "クーポン",
				R.drawable.tabbar_coupon, R.id.webkit));
		tabHost.addTab(createTabItem(tabHost, "shop", "近くの店",
				R.drawable.tabbar_shop, R.id.storeMap));
		tabHost.addTab(createTabItem(tabHost, "after", "ｱﾌﾀｰｹｱ",
				R.drawable.tabbar_after, R.id.webkit));
		tabHost.addTab(createTabItem(tabHost, "download", "ｱﾌﾟﾘ一括",
				R.drawable.tabbar_download, R.id.webkit));

		tabHost.setOnTabChangedListener(new OnTabChangeListener() {
			public void onTabChanged(String tabId) {
				Log.d("tabHostObj", "onTabChanged occur!!! tabId : " + tabId);
				if (!tabChangeExec) {
					tabChangeExec = true;
					return;
				}
				if (tabId.equals("home")) {
					loadURLWithPath(webView, Config.URL_PATH_HOME);
				} else if (tabId.equals("news")) {
					loadURLWithPath(webView, Config.URL_PATH_NEWS);
				} else if (tabId.equals("coupon")) {
					loadURLWithPath(webView, Config.URL_PATH_COUPON);
				} else if (tabId.equals("shop")) {
					/*
					 * double viewWidth = webView.getWidth(); double M_PI =
					 * Math.PI; double longitudeDelta = 0.01; Double
					 * MERCATOR_RADIUS = 85445659.44705395; //int zoomLevel =
					 * 16; GeoPoint center = storeMapView.getMapCenter(); int
					 * zoomLevel = storeMapView.getZoomLevel(); double span =
					 * Math.exp((21 - zoomLevel) * Math.log(2.0)) /
					 * MERCATOR_RADIUS / M_PI * (180.0 * viewWidth);
					 * Log.d("span", Double.toString(span));
					 * updateMapStore(e6ToDeg(center.getLatitudeE6()),
					 * e6ToDeg(center.getLongitudeE6()), span, span);
					 */
					// MapViewの表示変更後のイベントが取れないため全ての店舗を取得する
					// TODO: 表示変更後のイベント取得方法を調査
					// GeoPoint center = storeMapView.getMapCenter();
					// updateMapStore(e6ToDeg(center.getLatitudeE6()),
					// e6ToDeg(center.getLongitudeE6()), 100.0, 100.0);
					updateMapStore(35, 139, 100.0, 100.0);
				} else if (tabId.equals("after")) {
					loadURLWithPath(webView, Config.URL_PATH_AFTERCARE);
				} else if (tabId.equals("download")) {
					loadURL(webView, Config.URL_PATH_DOWNLOAD);

					// //////////// TODO INST
					Intent i = new Intent(TeluruActivity.this,
							com.camobile.mediaapp.AppListActivity.class);
					startActivity(i);
					// ////////////
				}
			}
		});
	}

	private void setupGCM() {
		GCMRegistrar.checkManifest(this);
		GCMRegistrar.checkDevice(this);
		final String regId = GCMRegistrar.getRegistrationId(this);
		if (regId.equals("")) {
			GCMRegistrar.register(this, Config.GCM_SENDER_ID);
		} else {
			sendNotificationRegistrationId(regId);
		}
	}

	private void setTabIndex(int index, boolean exec) {
		tabChangeExec = exec;
		if (tahHostChangeFirst && index == 0) {
			tabHost.setCurrentTab(1);
			tabHost.setCurrentTab(0);
		} else {
			tabHost.setCurrentTab(index);
		}
		tahHostChangeFirst = false;
	}

	private void loadURL(WebView _webView, String url) {
		Log.d("Load URL", url);
		// _webView.clearView();
		_webView.loadUrl(url);
	}

	private void loadURLWithPath(WebView webView, String urlPath) {
		loadURL(webView, Utils.teluruUrl(urlPath));
	}

	private TabSpec createTabItem(TabHost tabs, String tag, String label,
			int drawableId, int viewId) {
		View view = LayoutInflater.from(this).inflate(R.layout.tab_item, null);

		if (drawableId != 0) {
			((ImageView) view.findViewById(R.id.tab_item_icon))
					.setImageResource(drawableId);
		}
		((TextView) view.findViewById(R.id.tab_item_text)).setText(label);

		TabHost.TabSpec spec = tabs.newTabSpec(tag);
		spec.setContent(viewId);
		spec.setIndicator(view);
		return spec;
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	private void updateMapStore(double latitude, double longitude,
			double latitudeDelta, double longitudeDelta) {
		class Param {
			double latitude;
			double longitude;
			double latitudeDelta;
			double longitudeDelta;
		}
		class ResultStore {
			String id;
			String name;
			double latitude;
			double longitude;
		}
		class Result {
			ArrayList<ResultStore> stores;

			public Result() {
				stores = new ArrayList<ResultStore>();
			}
		}
		AsyncTask<Param, Void, Result> task = new AsyncTask<Param, Void, Result>() {
			@Override
			protected Result doInBackground(Param... params) {
				Param param = params[0];
				Result result = new Result();

				String url = Utils.teluruUrl(Config.URL_PATH_API_STORES);
				// HttpPost post = new HttpPost(url);
				ArrayList<NameValuePair> postParams = new ArrayList<NameValuePair>();
				postParams.add(new BasicNameValuePair("latitude", Double
						.toString(param.latitude)));
				postParams.add(new BasicNameValuePair("longitude", Double
						.toString(param.longitude)));
				postParams.add(new BasicNameValuePair("latitudeDelta", Double
						.toString(param.latitudeDelta)));
				postParams.add(new BasicNameValuePair("longitudeDelta", Double
						.toString(param.longitudeDelta)));
				HttpResponse res = null;
				try {
					try {
						res = httpClient.post(url, postParams);
					} catch (Exception e) {
						e.printStackTrace();
					}
					int status = res.getStatusLine().getStatusCode();
					if (HttpStatus.SC_OK == status) {
						try {
							ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
							res.getEntity().writeTo(outputStream);
							Log.d("Http Output", outputStream.toString());
							try {
								JSONObject root = new JSONObject(
										outputStream.toString());
								if (root.getString("status").equals("ok")) {
									JSONArray stores = root
											.getJSONArray("stores");
									int count = stores.length();
									for (int i = 0; i < count; i++) {
										JSONObject store = (JSONObject) stores
												.get(i);
										String id = store.getString("id");
										String name = store.getString("name");
										Double latitude = Double
												.parseDouble(store
														.getString("latitude"));
										Double longitude = Double
												.parseDouble(store
														.getString("longitude"));
										ResultStore resultStore = new ResultStore();
										resultStore.id = id;
										resultStore.name = name;
										resultStore.latitude = latitude;
										resultStore.longitude = longitude;
										result.stores.add(resultStore);
									}
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						Log.d("HttpSampleActivity", "Status" + status);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return result;
			}

			@Override
			protected void onPostExecute(Result result) {
				for (ResultStore store : result.stores) {
					boolean exist = false;
					for (Overlay overlay : storeMapView.getOverlays()) {
						if (overlay instanceof StoreMapOverlay) {
							StoreMapOverlay storeMap = (StoreMapOverlay) overlay;
							if (storeMap.id.equals(store.id)) {
								exist = true;
								break;
							}
						}
					}
					if (!exist) {
						storeMapView.getOverlays().add(
								new StoreMapOverlay(storeMapMaker,
										TeluruActivity.this, store.id,
										store.name, store.latitude,
										store.longitude));
					}
				}
				storeMapView.invalidate();
			}
		};
		Param param = new Param();
		param.latitude = latitude;
		param.longitude = longitude;
		param.latitudeDelta = latitudeDelta;
		param.longitudeDelta = longitudeDelta;
		task.execute(param);
	}

	// private double e6ToDeg(int e6) {
	// return (double)e6 / 1000000.0;
	// }

	public void showStore(String id) {
		setTabIndex(0, true);
		loadURLWithPath(webView, Config.URL_PATH_STORE + '/' + id);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (showingLogin) {
				if (loginView.canGoBack() == true) {
					loginView.goBack();
				}
			} else {
				if (webView.canGoBack() == true) {
					webView.goBack();
				}
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	static public void sendNotificationRegistrationId(String registrationId) {
		AsyncTask<String, Void, Void> task = new AsyncTask<String, Void, Void>() {
			@Override
			protected Void doInBackground(String... registrationIds) {
				String url = Utils
						.teluruUrl(Config.URL_PATH_REGIST_NOTIFICATION_DEV_TOKEN);
				ArrayList<NameValuePair> postParams = new ArrayList<NameValuePair>();
				postParams.add(new BasicNameValuePair("dev_token",
						registrationIds[0]));
				HttpResponse res = null;
				SelfSignedSslHttpClient httpClient = singleton.httpClient;
				try {
					try {
						res = httpClient.post(url, postParams);
					} catch (Exception e) {
						e.printStackTrace();
					}
					int status = res.getStatusLine().getStatusCode();
					if (HttpStatus.SC_OK == status) {
						try {
							ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
							res.getEntity().writeTo(outputStream);
							Log.d("Http Output", outputStream.toString());
							try {
								JSONObject root = new JSONObject(
										outputStream.toString());
								if (root.getString("status").equals("ok")) {
									Log.d("Notification Registration Id",
											"Succeed");
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						Log.d("HttpSampleActivity", "Status" + status);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		};
		task.execute(registrationId);
	}

	private void updateAffiliateId() {
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				String url = Utils.teluruUrl(Config.URL_PATH_API_AFFILIATE_ID);
				HttpResponse res = null;
				SelfSignedSslHttpClient httpClient = singleton.httpClient;
				try {
					try {
						res = httpClient.get(url);
					} catch (Exception e) {
						e.printStackTrace();
					}
					int status = res.getStatusLine().getStatusCode();
					if (HttpStatus.SC_OK == status) {
						try {
							ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
							res.getEntity().writeTo(outputStream);
							Log.d("Http Output", outputStream.toString());
							try {
								JSONObject root = new JSONObject(
										outputStream.toString());
								affiliate_id = root.getString("affiliate_id");

								SharedPreferences pref = getSharedPreferences(
										Config.SHARED_PREF_NAME,
										MODE_WORLD_READABLE);
								Editor e = pref.edit();
								e.putString(Config.SHARED_PREF_AFFILIATE_ID,
										affiliate_id);
								e.commit();

								// //////////// TODO INST
								Log.d("", "affiliate_id" + affiliate_id);
								InstPrefs.setInputStoreNum(affiliate_id);// ショップID
								// ////////////
							} catch (JSONException e) {
								e.printStackTrace();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						Log.d("HttpSampleActivity", "Status" + status);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		};
		task.execute();
	}
}