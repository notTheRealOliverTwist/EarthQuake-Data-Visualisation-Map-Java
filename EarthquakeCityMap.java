package module6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.providers.OpenStreetMap;
import de.fhpotsdam.unfolding.providers.MBTilesMapProvider;
import de.fhpotsdam.unfolding.utils.MapUtils;
import parsing.ParseFeed;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * EarthquakeCityMap An application with an interactive map displaying
 * earthquake data. Author: UC San Diego Intermediate Software Development MOOC
 * team
 * 
 * @author Your name here Date: July 17, 2015
 */
public class EarthquakeCityMap extends PApplet {

	// We will use member variables, instead of local variables, to store the data
	// that the setup and draw methods will need to access (as well as other
	// methods)
	// You will use many of these variables, but the only one you should need to add
	// code to modify is countryQuakes, where you will store the number of
	// earthquakes
	// per country.

	// You can ignore this. It's to get rid of eclipse warnings
	private static final long serialVersionUID = 1L;

	// IF YOU ARE WORKING OFFILINE, change the value of this variable to true
	private static final boolean offline = false;

	/**
	 * This is where to find the local tiles, for working without an Internet
	 * connection
	 */
	public static String mbTilesString = "blankLight-1-3.mbtiles";

	// feed with magnitude 2.5+ Earthquakes
	private String earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.atom";

	// The files containing city names and info and country names and info
	private String cityFile = "city-data.json";
	private String countryFile = "countries.geo.json";
	private String airportFile = "airports.dat";

	// The map
	private UnfoldingMap map;
	private UnfoldingMap map2;

	// Markers for each city
	private List<Marker> cityMarkers;
	//Marker for global airports
	private List<Marker> airportMarkers;
	// Markers for each earthquake
	private List<Marker> quakeMarkers;

	// A List of country markers
	private List<Marker> countryMarkers;
	
	// Markers used for life expectancy map
	HashMap<String, Float> lifeExpMap;
	List<Feature> countries;

	// NEW IN MODULE 5
	private CommonMarker lastSelected;
	private CommonMarker lastClicked;
	
	public void setup() {
		// (1) Initializing canvas and map tiles
		size(displayWidth, displayHeight,OPENGL);
		if (offline) {
			map = new UnfoldingMap(this, 0, 0, displayWidth, displayHeight, new MBTilesMapProvider(mbTilesString));
			map2 = new UnfoldingMap(this,displayWidth-300,25,300,displayHeight,new MBTilesMapProvider(mbTilesString));
			//earthquakesURL = "2.5_week.atom"; // The same feed, but saved August 7, 2015
		} else {
			map = new UnfoldingMap(this, 0, 0, displayWidth, displayHeight, new OpenStreetMap.OpenStreetMapProvider());
			map2 = new UnfoldingMap(this,displayWidth-300,25,300,displayHeight,new Google.GoogleMapProvider());
			// IF YOU WANT TO TEST WITH A LOCAL FILE, uncomment the next line
			 //earthquakesURL = "test2.atom";
		}
		
		MapUtils.createDefaultEventDispatcher(this, map);
		MapUtils.createDefaultEventDispatcher(this, map2);

		// (2) Reading in earthquake data and geometric properties
		// STEP 1: load country features and markers
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);

		// STEP 2: read in city data
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for (Feature city : cities) {
			cityMarkers.add(new CityMarker(city));
		}
		List<PointFeature> airports = ParseFeed.parseAirports(this, airportFile);
		airportMarkers = new ArrayList<Marker>();
		
		for(PointFeature airport : airports) {
			AirportMarker m = new AirportMarker(airport);
			m.setRadius(15);
			airportMarkers.add(m);
		}

		// STEP 3: read in earthquake RSS feed
		List<PointFeature> earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
		quakeMarkers = new ArrayList<Marker>();

