package com.minamo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class ArrivalTimeManager {
	private HashMap<String, ArrivalTime> arrivalTimeMap;

	public ArrivalTimeManager() {
		arrivalTimeMap = new HashMap<>();
	}

	private String createKey(String stationName, String phase) {
		return stationName + "_" + phase;
	}

	public void updateArrivalTime(String stationName, String component, String phase, Date arrivalTime) {
		String key = createKey(stationName, phase);
		if (arrivalTimeMap.containsKey(key)) {
			System.out.println("Updating arrival time for " + stationName + " (" + phase + ")");
			arrivalTimeMap.get(key).setArrivalTime(arrivalTime);
		} else {
			System.out.println("Adding new arrival time for " + stationName + " (" + phase + ")");
			arrivalTimeMap.put(key, new ArrivalTime(stationName, component, phase, arrivalTime));
		}
	}

	public ArrivalTime getArrivalTime(String stationName, String phase) {
		String key = createKey(stationName, phase);
		return arrivalTimeMap.get(key);
	}

	public HashMap<String, ArrivalTime> getArrivalTimeMap() {
		return arrivalTimeMap;
	}

	public void setArrivalTime(String stationName, String component, String phase, Date arrivalTime) {
		String key = createKey(stationName, phase);
		arrivalTimeMap.put(key, new ArrivalTime(stationName, component, phase, arrivalTime));
	}

	public void printAllArrivalTimes() {
		for (ArrivalTime arrival : arrivalTimeMap.values()) {
			System.out.println(arrival);
		}
	}

	public void outputToObs(String fileName) {
		File outputFile = new File(fileName);
		try (PrintWriter writer = new PrintWriter(
				new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
			for (ArrivalTime arrivalTime : arrivalTimeMap.values()) {
				Date date = arrivalTime.getArrivalTime();
				writer.println(String.format("%-6s %-4s %-4s %-1s %-6s %-1s %s %s %9.4f %-3s %9.2e %9.2e %9.2e %9.2e",
					arrivalTime.getStationName(),
					arrivalTime.getInstrument(),
					arrivalTime.getComponent(),
					arrivalTime.getPPhaseOnset(),
					arrivalTime.getPhaseDescriptor(),
					arrivalTime.getFirstMotion(),
					new SimpleDateFormat("yyyyMMdd").format(date),
					new SimpleDateFormat("HHmm").format(date),
					Float.parseFloat(new SimpleDateFormat("ss.SSSS").format(date)),
					arrivalTime.getErr(),
					arrivalTime.getErrMag(),
					arrivalTime.getCodaDuration(),
					arrivalTime.getAmplitude(),
					arrivalTime.getPeriod()));
			}
			System.out.println("Pick data written to " + outputFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void clearArrivalTimes() {
		arrivalTimeMap.clear();
	}
}

class ArrivalTime {
	private String stationName;
	private String instrument;
	private String component;
	private String pPhaseOnset;
	private String phaseDescriptor;
	private String firstMotion;
	private Date arrivalTime;
	private String err;
	private float errMag;
	private float codaDuration;
	private float amplitude;
	private float period;
	private float priorWt;

	public ArrivalTime(String stationName, String component, String phase, Date arrivalTime) {
		this.stationName = stationName;
		this.component = component;
		this.phaseDescriptor = phase;
		this.arrivalTime = arrivalTime;
		this.instrument = "?";
		this.pPhaseOnset = "?";
		this.firstMotion = "?";
		this.err = "GAU";
		this.errMag = -1.0f;
		this.codaDuration = -1.0f;
		this.amplitude = -1.0f;
		this.period = -1.0f;
		this.priorWt = -1.0f;
	}

	public String getInstrument() {
		return instrument;
	}

	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public String getPPhaseOnset() {
		return pPhaseOnset;
	}

	public void setPPhaseOnset(String pPhaseOnset) {
		this.pPhaseOnset = pPhaseOnset;
	}

	public String getPhaseDescriptor() {
		return phaseDescriptor;
	}

	public void setPhaseDescriptor(String phaseDescriptor) {
		this.phaseDescriptor = phaseDescriptor;
	}

	public String getFirstMotion() {
		return firstMotion;
	}

	public void setFirstMotion(String firstMotion) {
		this.firstMotion = firstMotion;
	}

	public String getErr() {
		return err;
	}

	public void setErr(String err) {
		this.err = err;
	}

	public float getErrMag() {
		return errMag;
	}

	public void setErrMag(float errMag) {
		this.errMag = errMag;
	}

	public float getCodaDuration() {
		return codaDuration;
	}

	public void setCodaDuration(float codaDuration) {
		this.codaDuration = codaDuration;
	}

	public float getAmplitude() {
		return amplitude;
	}

	public void setAmplitude(float amplitude) {
		this.amplitude = amplitude;
	}

	public float getPeriod() {
		return period;
	}

	public void setPeriod(float period) {
		this.period = period;
	}

	public float getPriorWt() {
		return priorWt;
	}

	public void setPriorWt(float priorWt) {
		this.priorWt = priorWt;
	}

	public String getStationName() {
		return stationName;
	}

	public Date getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(Date arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	@Override
	public String toString() {
		return "Station: " + stationName + ", Phase: " + phaseDescriptor + ", Arrival Time: " + arrivalTime;
	}
}
