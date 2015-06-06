package com.kylemsguy.safefishing.kml;

public class Placemark {
	public double lat, lon;
	public String name;
	public String description;
	public String toString() {
		return "name: " + name + " lat: " + lat + " lon: " + lon + " description: " + description;
	}
}