		for (PointFeature feature : earthquakes) {
			// check if LandQuake
			if (isLand(feature)) {
				quakeMarkers.add(new LandQuakeMarker(feature));
			}
			// OceanQuakes
			else {
				quakeMarkers.add(new OceanQuakeMarker(feature));
			}
		}
		
		lifeExpMap = ParseFeed.loadLifeExpectancyFromCSV(this,"LifeExpectancyWorldBank.csv");
		

		// Load country polygons and adds them as markers
		countries = GeoJSONReader.loadData(this, "countries.geo.json");
		countryMarkers = MapUtils.createSimpleMarkers(countries);
		map2.addMarkers(countryMarkers);

		// Country markers are shaded according to life expectancy (only once)
		shadeCountries();

		// could be used for debugging
		printQuakes();

		// (3) Add markers to map
		// NOTE: Country markers are not added to the map. They are used
		// for their geometric properties
		map.addMarkers(quakeMarkers);
		map.addMarkers(cityMarkers);
		map.addMarkers(airportMarkers);

		// Create the buffer to have the information cards display on top of
		// map icons
		sortAndPrint(10);

	} // End setup

	public void draw() {
		background(0);
		map.draw();
		map2.draw();
		addKey();
		if (lastSelected != null) {
			lastSelected.drawTitle(this.g, mouseX, mouseY);
		}
	}
	//Helper method to color each country based on life expectancy
	//Red-orange indicates low (near 40)
	//Blue indicates high (near 100)
	
	private void shadeCountries() {
		for (Marker marker : countryMarkers) {
			// Find data for country of the current marker
			String countryId = marker.getId();
			System.out.println(lifeExpMap.containsKey(countryId));
			if (lifeExpMap.containsKey(countryId)) {
				float lifeExp = lifeExpMap.get(countryId);
				// Encode value as brightness (values range: 40-90)
				int colorLevel = (int) map(lifeExp, 40, 90, 10, 255);
				marker.setColor(color(255-colorLevel, 100, colorLevel));
			}
			else {
				marker.setColor(color(150,150,150));
			}
		}
	}
	
	private void unhideCities() {
		for(Marker m : cityMarkers) {
			m.setHidden(false);
		}
	}
	
	private void hideCities() {
		for(Marker m : cityMarkers) {
			m.setHidden(true);
		}
	}
	private void hideQuakes() {
		for(Marker m : quakeMarkers) {
			m.setHidden(true);
		}
	}
	private void unhideQuakes() {
		for(Marker m : quakeMarkers) {
			m.setHidden(false);
		}
	}
	private void hideAirports() {
		for(Marker m : airportMarkers) {
			m.setHidden(true);
		}
	}
	private void unhideAirports() {
		for(Marker m : airportMarkers) {
			m.setHidden(false);
		}
	}
	/**
	 * Event handler that gets called automatically when the mouse moves.
	 */
	@Override
	public void mouseMoved() {
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;

		}
		selectMarkerIfHover(quakeMarkers);
		selectMarkerIfHover(cityMarkers);
		selectMarkerIfHover(airportMarkers);
	}

	// If there is a marker under the cursor, and lastSelected is null
	// set the lastSelected to be the first marker found under the cursor
	// Make sure you do not select two markers.
	//
	private void selectMarkerIfHover(List<Marker> markers) {
		// TODO: Implement this method
		for (Marker m : markers) {
			if (m.isInside(map, mouseX, mouseY)) {
				lastSelected = (CommonMarker) m;
				lastSelected.setSelected(true);
				System.out.println(m.getStringProperty("name"));
				break;
			}
		}
	}
	

	/**
	 * The event handler for mouse clicks It will display an earthquake and its
	 * threat circle of cities Or if a city is clicked, it will display all the
	 * earthquakes where the city is in the threat circle
	 */
	@Override
	public void mouseClicked()
	{
		if (lastClicked != null) {
			unhideMarkers();
			lastClicked = null;
		}
		else {
			clearMarkers();
			if(checkEarthquakeMarker() != null) {
				Marker marker = checkEarthquakeMarker();
				displayCity(marker);
				displayAirports(marker);
				marker.setHidden(false);
			}
			else if(checkCityMarker() != null) {
				Marker marker = checkCityMarker();
				displayEarthquake(marker);
				marker.setHidden(false);
			}
			else {
				unhideMarkers();
			}
		}
	}
	
	private Marker checkEarthquakeMarker() {
		for(Marker marker: quakeMarkers) {
			if(marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker) marker;
				return marker;
			}
		}
		return null;
	}
	
	private Marker checkCityMarker() {
		for(Marker marker: cityMarkers){
			if(marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker) marker;
				return marker;
			}
		}
		return null;
	}
	
	private void displayAirports(Marker marker) {
		double rad = ((EarthquakeMarker)marker).threatCircle();
		for(Marker airport: airportMarkers) {
			Location point = airport.getLocation();
			double dis = marker.getDistanceTo(point);
			if(Math.abs(dis) <= Math.abs(rad)) {
				airport.setHidden(false);
			}
			else {
				airport.setHidden(true);
			}
		}
	}
	
	private void displayCity(Marker marker) {
		double rad = ((EarthquakeMarker)marker).threatCircle();
		for(Marker city: cityMarkers) {
			Location point = city.getLocation();
			double dis = marker.getDistanceTo(point);
			if(Math.abs(dis) <= Math.abs(rad)) {
				city.setHidden(false);
			}
			else {
				city.setHidden(true);
			}
		}
	}
	
	private void clearMarkers() {
		hideCities();
		hideAirports();
		hideQuakes();
	}
	
	private void displayEarthquake(Marker marker) {
		Location point = marker.getLocation();
		for(Marker quakemarker: quakeMarkers) {
			double dis = quakemarker.getDistanceTo(point);
			double rad = ((EarthquakeMarker)quakemarker).threatCircle();
			if(Math.abs(dis) <= Math.abs(rad)) {
				quakemarker.setHidden(false);
			}
			else {
				quakemarker.setHidden(true);
			}
		}
	}

	// loop over and unhide all markers
	private void unhideMarkers() {
		unhideCities();
		unhideAirports();
		unhideQuakes();
	}

	// helper method to draw key in GUI
	private void addKey() {
		// Remember you can use Processing's graphics methods here
		fill(255, 250, 240);

		int xbase = 25;
		int ybase = 50;

		rect(0, 0, 300, displayHeight);

		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Earthquake Key", xbase + 25, ybase + 25);

		fill(150, 30, 30);
		int tri_xbase = xbase + 35;
		int tri_ybase = ybase + 70;
		triangle(tri_xbase, tri_ybase - CityMarker.TRI_SIZE, tri_xbase - CityMarker.TRI_SIZE,
				tri_ybase + CityMarker.TRI_SIZE, tri_xbase + CityMarker.TRI_SIZE, tri_ybase + CityMarker.TRI_SIZE);
		
		fill(255, 16, 240);
		ellipse(xbase+35,ybase+50,10,10);
		
		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
		text("Airport Marker",xbase+50,ybase+50);
		text("City Marker", tri_xbase + 15, tri_ybase);

		text("Land Quake", xbase + 50, ybase + 90);
		text("Ocean Quake", xbase + 50, ybase + 110);
		text("Size ~ Magnitude", xbase + 25, ybase + 130);

		fill(255, 255, 255);
		ellipse(xbase + 35, ybase + 90, 10, 10);
		rect(xbase + 35 - 5, ybase + 110 - 5, 10, 10);

		fill(color(255, 255, 0));
		ellipse(xbase + 35, ybase + 150, 12, 12);
		fill(color(0, 0, 255));
		ellipse(xbase + 35, ybase + 180, 12, 12);
		fill(color(255, 0, 0));
		ellipse(xbase + 35, ybase + 200, 12, 12);

		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Shallow", xbase + 50, ybase + 150);
		text("Intermediate", xbase + 50, ybase + 180);
		text("Deep", xbase + 50, ybase + 200);

		text("Past hour", xbase + 50, ybase + 220);

		fill(255, 255, 255);
		int centerx = xbase + 35;
		int centery = ybase + 220;
		ellipse(centerx, centery, 12, 12);

		strokeWeight(2);
		line(centerx - 8, centery - 8, centerx + 8, centery + 8);
		line(centerx - 8, centery + 8, centerx + 8, centery - 8);
		
		fill(0);
		text("Secondary Map ~~ Life Expectancy", xbase+25,ybase+260);
		text("Indicates Longer Life Span",xbase+45,ybase+290);
		text("Indicates Shorter Life Span",xbase+45,ybase+310);
		fill(color(0,0, 255));
		ellipse(xbase + 35, ybase + 290, 12, 12);
		fill(color(255, 0, 0));
		ellipse(xbase + 35, ybase + 310, 12, 12);
		
		fill(255, 250, 240);
		rect(displayWidth-300, -50, 300, 225);
		fill(0);
		textSize(20);
		text("Life Expectancy Graph: ", displayWidth - 265, 75);
	}
	
	// Checks whether this quake occurred on land. If it did, it sets the
	// "country" property of its PointFeature to the country where it occurred
	// and returns true. Notice that the helper method isInCountry will
	// set this "country" property already. Otherwise it returns false.
	private boolean isLand(PointFeature earthquake) {

		// IMPLEMENT THIS: loop over all countries to check if location is in any of
		// them
		// If it is, add 1 to the entry in countryQuakes corresponding to this country.
		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}

		// not inside any country
		return false;
	}

	// prints countries with number of earthquakes
	private void printQuakes() {
		int totalWaterQuakes = quakeMarkers.size();
		for (Marker country : countryMarkers) {
			String countryName = country.getStringProperty("name");
			int numQuakes = 0;
			for (Marker marker : quakeMarkers) {
				EarthquakeMarker eqMarker = (EarthquakeMarker) marker;
				if (eqMarker.isOnLand()) {
					if (countryName.equals(eqMarker.getStringProperty("country"))) {
						numQuakes++;
					}
				}
			}
			if (numQuakes > 0) {
				totalWaterQuakes -= numQuakes;
				System.out.println(countryName + ": " + numQuakes);
			}
		}
		System.out.println("OCEAN QUAKES: " + totalWaterQuakes);
	}

	// helper method to test whether a given earthquake is in a given country
	// This will also add the country property to the properties of the earthquake
	// feature if
	// it's in one of the countries.
	// You should not have to modify this code
	private boolean isInCountry(PointFeature earthquake, Marker country) {
		// getting location of feature
		Location checkLoc = earthquake.getLocation();

		// some countries represented it as MultiMarker
		// looping over SimplePolygonMarkers which make them up to use isInsideByLoc
		if (country.getClass() == MultiMarker.class) {

			// looping over markers making up MultiMarker
			for (Marker marker : ((MultiMarker) country).getMarkers()) {

				// checking if inside
				if (((AbstractShapeMarker) marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));

					// return if is inside one
					return true;
				}
			}
		}

		// check if inside country represented by SimplePolygonMarker
		else if (((AbstractShapeMarker) country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));

			return true;
		}
		return false;
	}
	
	private void sortAndPrint(int numToPrint) {
		ArrayList<EarthquakeMarker> sortedQuakes = new ArrayList<EarthquakeMarker>();
		for(Marker m : quakeMarkers){
			sortedQuakes.add((EarthquakeMarker)m);
		}
		Collections.sort(sortedQuakes);
		for(int i = 0; i < numToPrint; i++) {
			if(i < sortedQuakes.size()) {
				System.out.println(sortedQuakes.get(i).getTitle());
			}
			else {
				break;
			}
		}
	}
}
