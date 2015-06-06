package com.kylemsguy.fishyfishes.kml;

public class Placemark {
	public double lat, lon;
	public String description;
	public String name;
	public String toString() {
		return "lat: " + lat + " lon: " + lon + " description: " + description;
	}
}