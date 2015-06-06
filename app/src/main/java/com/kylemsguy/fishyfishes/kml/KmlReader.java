package com.kylemsguy.fishyfishes.kml;

import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class KmlReader {
	public static Placemark[] getPlacemarks(InputStream is) throws Exception {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().
			parse(is);
		NodeList placemarks = document.getElementsByTagName("Placemark");
		Placemark[] retval = new Placemark[placemarks.getLength()];
		for (int i = 0; i < placemarks.getLength(); i++) {
			Element placemark = (Element) placemarks.item(i);
			double lon = Double.parseDouble(placemark.getElementsByTagName("longitude").item(0).getTextContent());
			double lat = Double.parseDouble(placemark.getElementsByTagName("latitude").item(0).getTextContent());
			String description = placemark.getElementsByTagName("description").item(0).getTextContent();
			Placemark a = new Placemark();
			a.lat = lat;
			a.lon = lon;
			a.description = description;
			retval[i] = a;
		}
		return retval;
	}
}