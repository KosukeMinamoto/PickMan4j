package com.minamo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

import edu.mines.jtk.dsp.BandPassFilter;

import edu.sc.seis.seisFile.sac.SacHeader;
import edu.sc.seis.seisFile.sac.SacTimeSeries;

public class WaveProcessor {
	public static SacTimeSeries read(String sacFilePath) throws Exception {
		return SacTimeSeries.read(sacFilePath);
	}

	public static LocalDateTime getStarttime(SacTimeSeries sac) {
		SacHeader hdr = sac.getHeader();
		LocalDateTime time = LocalDateTime.of(
				hdr.getNzyear(),
				1,
				1,
				hdr.getNzhour(),
				hdr.getNzmin(),
				hdr.getNzsec(),
				hdr.getNzmsec());
		time = time.plusDays(hdr.getNzjday() - 1);
		return time;
	}

	public static TimeSeries getTimeSeries(SacTimeSeries sac) {
		SacHeader hdr = sac.getHeader();
		float[] data = sac.getY();
		int npts = hdr.getNpts();
		float delta = hdr.getDelta();

		TimeSeries timeSeries = new TimeSeries("time");
		ZoneId zoneId = ZoneId.systemDefault();
		LocalDateTime startTime = getStarttime(sac);
		for (int i = 0; i < npts; i++) {
			LocalDateTime nowDT = startTime.plusNanos((long) (i*1e9*delta));
			Date nowDate = Date.from(nowDT.atZone(zoneId).toInstant());
			timeSeries.add(new Millisecond(nowDate), data[i]);
		}
		return timeSeries;
	}

	public static SacTimeSeries detrend(SacTimeSeries sac) {
		try {
			SacHeader hdr = sac.getHeader();
			int npts = hdr.getNpts();
			float[] data = sac.getY();
			double[] trend = new double[npts];
			float[] detrended = new float[npts];

			List<WeightedObservedPoint> points = new ArrayList<>();
			for (int i = 0; i < npts; i++) {
				points.add(new WeightedObservedPoint(1.0, i, data[i]));
			}

			PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
			double[] coefficients = fitter.fit(points);
			
			for (int i = 0; i < npts; i++) {
				trend[i] = coefficients[0] + coefficients[1] * i;
				detrended[i] = (float) (data[i] - trend[i]);
			}
			sac.setY(detrended);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sac;
	}

	public static SacTimeSeries bandPassFilter(SacTimeSeries sac, float freqmin, float freqmax) {
		try {
			SacHeader hdr = sac.getHeader();
			int npts = hdr.getNpts();

			float[] dataIni = sac.getY();
			float[] dataOut = new float[npts];

			float delta = hdr.getDelta();
			float nyquist = 0.5f / delta;
			float klower = freqmin / nyquist;
			float kupper = freqmax / nyquist;
			
			float kwidth = Math.min(klower, 1 - kupper) / 2;
			float aerror = 0.001f;
			BandPassFilter bpf = new BandPassFilter(klower, kupper, kwidth, aerror);
			bpf.apply(dataIni, dataOut);
			sac.setY(dataOut);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sac;
	}
}
