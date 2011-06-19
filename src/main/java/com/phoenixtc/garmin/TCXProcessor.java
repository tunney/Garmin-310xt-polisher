package com.phoenixtc.garmin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.xerces.dom.ElementNSImpl;
import org.w3c.dom.Node;

import com.garmin.xmlschemas.trainingcenterdatabase.v2.ActivityLapT;
import com.garmin.xmlschemas.trainingcenterdatabase.v2.ActivityListT;
import com.garmin.xmlschemas.trainingcenterdatabase.v2.ActivityT;
import com.garmin.xmlschemas.trainingcenterdatabase.v2.ExtensionsT;
import com.garmin.xmlschemas.trainingcenterdatabase.v2.HeartRateInBeatsPerMinuteT;
import com.garmin.xmlschemas.trainingcenterdatabase.v2.TrackT;
import com.garmin.xmlschemas.trainingcenterdatabase.v2.TrackpointT;
import com.garmin.xmlschemas.trainingcenterdatabase.v2.TrainingCenterDatabaseT;

public class TCXProcessor {

	private static final String NEWLINE = "\r\n";
	private long offset = 0;
	private DatatypeFactory datatypeFactory;

	private String inputFileName;
	private String outputFileName;

	public TCXProcessor(String inputFileName, String outputFileName)
			throws DatatypeConfigurationException {
		datatypeFactory = DatatypeFactory.newInstance();

		this.inputFileName = inputFileName;
		this.outputFileName = outputFileName;
	}

