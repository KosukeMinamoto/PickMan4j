package com.minamo;

import java.io.File;
import java.util.Arrays;

import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JFileChooser;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeries;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.entity.ChartEntity;

import edu.sc.seis.seisFile.sac.SacTimeSeries;

public class MainFrame {
	private static JLabel mousePositionLabel;
	private static File selectedDir;
	private static ArrivalTimeManager arrivalTimeManager = new ArrivalTimeManager();
	private static JTextField lowFreqField;
	private static JTextField highFreqField;
	private static float lowFreq = 1.0f;
	private static float highFreq = 16.0f;

	private static JFrame zoomFrame;
	private static JFreeChart zoomChart;
	private static ChartPanel zoomChartPanel;

	public static void main(String[] args) {
		JFrame frame = createMainFrame();
		JPanel chartPanel = createChartPanel(frame);
		JPanel filePanel = createFilePanel(frame, chartPanel);
		createZoomWindow();
		chartPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		frame.add(filePanel, BorderLayout.EAST);
		SacTimeSeries[] dummyWaveforms = new SacTimeSeries[0];
		updateCharts(chartPanel, dummyWaveforms, frame);

		JPanel inputPanel = new JPanel();
		lowFreqField = new JTextField("1", 5);
		highFreqField = new JTextField("45", 5);
		inputPanel.add(new JLabel("Freq Min:"));
		inputPanel.add(lowFreqField);
		inputPanel.add(new JLabel("Freq Max:"));
		inputPanel.add(highFreqField);

		JButton updateButton = new JButton("UPDATE");
		updateButton.addActionListener(e -> {
			try {
				lowFreq = Float.parseFloat(lowFreqField.getText());
				highFreq = Float.parseFloat(highFreqField.getText());
				System.out.println("BP freq: " + lowFreq + " - " + highFreq + " Hz");

				if (selectedDir != null) {
					Component viewComponent = ((JScrollPane) filePanel.getComponent(1)).getViewport().getView();
					if (viewComponent instanceof JList<?>) {
						@SuppressWarnings("unchecked")
						JList<String> fileList = (JList<String>) viewComponent;
						int[] selectedIndices = fileList.getSelectedIndices();
						if (selectedIndices.length == 0) {
							System.err.println("Please select at least one file.");
							return;
						}

						File[] filesInDir = selectedDir.listFiles((dir, name) -> name.endsWith(".sac") || name.endsWith(".SAC"));
						Arrays.sort(filesInDir, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
						SacTimeSeries[] waveforms = new SacTimeSeries[selectedIndices.length];
						for (int i = 0; i < selectedIndices.length; i++) {
							waveforms[i] = WaveProcessor.read(filesInDir[selectedIndices[i]].getAbsolutePath());
							waveforms[i] = WaveProcessor.bandPassFilter(waveforms[i], lowFreq, highFreq);
						}
						updateCharts(chartPanel, waveforms, frame);
					}
				}
			} catch (NumberFormatException ex) {
				System.err.println("Invalid frequency input.");
			} catch (Exception ex) {
				lowFreq = 1.0f;
				highFreq = 45.0f;
				lowFreqField.setText(String.valueOf(lowFreq));
				highFreqField.setText(String.valueOf(highFreq));
				ex.printStackTrace();
			}
		});
		inputPanel.add(updateButton);

		JButton saveButton = new JButton("SAVE");
		saveButton.addActionListener(e -> writeSeismicDataToFile());

		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(inputPanel, BorderLayout.CENTER);
		southPanel.add(saveButton, BorderLayout.SOUTH);

		filePanel.add(southPanel, BorderLayout.SOUTH);

		mousePositionLabel = new JLabel("Mouse Position: (0, 0)");
		frame.add(mousePositionLabel, BorderLayout.NORTH);
		frame.setVisible(true);
	}

	private static JFrame createMainFrame() {
		JFrame frame = new JFrame("PickMan Monitor");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1200, 600);
		frame.setLayout(new BorderLayout());
		return frame;
	}

	private static JPanel createChartPanel(JFrame frame) {
		JPanel chartPanel = new JPanel(new GridLayout(1, 1));
		frame.add(chartPanel, BorderLayout.CENTER);
		return chartPanel;
	}

