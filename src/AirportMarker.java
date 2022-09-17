package module6;

import java.util.List;

import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import processing.core.PGraphics;

/** 
 * A class to represent AirportMarkers on a world map.
 *   
 * @Adam Setters && UCSandiego
 *
 */
public class AirportMarker extends CommonMarker {
	public static List<SimpleLinesMarker> routes;
	
	public AirportMarker(Feature city) {
		super(((PointFeature)city).getLocation(), city.getProperties());
	
	}
	
	@Override
	public void drawMarker(PGraphics pg, float x, float y) {
		pg.fill(255, 16, 240);
		pg.ellipse(x, y, 5, 5);
	}

	@Override
	public void showTitle(PGraphics pg, float x, float y) {
		String title = this.getStringProperty("name")+
				"\n"+
				this.getStringProperty("city")+
				"\n"+
				this.getStringProperty("country")+
				"\n"+
				this.getStringProperty("code");
		float widthOfTitle = pg.textWidth(title);
		pg.fill(255);
		pg.rect(x, y - 25, widthOfTitle+20, 80);
		pg.fill(0);
		pg.text(title, x, y);
	}
	
}
