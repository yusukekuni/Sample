package jp.teluru.Teluru;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import jp.teluru.Teluru.Config;
import jp.teluru.Teluru.Utils;
import jp.teluru.Teluru.AnimationHelper;
import jp.teluru.Teluru.TeluruActivity;

public class StoreMapOverlay extends ItemizedOverlay<OverlayItem> {
	private List<OverlayItem> items=new ArrayList<OverlayItem>();
	private Drawable marker=null;
	private TeluruActivity activity; 
	public final String id;
	private String name;
	private Double latitude;
	private Double longitude;
	
	public StoreMapOverlay(Drawable marker, TeluruActivity activity, String id, String name, Double latitude, Double longitude) {
		super(marker);
		this.marker=marker;
		this.activity=activity;
		this.id = id;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		items.add(new OverlayItem(getPoint(latitude, longitude), name, name));
		populate();
	}
	
	@Override
	protected OverlayItem createItem(int i) {
		return(items.get(i));
	}
	
	@Override
	public void draw(Canvas canvas, MapView mapView,
										boolean shadow) {
		super.draw(canvas, mapView, shadow);
		
		boundCenterBottom(marker);
	}
	
	@Override
	protected boolean onTap(int i) {
		/*
		Toast.makeText(this.activity,
										items.get(i).getSnippet(),
										Toast.LENGTH_SHORT).show();
		*/
		this.activity.showStore(this.id);
		return(true);
	}
	
	@Override
	public int size() {
		return(items.size());
	}

	private GeoPoint getPoint(double lat, double lon) {
		return(new GeoPoint((int)(lat*1000000.0), (int)(lon*1000000.0)));
	}
}