	private static JPanel createFilePanel(JFrame frame, JPanel chartPanel) {
		JPanel filePanel = new JPanel(new BorderLayout());
		JButton selectDirButton = new JButton("Select Directory");
		filePanel.add(selectDirButton, BorderLayout.NORTH);

		JList<String> fileList = new JList<>();
		fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JScrollPane scrollPane = new JScrollPane(fileList);
		filePanel.add(scrollPane, BorderLayout.CENTER);

		selectDirButton.addActionListener(e -> selectDirectory(fileList, chartPanel, frame));
		return filePanel;
	}

	private static void createZoomWindow() {
		zoomFrame = new JFrame("Zoom");
		zoomFrame.setSize(400, 400);
		zoomChart = ChartFactory.createTimeSeriesChart(
				null,
				null,
				null,
				new TimeSeriesCollection(),
				false,
				false,
				false);
		zoomChartPanel = new ChartPanel(zoomChart);
		zoomFrame.add(zoomChartPanel, BorderLayout.CENTER);

		zoomFrame.setVisible(true);
	}

	private static void selectDirectory(JList<String> fileList, JPanel chartPanel, JFrame frame) {
		try {
			JFileChooser dirChooser = new JFileChooser();
			dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			if (selectedDir != null) {
				File parentDir = selectedDir.getParentFile();
				if (parentDir != null) {
					dirChooser.setCurrentDirectory(parentDir);
				}
			}

			int returnValue = dirChooser.showOpenDialog(null);
			if (returnValue != JFileChooser.APPROVE_OPTION) {
				System.out.println("Directory was not selected.");
				return;
			}

			selectedDir = dirChooser.getSelectedFile();
			File[] filesInDir = selectedDir.listFiles((dir, name) -> name.endsWith(".sac") || name.endsWith(".SAC"));

			arrivalTimeManager.clearArrivalTimes();

			if (filesInDir == null || filesInDir.length == 0) {
				System.out.println("No SAC file was found in the " + selectedDir.getAbsolutePath());
				return;
			}
			updateFileList(fileList, filesInDir);
			addFileSelectionListener(fileList, filesInDir, chartPanel, frame);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void updateFileList(JList<String> fileList, File[] filesInDir) {
		Arrays.sort(filesInDir, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

		String[] fileNames = new String[filesInDir.length];
		for (int i = 0; i < filesInDir.length; i++) {
			fileNames[i] = filesInDir[i].getName();
		}
		fileList.setListData(fileNames);
	}

	private static void addFileSelectionListener(
			JList<String> fileList, File[] filesInDir, JPanel chartPanel, JFrame frame) {

		for (ListSelectionListener listener : fileList.getListSelectionListeners()) {
			fileList.removeListSelectionListener(listener);
		}

		fileList.addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting()) {
				try {
					int[] selectedIndices = fileList.getSelectedIndices();
					if (selectedIndices.length == 0) {
						System.err.println("Please select at least one file.");
						return;
					}
					SacTimeSeries[] waveforms = new SacTimeSeries[selectedIndices.length];
					for (int i = 0; i < selectedIndices.length; i++) {
						waveforms[i] = WaveProcessor.read(filesInDir[selectedIndices[i]].getAbsolutePath());
						waveforms[i] = WaveProcessor.bandPassFilter(waveforms[i], lowFreq, highFreq);
					}
					updateCharts(chartPanel, waveforms, frame);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
	}

	private static void updateCharts(JPanel chartPanel, SacTimeSeries[] waveforms, JFrame frame) {
		chartPanel.removeAll();
		chartPanel.setLayout(new GridLayout(1, 1));

		TimeSeriesCollection dataset = new TimeSeriesCollection();
		double offset = 0.0;
		double offsetIncrement = 1.0;

		java.awt.Color[] seriesColor = new java.awt.Color[waveforms.length];
		String[] fileNames = new String[waveforms.length];
		for (int i = 0; i < waveforms.length; i++) {
			TimeSeries series = WaveProcessor.getTimeSeries(waveforms[i]);
			TimeSeries normalizedSeries = normalizeTimeSeries(series);

			TimeSeries offsetSeries = new TimeSeries("offset_" + i);
			for (int j = 0; j < normalizedSeries.getItemCount(); j++) {
				double valueWithOffset = normalizedSeries.getValue(j).doubleValue() + offset;
				offsetSeries.add(normalizedSeries.getTimePeriod(j), valueWithOffset);
			}
			dataset.addSeries(offsetSeries);

			String station = waveforms[i].getHeader().getKstnm();
			String component = waveforms[i].getHeader().getKcmpnm();
			fileNames[i] = (station + "__" + component).replace("^@", "").replace(" ", "");
			offset += offsetIncrement;

			if (fileNames[i].contains("Z") || fileNames[i].contains("U") || fileNames[i].contains("V")) {
				seriesColor[i] = java.awt.Color.BLUE;
			} else if (fileNames[i].contains("X") || fileNames[i].contains("Y") || fileNames[i].contains("H")) {
				seriesColor[i] = java.awt.Color.BLACK;
			} else {
				seriesColor[i] = java.awt.Color.RED;
			}
		}

		JFreeChart chart = ChartFactory.createTimeSeriesChart(
				null,
				"Time",
				"Amplitude",
				dataset,
				false,
				false,
				false);

		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setDomainPannable(true);
		plot.setRangePannable(true);
		plot.clearAnnotations();
		for (int i = 0; i < waveforms.length; i++) {
			plot.getRenderer().setSeriesPaint(i, seriesColor[i]);
			updateAnnotations(plot, waveforms[i].getHeader().getKstnm(), i);
		}

		org.jfree.chart.axis.SymbolAxis yAxis = new org.jfree.chart.axis.SymbolAxis(null, fileNames);
		yAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(1));
		yAxis.setTickLabelsVisible(true);
		yAxis.setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
		yAxis.setTickLabelPaint(java.awt.Color.BLACK);
		yAxis.setVerticalTickLabels(true);
		plot.setRangeAxis(yAxis);

		ChartPanel chartPanelComponent = new ChartPanel(chart);
		chartPanelComponent.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chartPanel.add(chartPanelComponent);

		chartPanelComponent.addChartMouseListener(new ChartMouseListener() {
			@Override
			public void chartMouseClicked(ChartMouseEvent event) {
				ChartEntity entity = event.getEntity();
				if (entity instanceof XYItemEntity) {
					XYItemEntity itemEntity = (XYItemEntity) entity;
					int seriesIndex = itemEntity.getSeriesIndex();
					int itemIndex = itemEntity.getItem();
					TimeSeries series = dataset.getSeries(seriesIndex);
					RegularTimePeriod xTime = series.getTimePeriod(itemIndex);

					String stationName = fileNames[seriesIndex].split("__")[0];
					String component = fileNames[seriesIndex].split("__")[1];
					String phase = event.getTrigger().getButton() == java.awt.event.MouseEvent.BUTTON1 ? "P" : "S";

					java.util.Date xDate = new java.util.Date(xTime.getFirstMillisecond());

					if ((event.getTrigger().getModifiersEx() & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0) {
						arrivalTimeManager.removeArrivalTime(stationName, component, phase);
					} else {
						arrivalTimeManager.updateArrivalTime(stationName, component, phase, xDate);
					}
				}
				plot.clearAnnotations();
				for (int i = 0; i < waveforms.length; i++) {
					String stationName = fileNames[i].split("__")[0];
					updateAnnotations(plot, stationName, i);
				}
			}

			@Override
			public void chartMouseMoved(ChartMouseEvent event) {
				int x = event.getTrigger().getX();
				int y = event.getTrigger().getY();
				mousePositionLabel.setText("Mouse Position: (" + x + ", " + y + ")");
				updateZoomChart(event, dataset);
			}
		});
		frame.revalidate();
		frame.repaint();
	}

	private static void updateAnnotations(XYPlot plot, String station, int index) {
		double yMin = index - 0.5;
		double yMax = index + 0.5;
		station = station.replace(" ", "");
		if (arrivalTimeManager.containsArrivalTime(station + "_P")) {
			ArrivalTime arrivalTime = arrivalTimeManager.getArrivalTimeMap().get(station + "_P");
			double xValue = arrivalTime.getArrivalTime().getTime();
			java.awt.Color annotationColor = java.awt.Color.MAGENTA;
			plot.addAnnotation(new XYLineAnnotation(
					xValue,
					yMin,
					xValue,
					yMax,
					new BasicStroke(2.0f),
					annotationColor),
					true);
		}
		if (arrivalTimeManager.containsArrivalTime(station + "_S")) {
			ArrivalTime arrivalTime = arrivalTimeManager.getArrivalTimeMap().get(station + "_S");
			double xValue = arrivalTime.getArrivalTime().getTime();
			java.awt.Color annotationColor = java.awt.Color.CYAN;

			plot.addAnnotation(new XYLineAnnotation(
					xValue,
					yMin,
					xValue,
					yMax,
					new BasicStroke(2.0f),
					annotationColor),
					true);
		}
	}

	private static void updateZoomChart(ChartMouseEvent event, TimeSeriesCollection dataset) {
		ChartEntity entity = event.getEntity();
		if (entity instanceof XYItemEntity) {
			XYItemEntity itemEntity = (XYItemEntity) entity;
			int seriesIndex = itemEntity.getSeriesIndex();
			int itemIndex = itemEntity.getItem();
			TimeSeries series = dataset.getSeries(seriesIndex);

			int startIndex = Math.max(0, itemIndex - 1000);
			int endIndex = Math.min(series.getItemCount() - 1, itemIndex + 1000);

			org.jfree.data.xy.XYSeries zoomSeries = new org.jfree.data.xy.XYSeries("Zoom");
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
			for (int i = startIndex; i <= endIndex; i++) {
				double value = series.getValue(i).doubleValue();
				min = Math.min(min, value);
				max = Math.max(max, value);
			}
			double range = max - min;
			for (int i = startIndex; i <= endIndex; i++) {
				double normalizedValue = (series.getValue(i).doubleValue() - min) / range * 2 - 1;
				zoomSeries.add(i, normalizedValue);
			}

			org.jfree.data.xy.XYSeriesCollection zoomDataset = new org.jfree.data.xy.XYSeriesCollection(zoomSeries);
			JFreeChart zoomChart = ChartFactory.createXYLineChart(
					null,
					null,
					null,
					zoomDataset,
					org.jfree.chart.plot.PlotOrientation.VERTICAL,
					false,
					false,
					false);

			int centerIndex = (startIndex + endIndex) / 2;
			XYPlot zoomPlot = (XYPlot) zoomChart.getPlot();
			zoomPlot.setDataset(zoomDataset);
			zoomPlot.clearAnnotations();
			zoomPlot.addAnnotation(new XYLineAnnotation(
					centerIndex,
					zoomPlot.getRangeAxis().getLowerBound(),
					centerIndex,
					zoomPlot.getRangeAxis().getUpperBound(),
					new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f}, 0.0f),
					java.awt.Color.BLACK));

			zoomChartPanel.setChart(zoomChart);
			zoomFrame.revalidate();
			zoomFrame.repaint();
		}
	}

	private static void writeSeismicDataToFile() {
		if (selectedDir == null) {
			System.err.println("No directory selected.");
			return;
		}

		String fileName = selectedDir.getName() + ".obs";
		arrivalTimeManager.outputToObs(fileName);
		// arrivalTimeManager.printAllArrivalTimes();
	}

	private static TimeSeries normalizeTimeSeries(TimeSeries series) {
		TimeSeries normalizedSeries = new TimeSeries("normalized");
		double maxVal = 0;
		for (int i = 0; i < series.getItemCount(); i++) {
			double val = series.getValue(i).doubleValue();
			if (val > maxVal) {
				maxVal = val;
			}
		}
		for (int i = 0; i < series.getItemCount(); i++) {
			normalizedSeries.add(series.getTimePeriod(i), series.getValue(i).doubleValue() / (2 * maxVal));
		}
		return normalizedSeries;
	}
}
