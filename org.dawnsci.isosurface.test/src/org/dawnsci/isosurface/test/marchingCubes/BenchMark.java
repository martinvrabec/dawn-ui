package org.dawnsci.isosurface.test.marchingCubes;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.dawnsci.isosurface.alg.MarchingCubes;
import org.dawnsci.isosurface.alg.MarchingCubesModel;
import org.dawnsci.isosurface.alg.Surface;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dawnsci.analysis.api.dataset.ILazyDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetFactory;
import org.eclipse.dawnsci.analysis.dataset.impl.IntegerDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.Random;
import org.junit.Ignore;
import org.junit.Test;

public class BenchMark 
{
	
	// benchmarking
	int[] dataSize = { 250, 250, 250 };
	int[] boxSize = { 4, 4, 4 };

	// general
	MarchingCubes algorithm;
	ILazyDataset lz;
	MarchingCubesModel model;
	Surface testResult;
	IProgressMonitor monitor;

	private void start(int[] dataSetSizeXYZ, int[] boxSizeXYZ) {
		

		Random.seed(123456789);

		lz = Random.lazyRand(dataSetSizeXYZ);
		IntegerDataset axis = DatasetFactory.createRange(IntegerDataset.class, dataSetSizeXYZ[0]);
		List<IntegerDataset> axes = Arrays.asList(axis, axis, axis);
		

		model = new MarchingCubesModel(lz,axes,0.5,boxSizeXYZ,new int[]{1,1,1}, 1,"traceID");
		algorithm = new MarchingCubes(model);	
		
		monitor = new IProgressMonitor() {

			@Override
			public void beginTask(String name, int totalWork) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void done() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void internalWorked(double work) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public boolean isCanceled() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void setCanceled(boolean value) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void setTaskName(String name) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void subTask(String name) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void worked(int work) {
				// TODO Auto-generated method stub
				
			}
			
			
		};
		
		// execute the algorithmA
		testResult = algorithm.execute(monitor);
	}

	@Ignore
	@Test
	public void RandomNoiseBenchMarkTest() throws Exception {
		int BENCHMARK_TEST_COUNT = 100;

		start(dataSize, boxSize);

		System.out.println("++++++ STARTING BENCH MARK TEST ++++++");
		System.out.println("RUNNING " + BENCHMARK_TEST_COUNT + " TESTS");
		System.out.println("BOX SIZE - " + Arrays.toString(boxSize));
		System.out.println("DATA SIZE - " + Arrays.toString(dataSize));

		double average = 0;
		for (int i = 0; i < BENCHMARK_TEST_COUNT; i++) {
			java.util.Random rnd = new java.util.Random();

			Random.seed(rnd.nextInt());

			if (i % 10 == 0) {
				System.out.print("\n");
			}
			double startTime = System.currentTimeMillis();
			testResult = algorithm.execute(monitor);
			double completionTime = System.currentTimeMillis() - startTime;
			average += completionTime;
			System.out.print(completionTime + ",\t");

		}

		average /= BENCHMARK_TEST_COUNT;

		System.out.println("\n\n++++++ ENDING BENCH MARK TEST ++++++");

		System.out.println("AVERAGE COMPLETION TIME - " + average);

		System.out.println("++++++++++++++++++++++++++++++++++++");

		InetAddress addr;

		addr = InetAddress.getLocalHost();
		java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();

		String resultPrint = Double.toString(average);
		resultPrint += "\t" + Arrays.toString(dataSize) + "\t"
				+ Arrays.toString(boxSize);
		resultPrint += "\t" + localMachine.getHostName();

		DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		Date dateobj = new Date();

		resultPrint += "\t" + df.format(dateobj);

		Files.write(Paths.get("Results/random_noise_bench_mark_results.txt"),
				(resultPrint + "\n").getBytes(), StandardOpenOption.APPEND);

		System.out.println(resultPrint);

	}

	@Ignore
	@Test
	public void varrying_BoxSize_RandomNoiseBenchMarkTest() throws Exception {
		this.dataSize = new int[] { 100, 100, 100 };
		for (int i = 20; i > 0; i--) {
			this.boxSize = new int[] { i, i, i };
			RandomNoiseBenchMarkTest();
		}
	}

	@Ignore
	@Test
	public void varrying_DataSize_RandomNoiseBenchMarkTest() throws Exception {
		this.boxSize = new int[] { 4, 4, 4 };
		for (int i = 50; i < 300; i += 25) {
			this.dataSize = new int[] { i, i, i };
			RandomNoiseBenchMarkTest();
		}
	}
}
