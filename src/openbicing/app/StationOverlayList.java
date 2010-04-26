package openbicing.app;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.google.android.maps.Overlay;

public class StationOverlayList {

	private List<Overlay> mapOverlays;
	private Context context;
	private HomeOverlay hOverlay;
	private Handler handler;
	private int current = -1;
	private int first;
	private StationOverlay last = null;

	public StationOverlayList(Context context, List<Overlay> mapOverlays,
			Handler handler) {
		this.context = context;
		this.mapOverlays = mapOverlays;
		this.handler = handler;
		hOverlay = new HomeOverlay(this.context, handler);
		hOverlay.setLastKnownLocation();
		addHome();
	}

	public List<Overlay> getList() {
		return mapOverlays;
	}

	public void addStationOverlay(Overlay overlay) {
		if (overlay instanceof StationOverlay) {
			StationOverlay sht = (StationOverlay) overlay;
			sht.setHandler(handler);
		}
		this.mapOverlays.add(overlay);
		if (this.current == -1) {
			this.current = 0;
			this.first = 0;
		}
	}
	
	public StationOverlay get(int position){
		if (mapOverlays.get(position) instanceof StationOverlay){
			return (StationOverlay) mapOverlays.get(position);
		}else{
			return null;
		}
	}

	public void setCurrent(int position) {
		StationOverlay sht;
		if (this.last != null && this.last instanceof StationOverlay)
			this.last.setSelected(false);
		else {
			sht = get(position);
			if (sht!=null)
				sht.setSelected(false);
		}
		this.current = position;
		sht = get(position);
		if (sht!=null){
			this.last = sht;
			this.last.setSelected(true);
		}
	}

	public void updatePositions() {
		int i = 0;
		StationOverlay tmp;
		while (i < mapOverlays.size()) {
			if (mapOverlays.get(i) instanceof StationOverlay) {
				tmp = (StationOverlay) mapOverlays.get(i);
				tmp.setPosition(i);
			}
			i++;
		}
		
	}

	public void addHome() {
		mapOverlays.add(hOverlay);
	}

	public void addStationOverlay(int location, Overlay overlay) {
		this.mapOverlays.add(location, overlay);
	}

	public void setStationOverlay(int location, Overlay overlay) {
		this.mapOverlays.set(location, overlay);
	}

	public void updateStationOverlay(int location) {
		StationOverlay station = (StationOverlay) this.mapOverlays
				.get(location);
		station.update();
	}

	public void updateHome() {
		hOverlay.setLastKnownLocation();
	}

	public void clear() {
		mapOverlays.clear();
		current = -1;
		addHome();
	}

	public HomeOverlay getHome() {
		return hOverlay;
	}

	public StationOverlay getCurrent() {
		if (current!=-1){
			if(mapOverlays.size()>1){
				if (!(mapOverlays.get(current) instanceof StationOverlay))
					current++;
				this.last = (StationOverlay) mapOverlays.get(current);
				return (StationOverlay) mapOverlays.get(current);
			}else{
				return null;
			}
		}else
			return null;
	}

	public StationOverlay selectNext() {
		if (last != null)
			last.setSelected(false);
		else {
			StationOverlay sht = (StationOverlay) mapOverlays.get(current);
			sht.setSelected(false);
		}
		do {
			current++;
			if (current > mapOverlays.size() - 1)
				current = first;
		} while (!(mapOverlays.get(current) instanceof StationOverlay)
				&& mapOverlays.size() > 1);

		if (mapOverlays.get(current) instanceof StationOverlay) {
			StationOverlay res = (StationOverlay) mapOverlays.get(current);
			res.setSelected(true);
			last = res;
			return res;
		} else
			return null;

	}
	
	public StationOverlay findById(int id){
		Iterator i = mapOverlays.iterator();
		StationOverlay tmp;
		Object aws;
		while(i.hasNext()){
			aws = i.next();
			if(aws instanceof StationOverlay){
				tmp = (StationOverlay) aws;
				if (tmp.getId() == id)
					return tmp;
			}
		}
		return null;
	}

	public StationOverlay selectPrevious() {
		if (last != null)
			last.setSelected(false);
		else {
			StationOverlay sht = (StationOverlay) mapOverlays.get(current);
			sht.setSelected(false);
		}
		
		do {
			current--;
			if (current < 0) {
				current = mapOverlays.size() - 1;
			}
		} while (!(mapOverlays.get(current) instanceof StationOverlay)
				&& mapOverlays.size() > 1);

		if (mapOverlays.get(current) instanceof StationOverlay) {
			StationOverlay res = (StationOverlay) mapOverlays.get(current);
			res.setSelected(true);
			last = res;
			return res;
		} else {
			return null;
		}
	}
}
