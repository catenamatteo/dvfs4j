package eu.nicecode.dvfs4j;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import eu.nicecode.dvfs4j.DVFS;
import eu.nicecode.dvfs4j.DVFSFactory;

public class TestDVFS{

	@Test
	public void test() {
		
		DVFS dvfs = DVFSFactory.getDVFS();
		int numcore = dvfs.getNumOfCores();
		int[] availfreq = dvfs.getAvailableFrequencies();

		dvfs.setMinFrequency(0);
		assertEquals(dvfs.getMinFrequency(), dvfs.getFrequency(0));
		
		dvfs.setMaxFrequency(0);
		assertEquals(dvfs.getMaxFrequency(), dvfs.getFrequency(0));
		
		dvfs.setMinFrequency();
		for (int i = 0; i < numcore; i++) assertEquals(dvfs.getMinFrequency(), dvfs.getFrequency(i));
		dvfs.setMaxFrequency();
		for (int i = 0; i < numcore; i++) assertEquals(dvfs.getMaxFrequency(), dvfs.getFrequency(i));		
		
		int freq = availfreq[1];
		dvfs.setFrequency(0, freq);
		assertEquals(freq, dvfs.getFrequency(0));
		
		dvfs.setFrequency(freq);
		for (int i = 0; i < numcore; i++) assertEquals(freq, dvfs.getFrequency(i));
		
		dvfs.increaseFrequency(0);
		assertEquals(availfreq[2], dvfs.getFrequency(0));

		dvfs.decreaseFrequency(0);
		assertEquals(freq, dvfs.getFrequency(0));
		
		dvfs.setMinFrequency();
		dvfs.increaseFrequency();
		for (int i = 0; i < numcore; i++) assertEquals(availfreq[1], dvfs.getFrequency(i));
		dvfs.decreaseFrequency();
		for (int i = 0; i < numcore; i++) assertEquals(dvfs.getMinFrequency(), dvfs.getFrequency(i));
		
		dvfs.setFrequencyGEQ(0, availfreq[1]+1);
		assertEquals(availfreq[2], dvfs.getFrequency(0));
		dvfs.setFrequencyGEQ(availfreq[1]+1);
		for (int i = 0; i < numcore; i++) assertEquals(availfreq[2], dvfs.getFrequency(i));
		
		dvfs.setFrequencyLEQ(0, availfreq[2]-1);
		assertEquals(availfreq[1], dvfs.getFrequency(0));
		dvfs.setFrequencyLEQ(availfreq[2]-1);
		for (int i = 0; i < numcore; i++) assertEquals(availfreq[1], dvfs.getFrequency(i));
		
		dvfs.close();		
	}
	
}
