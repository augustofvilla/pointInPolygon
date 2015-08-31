package reverse.geocoding;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.IOUtils;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class PointInPolygon {

	private FilterFactory2 filterFactory;
	private SimpleFeatureCollection features;
	private ReferencedEnvelope env;

	public static void main(String[] args) {
		Writer fileWriter = null;
		try {

			if (args.length != 5) {
				System.out.println("Incorrect number of parameters. Usage: java PointInPolygon shapeFile coordinatesFile outputFile longitudeHeaderName latitudeHeaderName");
			}
			
			String shapeFile = args[0];
			String coorinatesFile = args[1];
			String outPutFile = args[2];
			String longitudeHeader = args[3];
			String latitudeHeader = args[4];
			
			File file = new File(shapeFile);

			fileWriter = new FileWriter(outPutFile);
			
			PointInPolygon tester = new PointInPolygon();
			FileDataStore store = FileDataStoreFinder.getDataStore(file);
			SimpleFeatureSource featureSource = store.getFeatureSource();
			tester.setFeatures(featureSource.getFeatures());

			CoordinateReferenceSystem targetCRS = featureSource.getFeatures().getBounds().getCoordinateReferenceSystem();
			CoordinateReferenceSystem sourceCRS = DefaultGeographicCRS.WGS84;
			MathTransform transform = CRS.findMathTransform(sourceCRS,targetCRS, true);
			GeometryFactory fac = new GeometryFactory();

			File csvData = new File(coorinatesFile);
			CSVParser parser = CSVParser.parse(csvData,Charset.defaultCharset(), CSVFormat.DEFAULT.withHeader());

			for (org.apache.commons.csv.CSVRecord csvRecord : parser) {
				String lonString = csvRecord.get(longitudeHeader);
				String latString = csvRecord.get(latitudeHeader);

				Double lon = Double.valueOf(lonString);
				Double lat = Double.valueOf(latString);

				Point p = fac.createPoint(new Coordinate(lon, lat));
				Point transformedPoint = (Point) JTS.transform(p, transform);

				String barrio = tester.isInShape(transformedPoint);
				if (barrio != null) {
					fileWriter.write(barrio+System.lineSeparator());
				} else {
					fileWriter.write("NULL"+System.lineSeparator());
				}

			}

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			IOUtils.closeQuietly(fileWriter);
		}

	}

	public PointInPolygon() {

		filterFactory = CommonFactoryFinder.getFilterFactory2(GeoTools
				.getDefaultHints());
	}

	private String isInShape(Point p) {
		if (!env.contains(p.getCoordinate())) {
			return null;
		}
		Expression propertyName = filterFactory.property(features.getSchema().getGeometryDescriptor().getName());
		Filter filter = filterFactory.contains(propertyName,filterFactory.literal(p));
		SimpleFeatureCollection sub = features.subCollection(filter);

		String barrio = null;
		
		SimpleFeatureIterator it = sub.features();
		if (it.hasNext()) {
			SimpleFeature sf = it.next();
			barrio = (String) sf.getAttribute("BARRIO");
		} 
		it.close();
		return barrio;
	}

	private void setFeatures(SimpleFeatureCollection features) {
		this.features = features;
		env = features.getBounds();
	}

}
