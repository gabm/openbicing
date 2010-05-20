package net.homelinux.penecoptero.android.openbicing.app;

import java.util.List;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MainActivity extends MapActivity {

	private MapView mapView;
	public static final int MENU_ITEM_SYNC = Menu.FIRST;
	public static final int MENU_ITEM_LOCATION = Menu.FIRST + 1;
	public static final int MENU_ITEM_WHATEVER = Menu.FIRST + 2;
	public static final int MENU_ITEM_LIST = Menu.FIRST + 3;
	public static final int MENU_ITEM_SETTINGS = Menu.FIRST + 4;
	public static final int MENU_ITEM_HELP = Menu.FIRST + 5;
	public static final int KEY_LAT = 0;
	public static final int KEY_LNG = 1;
	public static final int SETTINGS_ACTIVITY = 0;

	private StationOverlayList stations;
	private StationsDBAdapter mDbHelper;
	private InfoLayer infoLayer;
	private boolean view_all = false;
	private HomeOverlay hOverlay;
	private ProgressDialog progressDialog;
	private FrameLayout fl;
	private SlidingDrawer sd;

	private Handler infoLayerPopulator;

	private int green, red, yellow;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mapView = (MapView) findViewById(R.id.mapview);
		fl = (FrameLayout) findViewById(R.id.content);
		sd = (SlidingDrawer) findViewById(R.id.drawer);
		infoLayer = (InfoLayer) findViewById(R.id.info_layer);

		infoLayerPopulator = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == InfoLayer.POPULATE) {
					infoLayer.inflateStation(stations.get(msg.arg1));
				}
			}
		};

		RelativeLayout.LayoutParams zoomControlsLayoutParams = new RelativeLayout.LayoutParams(
				android.view.ViewGroup.LayoutParams.FILL_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		zoomControlsLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		zoomControlsLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

		mapView.addView(mapView.getZoomControls(), zoomControlsLayoutParams);

		List<Overlay> mapOverlays = mapView.getOverlays();

		stations = new StationOverlayList(this, mapOverlays, new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == hOverlay.MOTION_CIRCLE_STOP && !view_all) {
					// Home Circle has changed its radius
					try {
						view_near();
					} catch (Exception e) {

					}
				} else if (msg.what == StationOverlay.TOUCHED && msg.arg1 != -1) {
					// One station has been touched
					stations.setCurrent(msg.arg1);
					infoLayer.inflateStation(stations.getCurrent());
				} else if (msg.what == hOverlay.LOCATION_CHANGED) {
					// Location has changed
					mDbHelper.setCenter(hOverlay.getPoint());
					try {
						mDbHelper.updateDistances(hOverlay.getPoint());
						infoLayer.update();

						if (view_all) {
							view_all();
						} else {
							view_near();
						}
					} catch (Exception e) {

					}
					;
				}
			}
		});

		mDbHelper = new StationsDBAdapter(this, mapView, new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case StationsDBAdapter.FETCH:
					////Log.i("openBicing", "Data fetched");
					break;
				case StationsDBAdapter.UPDATE_MAP:
					////Log.i("openBicing", "Map Updated");
					progressDialog.dismiss();
					StationOverlay current = stations.getCurrent();
					if (current == null) {
						infoLayer
								.inflateMessage(getString(R.string.no_bikes_around));
					}
					if (current != null) {
						current.setSelected(true);
						infoLayer.inflateStation(current);
						if (view_all)
							view_all();
						else
							view_near();
					} else {
						//Log.i("openBicing", "Error getting an station..");
					}
					mapView.invalidate();
					break;
				case StationsDBAdapter.UPDATE_DATABASE:
					
					//Log.i("openBicing", "Database updated");
					break;
				case StationsDBAdapter.NETWORK_ERROR:
					//Log.i("openBicing", "Network error, last update from " + mDbHelper.getLastUpdated());
					Toast toast = Toast.makeText(getApplicationContext(),
							getString(R.string.network_error)
									+ mDbHelper.getLastUpdated(),
							Toast.LENGTH_LONG);
					toast.show();
					break;
				}
			}
		}, stations);

		mDbHelper.setCenter(stations.getHome().getPoint());

		if (savedInstanceState != null) {
			stations.updateHome();
			stations.getHome().setRadius(
					savedInstanceState.getInt("homeRadius"));
			this.view_all = savedInstanceState.getBoolean("view_all");
		} else {
			updateHome();
		}

		try {
			mDbHelper.loadStations();
			if (savedInstanceState == null) {
				String strUpdated = mDbHelper.getLastUpdated();
				if (strUpdated == null) {
					this.fillData(view_all);
				} else {
					Toast toast = Toast.makeText(this.getApplicationContext(),
							"Last Updated: " + mDbHelper.getLastUpdated(),
							Toast.LENGTH_LONG);
					toast.show();
				}
			}

		} catch (Exception e) {
			//Log.i("openBicing", "SHIT ... SUCKS");
		}
		;

		if (view_all)
			view_all();
		else
			view_near();
		hOverlay = stations.getHome();
		//Log.i("openBicing", "CREATE!");
	}

	private void fillData(boolean all) {
		Bundle data = new Bundle();
		if (!all) {
			GeoPoint center = stations.getHome().getPoint();
			data.putInt(StationsDBAdapter.CENTER_LAT_KEY, center
					.getLatitudeE6());
			data.putInt(StationsDBAdapter.CENTER_LNG_KEY, center
					.getLongitudeE6());
			data.putInt(StationsDBAdapter.RADIUS_KEY, stations.getHome()
					.getRadius());
		}

		data.putString(StationsDBAdapter.STATION_PROVIDER_KEY,
				StationsDBAdapter.BICING_PROVIDER);

		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle("");
		progressDialog.setMessage(getString(R.string.loading));
		progressDialog.show();
		try {
			mDbHelper.sync(all, data);
		} catch (Exception e) {
			//Log.i("openBicing", "Error Updating?");
			e.printStackTrace();
			progressDialog.dismiss();
		}
		;
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_ITEM_SYNC, 0, R.string.menu_sync).setIcon(
				R.drawable.ic_menu_refresh);
		menu.add(0, MENU_ITEM_LOCATION, 0, R.string.menu_location).setIcon(
				android.R.drawable.ic_menu_mylocation);
		menu.add(0, MENU_ITEM_WHATEVER, 0, R.string.menu_view_all).setIcon(
				android.R.drawable.checkbox_off_background);
		menu.add(0, MENU_ITEM_SETTINGS, 0, R.string.menu_settings).setIcon(
				android.R.drawable.ic_menu_preferences);
		return true;
	}

	public void updateHome() {
		try {
			stations.updateHome();
			mapView.getController().setCenter(stations.getHome().getPoint());
			mapView.getController().setZoom(16);
		} catch (Exception e) {
			//Log.i("openBicing", "center is null..");
		}
	}

	public void view_all() {
		try {
			mDbHelper.populateStations();
			populateList(true);
		} catch (Exception e) {

		}
		;
	}

	public void view_near() {
		try {
			mDbHelper.populateStations(stations.getHome().getPoint(), stations
					.getHome().getRadius());
			populateList(false);
			if (!infoLayer.isPopulated()) {
				StationOverlay current = stations.getCurrent();
				if (current != null) {
					infoLayer.inflateStation(current);
					current.setSelected(true);
				} else {
					infoLayer
							.inflateMessage(getString(R.string.no_bikes_around));
				}
			}
		} catch (Exception e) {

		}
		;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_SYNC:
			try {
				this.fillData(view_all);
			} catch (Exception e) {

			}
			;
			return true;
		case MENU_ITEM_LOCATION:
			updateHome();
			return true;
		case MENU_ITEM_WHATEVER:
			if (!view_all) {
				item.setIcon(android.R.drawable.checkbox_on_background);
				view_all();
			} else {
				item.setIcon(android.R.drawable.checkbox_off_background);
				view_near();
			}
			view_all = !view_all;
			return true;
		case MENU_ITEM_SETTINGS:
			this
					.startActivityForResult(new Intent(this,
							SettingsActivity.class), SETTINGS_ACTIVITY);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		//Log.i("openBicing", "RESUME!");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		//Log.i("openBicing", "SaveInstanceState!");
		outState.putInt("homeRadius", stations.getHome().getRadius());
		outState.putBoolean("view_all", view_all);
	}

	@Override
	protected void onPause() {
		super.onPause();
		//Log.i("openBicing", "PAUSE!");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Log.i("openBicing", "DESTROY!");

	}

	@Override
	protected void onStop() {
		super.onStop();
		//Log.i("openBicing", "STOP!");
		hOverlay.stopUpdates();
		if (this.isFinishing())
			this.finish();

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.i("openBicing", "Activity Result");
		if (requestCode == SETTINGS_ACTIVITY) {
			if (resultCode == RESULT_OK) {
				hOverlay.restartUpdates();
			}
		}
	}

	public void populateList(boolean all) {
		try {
			ListView lv = new ListView(this);
			List sts;
			if (all) {
				sts = mDbHelper.getMemory();
			} else {
				sts = mDbHelper.getMemory(hOverlay.getRadius());
			}

			green = R.drawable.green_gradient;
			yellow = R.drawable.yellow_gradient;
			red = R.drawable.red_gradient;
			ArrayAdapter adapter = new ArrayAdapter(this,
					R.layout.stations_list_item, sts) {
				LayoutInflater mInflater = getLayoutInflater();

				@Override
				public View getView(int position, View convertView,
						ViewGroup parent) {
					View row;
					if (convertView == null) {
						row = mInflater.inflate(R.layout.stations_list_item,
								null);
					} else {
						row = convertView;
					}
					StationOverlay tmp = (StationOverlay) getItem(position);
					TextView stId = (TextView) row
							.findViewById(R.id.station_list_item_id);
					stId.setText(tmp.getName());
					TextView stOc = (TextView) row
							.findViewById(R.id.station_list_item_ocupation);
					stOc.setText(tmp.getOcupation());
					TextView stDst = (TextView) row
							.findViewById(R.id.station_list_item_distance);
					stDst.setText(tmp.getDistance());
					TextView stWk = (TextView) row
							.findViewById(R.id.station_list_item_walking_time);
					stWk.setText(tmp.getWalking());

					int bg;
					switch (tmp.getState()) {
					case StationOverlay.GREEN_STATE:
						bg = green;
						break;
					case StationOverlay.RED_STATE:
						bg = red;
						break;
					case StationOverlay.YELLOW_STATE:
						bg = yellow;
						break;
					default:
						bg = R.drawable.fancy_gradient;
					}
					LinearLayout sq = (LinearLayout) row
							.findViewById(R.id.station_list_item_square);
					sq.setBackgroundResource(bg);
					row.setId(tmp.getId());
					return row;
				}
			};
			lv.setAdapter(adapter);

			lv.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View v,
						int position, long id) {

					int pos = v.getId();
					if (pos != -1) {
						StationOverlay selected = stations.findById(pos);
						if (selected != null) {
							stations.setCurrent(selected.getPosition());
							Message tmp = new Message();
							tmp.what = InfoLayer.POPULATE;
							tmp.arg1 = selected.getPosition();
							infoLayerPopulator.dispatchMessage(tmp);
							mapView.getController().animateTo(
									selected.getCenter());
							int height = arg0.getHeight();
							DisplayMetrics dm = new DisplayMetrics();
							getWindowManager().getDefaultDisplay().getMetrics(
									dm);
							int w_height = dm.heightPixels;
							if (height > w_height / 2) {
								sd.animateClose();
							}
						}
					}
				}
			});
			lv.setBackgroundColor(Color.BLACK);
			lv.setLayoutParams(new LayoutParams(
					android.view.ViewGroup.LayoutParams.FILL_PARENT,
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
			fl.setBackgroundColor(Color.BLACK);
			fl.removeAllViews();
			fl.addView(lv);

			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			int height = dm.heightPixels;
			int calc = (lv.getCount() * 50) + 45;
			if (calc > height - 145)
				calc = height - 145;
			else if (lv.getCount() == 0)
				calc = 0;
			sd.setLayoutParams(new LayoutParams(
					android.view.ViewGroup.LayoutParams.FILL_PARENT, calc));
			//Log.i("openBicing", Integer.toString(fl.getHeight()));
		} catch (Exception e) {
			////Log.i("openBicing", "SHIT THIS SUCKS MEN ARGH FUCK IT!");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