	/**
	 * Process the TCX file and persist the changed version
	 * 
	 * @throws JAXBException Unable to marshal the TCX file
	 * @throws IOException Unable to save the modified TCX file
	 */
	public void process() throws JAXBException, IOException {
		JAXBContext jc = JAXBContext
				.newInstance("com.garmin.xmlschemas.trainingcenterdatabase.v2");

		Unmarshaller unmarshaller = jc.createUnmarshaller();

		JAXBElement<TrainingCenterDatabaseT> o = (JAXBElement<TrainingCenterDatabaseT>) unmarshaller
				.unmarshal(new File(inputFileName));

		Marshaller marshaller = jc.createMarshaller();

		TrainingCenterDatabaseT t = o.getValue();

		ActivityListT acts = t.getActivities();

		List<ActivityT> actList = acts.getActivity();

		for (Iterator<ActivityT> it = actList.iterator(); it.hasNext();) {
			ActivityT act = it.next();
			List<ActivityLapT> laps = act.getLap();

			int lapCount = 0;
			for (Iterator<ActivityLapT> lap_it = laps.iterator(); lap_it
					.hasNext();) {
				ActivityLapT lap = lap_it.next();
				processLap(laps, lapCount, lap);
			}
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		marshaller.marshal(o, baos);

		writeOutputToFile(baos.toByteArray());

	}

	private void processLap(List<ActivityLapT> laps, int lapCount,
			ActivityLapT lap) {

		long startTime = lap.getStartTime().toGregorianCalendar()
				.getTimeInMillis();
		startTime -= offset;
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTimeInMillis(startTime);
		XMLGregorianCalendar x = datatypeFactory
				.newXMLGregorianCalendar(gc);
		lap.setStartTime(x);

		List<TrackT> tracks = lap.getTrack();

		XMLGregorianCalendar lapEnd = null;

		XMLGregorianCalendar end = null;

		for (Iterator<TrackT> track_it = tracks.iterator(); track_it
				.hasNext();) {
			TrackT track = track_it.next();
			lapEnd = processTrack(lapEnd, end, track);

		}

		if (laps.size() - 1 > lapCount) {
			ActivityLapT nextLap = laps.get(lapCount + 1);

			long nextLapStartTime = nextLap.getStartTime()
					.toGregorianCalendar().getTimeInMillis()
					- offset;
			double lapTime = (nextLapStartTime - lap.getStartTime()
					.toGregorianCalendar().getTimeInMillis()) / 1000;

			lap.setTotalTimeSeconds(lapTime);
		} else {
			double lapTime = (lapEnd.toGregorianCalendar()
					.getTimeInMillis() - lap.getStartTime()
					.toGregorianCalendar().getTimeInMillis()) / 1000;

			lap.setTotalTimeSeconds(lapTime);
		}
		lapCount++;
	}

	private XMLGregorianCalendar processTrack(XMLGregorianCalendar lapEnd,
			XMLGregorianCalendar end, TrackT track) {
		List<TrackpointT> points = track.getTrackpoint();

		HeartRateInBeatsPerMinuteT bpm = null;
		ExtensionsT lastExt = null;

		for (Iterator<TrackpointT> p_it = points.iterator(); p_it
				.hasNext();) {
			TrackpointT point = p_it.next();

			lapEnd = processTrackPoint(end, bpm, lastExt, point);

		}
		end = points.get(points.size() - 1).getTime();
		return lapEnd;
	}

	private XMLGregorianCalendar processTrackPoint(XMLGregorianCalendar end,
			HeartRateInBeatsPerMinuteT bpm, ExtensionsT lastExt,
			TrackpointT point) {
		XMLGregorianCalendar lapEnd;
		XMLGregorianCalendar start;
		if (bpm == null) {
			if (point.getHeartRateBpm() != null) {
				bpm = point.getHeartRateBpm();
			}
		} else {
			if (point.getHeartRateBpm() == null) {
				if (bpm != null) {
					point.setHeartRateBpm(bpm);
				}
			}
		}
		if (point.getHeartRateBpm() != null) {
			bpm = point.getHeartRateBpm();
		}
		if (end != null) {
			start = point.getTime();
			long start_l = start.toGregorianCalendar()
					.getTimeInMillis();
			long end_l = end.toGregorianCalendar()
					.getTimeInMillis();

			offset = start_l - end_l;
			end = null;
		}

		ExtensionsT ex = point.getExtensions();

		boolean cadFound = false;

		if (ex != null) {
			cadFound = findCadenceExtension(ex.getAny());
			lastExt = ex;
		}

		if (!cadFound) {
			if (ex != null) {
				ex.getAny().add(lastExt);
			} else {
				point.setExtensions(lastExt);
			}

		}

		long init = point.getTime().toGregorianCalendar()
				.getTimeInMillis();
		GregorianCalendar gc1 = new GregorianCalendar();
		gc1.setTimeInMillis(init - offset);
		XMLGregorianCalendar x1 = datatypeFactory
				.newXMLGregorianCalendar(gc1);
		point.setTime(x1);

		lapEnd = point.getTime();
		return lapEnd;
	}

	/**
	 * There may or may not be a cadence sensor in use. Determine if one was in
	 * use
	 * 
	 * @param list
	 *            The list of extensions to query
	 * @return True if a cadence sensor was in use, false otherwise
	 */
	private boolean findCadenceExtension(List<Object> list) {
		boolean cadFound = false;

		if (list != null) {
			for (int j = 0; j < list.size(); j++) {
				Object ob = list.get(j);
				ElementNSImpl element = (ElementNSImpl) ob;

				Node n = element.getAttributes().getNamedItem("CadenceSensor");

				if (n != null) {
					cadFound = true;
				}
			}
		}
		return cadFound;
	}

	/**
	 * Persist the modified TCX file to the specified file
	 * 
	 * @param bytes
	 *            The contents of the TCX file to write
	 * @throws IOException
	 */
	private void writeOutputToFile(byte[] bytes) throws IOException {

		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		InputStreamReader isr = new InputStreamReader(bais);
		LineNumberReader lnr = new LineNumberReader(isr);

		FileWriter fw = new FileWriter(new File(outputFileName));

		String line = lnr.readLine();
		while (line != null) {
			line = line.replaceAll("TPX:TPX", "TPX");

			fw.write(line + NEWLINE);
			line = lnr.readLine();
		}

		fw.close();
	}
}
