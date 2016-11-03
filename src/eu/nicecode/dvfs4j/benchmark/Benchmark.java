package eu.nicecode.dvfs4j.benchmark;

import java.util.Random;

import eu.nicecode.dvfs4j.DVFS;
import eu.nicecode.dvfs4j.DVFSFactory;

public class Benchmark {

	public static void main(String[] args) throws Exception {

		int transitionpercore = 1000000;
		
		DVFS dvfs = DVFSFactory.getDVFS();
		Random r = new Random();
		
		int numcores = dvfs.getNumOfCores();
		int[] availFreqs = dvfs.getAvailableFrequencies();
		
		long start = System.nanoTime();
		for (int i = 0; i < transitionpercore; i++) {
			
			for (int coreid = 0; coreid < numcores; coreid++) {
				
				int idx = r.nextInt(availFreqs.length);
				int freq = availFreqs[idx];
				dvfs.setFrequency(coreid, freq);
				int setfreq = dvfs.getFrequency(coreid);
				if (setfreq != freq) throw new Exception("The current frequency is different from the required one!");
			}
		}
		long end = System.nanoTime();
		
		double meanlatency = ((end - start) / 1000.0)/(transitionpercore * numcores);
		System.out.println("Mean frequency transition latency is "+meanlatency+" microsecs (over "+(transitionpercore*numcores)+" transitions)");
		
		dvfs.close();
	}

}
