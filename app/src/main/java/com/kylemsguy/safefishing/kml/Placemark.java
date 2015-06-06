package com.kylemsguy.safefishing.kml;

public class Placemark {
	public double lat, lon;
	public String description;
	public String toString() {
		return "lat: " + lat + " lon: " + lon + " description: " + description;
	}
}