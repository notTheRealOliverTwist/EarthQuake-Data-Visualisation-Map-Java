package module6;

import java.util.ArrayList;

import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.UnfoldingMap;
import processing.core.PGraphics;

/** Implements a visual marker for ocean earthquakes on an earthquake map
 * 
 * @author UC San Diego Intermediate Software Development MOOC team
 * @author Your name here
 *
 */
public class OceanQuakeMarker extends EarthquakeMarker {
	private ArrayList<CityMarker> citiesByQuake = new ArrayList<CityMarker>();
	private UnfoldingMap map;
	private PGraphics currGraphics;
	
	public OceanQuakeMarker(PointFeature quake) {
		super(quake);
		
		// setting field in earthquake marker
		isOnLand = false;
	}
	

	/** Draw the earthquake as a square */
	@Override
	public void drawEarthquake(PGraphics pg, float x, float y) {
		pg.rect(x-radius, y-radius, 2*radius, 2*radius);
	}
	public void setImpactedCities(CityMarker cm) {
		citiesByQuake.add(cm);
	}
	public void setMap(UnfoldingMap m) {
		map = m;
	}
}
