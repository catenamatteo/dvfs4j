package eu.nicecode.dvfs4j;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class permits to control the operating frequency of CPU cores in Linux systems. 
 * To work, it requires the Linux {@code acpi_cpufreq} driver to be enabled.
 * Also, it requires access permissions on the files managed by {@code acpi_cpufreq}. 
 * This can be achieved, for instance, via:</br>
 * sudo chown owner:ownergrop /sys/devices/system/cpu/cpu{@literal *}/cpufreq/{@literal *}</br>
 * DVFS currently supports only systems equipped with a single multi-core CPU.</br>
 * To get an instance of DVFS, please use {@link eu.nicecode.dvfs4j.DVFSFactory}.
 * 
 * @author Matteo Catena
 *
 */
public class DVFS implements Closeable{

	private int numberOfCores;
	private int[] availableFrequencies;
	
	private String previousGovernor;
	private int previousFrequency;
	private int previousMaxFrequency;
	private int previousMinFrequency;
	private int[] currentFrequency;
	
	private int[] idx;
	
	DVFS() {
		
		try {

			// get the number of processors
			Process p = Runtime.getRuntime().exec("nproc");
			p.waitFor();
			
			BufferedReader in = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String result = in.readLine();
			in.close();
			numberOfCores = Integer.parseInt(result);
			
			currentFrequency = new int[numberOfCores];
			idx = new int[numberOfCores];
			
			// get the available frequencies
			in = new BufferedReader(new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies"));
			result = in.readLine();
			in.close();
			String[] freqStrings = result.split(" ");
			int[] declaredFrequencies = new int[freqStrings.length];
			for (int i = 0; i < declaredFrequencies.length; i++)  declaredFrequencies[i] = Integer.parseInt(freqStrings[i]);
			Arrays.sort(declaredFrequencies);
			
			//save the current governor, for restore
			in = new BufferedReader(new FileReader(
					"/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"));
			previousGovernor = in.readLine();
			in.close();
			
			//save the current frequency, for restore
			in = new BufferedReader(new InputStreamReader(new FileInputStream("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")));
			previousFrequency = Integer.parseInt(in.readLine());
			in.close();
			
			//save the current min frequency, for restore
			in = new BufferedReader(new InputStreamReader(new FileInputStream("/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq")));
			previousMinFrequency = Integer.parseInt(in.readLine());
			in.close();
			
			//save the current max frequency, for restore
			in = new BufferedReader(new InputStreamReader(new FileInputStream("/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq")));
			previousMaxFrequency = Integer.parseInt(in.readLine());
			in.close();
			
			//set the min possible frequency
			for (int i = 0; i < numberOfCores; i++) {
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("/sys/devices/system/cpu/cpu"+i+"/cpufreq/scaling_min_freq"));
				osw.write(Integer.toString(declaredFrequencies[0]));
				osw.close();
			}
			
			//set the max possible frequency
			for (int i = 0; i < numberOfCores; i++) {
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("/sys/devices/system/cpu/cpu"+i+"/cpufreq/scaling_max_freq"));
				osw.write(Integer.toString(declaredFrequencies[declaredFrequencies.length - 1]));
				osw.close();
			}
			
			//set the userspace governor
			for (int i = 0; i < numberOfCores; i++) {
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("/sys/devices/system/cpu/cpu"+i+"/cpufreq/scaling_governor"));
				osw.write("userspace");
				osw.close();
			}
			
			//compute the available frequency (get rid of "strange" cpu behaviours)
			List<Integer> af = new ArrayList<Integer>();
			for (int df : declaredFrequencies) {
				setFrequency(0, df, declaredFrequencies);
				if (df == getFrequency(0)) af.add(df);
			}
			availableFrequencies = new int[af.size()];
			for (int i = 0; i < af.size(); i++) availableFrequencies[i] = af.get(i);
			Arrays.sort(availableFrequencies);
			
			//now push the cpu to the min frequency
			setMinFrequency();
			
		} catch (IOException e) {

			throw new RuntimeException(e);
			
		} catch (InterruptedException e) {

			throw new RuntimeException(e);
		}
		
		
	}
	
	/**
	 * Close DVFS and restore the system state prior to its usage
	 */
	public void close() {
		
		try {

			//restore the previous frequency
			for (int i = 0; i < numberOfCores; i++) {
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("/sys/devices/system/cpu/cpu"+i+"/cpufreq/scaling_setspeed"));
				osw.write(Integer.toString(previousFrequency));
				osw.close();
			}
			
			//restore the min frequency
			for (int i = 0; i < numberOfCores; i++) {
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("/sys/devices/system/cpu/cpu"+i+"/cpufreq/scaling_min_freq"));
				osw.write(Integer.toString(previousMinFrequency));
				osw.close();
			}
			
			//restore the max frequency
			for (int i = 0; i < numberOfCores; i++) {
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("/sys/devices/system/cpu/cpu"+i+"/cpufreq/scaling_max_freq"));
				osw.write(Integer.toString(previousMaxFrequency));
				osw.close();
			}
			
			//restore the previous governor
			for (int i = 0; i < numberOfCores; i++) {
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("/sys/devices/system/cpu/cpu"+i+"/cpufreq/scaling_governor"));
				osw.write(previousGovernor);
				osw.close();
			}
			
			DVFSFactory.dvfs = null;
			
		} catch (IOException e) {

			throw new RuntimeException(e);
			
		}		
	}

	/**
	 * Set the required frequency {@code freq} on core {@code coreid}
	 * @param coreid
	 * @param freq
	 */
	public void setFrequency(int coreid, int freq) {
		
		setFrequency(coreid, freq, availableFrequencies);
		
	}
	
	/**
	 * Set the required frequency on all the cores
	 * @param freq
	 */
	public void setFrequency(int freq) {
		
		for (int i = 0; i < numberOfCores; i++) {
			setFrequency(i, freq, availableFrequencies);
		}
		
	}
	
	private void setFrequency(int coreid, int freq, int[] frequencies) {
		
		if (!isFreqAvailable(freq, frequencies))
			throw new IllegalArgumentException("Unavailable frequency!");
		
		try {
			
			idx[coreid] = Arrays.binarySearch(frequencies, freq);
			
			OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("/sys/devices/system/cpu/cpu"+coreid+"/cpufreq/scaling_setspeed"));
			osw.write(Integer.toString(freq));
			osw.close();
			
			currentFrequency[coreid] = freq;
			
		} catch (FileNotFoundException e) {
			
			throw new RuntimeException(e);
			
		} catch (IOException e) {
			
			throw new RuntimeException(e);		
		}
	}
	
	private boolean isFreqAvailable(int freq, int[] frequencies) {

		for (int f : frequencies)
			if (f == freq)
				return true;
		
		return false;
	}

	/**
	 * Get the frequency of code {@code coreid}
	 * @param coreid
	 * @return
	 */
	public int getFrequency(int coreid) {
		
		return currentFrequency[coreid];
	}
	
	/**
	 * Set core {@code coreid} to the maximum frequency
	 * @param coreid
	 */
	public void setMaxFrequency(int coreid) {
	
		setFrequency(coreid, availableFrequencies[availableFrequencies.length-1]);
	}
	
	/**
	 * Set the maximum frequency on all the cores
	 */
	public void setMaxFrequency() {
	
		setFrequency(availableFrequencies[availableFrequencies.length-1]);
	}
	
	/**
	 * Set core {@code coreid} to the minimum frequency
	 * @param coreid
	 */
	public void setMinFrequency(int coreid) {

		setFrequency(coreid, availableFrequencies[0]);
	}
	
	/**
	 * Set the minimum frequency on all the cores
	 * @param coreid
	 */
	public void setMinFrequency() {

		setFrequency(availableFrequencies[0]);
	}

	/**
	 * Get the maximum frequency available on the CPU
	 * @return
	 */
	public int getMaxFrequency() {
		
		return availableFrequencies[availableFrequencies.length - 1];
	}
	
	/**
	 * Get the minimum frequency available on the CPU
	 * @param coreid
	 */
	public int getMinFrequency() {
		
		return availableFrequencies[0];
	}
	
	/**
	 * Increase the frequency of core {@code coreid} by one step
	 * @param coreid
	 */
	public void increaseFrequency(int coreid) {
		
		if (idx[coreid] < (availableFrequencies.length - 1)) {
			
			setFrequency(availableFrequencies[++idx[coreid]]);
			
		}
		
	}

	/**
	 * Increase the frequency of all the cores by one step
	 * @param coreid
	 */
	public void increaseFrequency() {
		
		for (int i = 0 ; i < numberOfCores; i++) {
			if (idx[i] < (availableFrequencies.length - 1)) {
				
				setFrequency(i, availableFrequencies[++idx[i]]);
				
			}
		}
	}
	
	/**
	 * Decrease the frequency of core {@code coreid} by one step
	 * @param coreid
	 */
	public void decreaseFrequency(int coreid) {
		
		if (idx[coreid] > 0) {
			
			setFrequency(coreid, availableFrequencies[--idx[coreid]]);
			
		}
		
	}
	
	/**
	 * Decrease the frequency of all the cores by one step
	 * @param coreid
	 */
	public void decreaseFrequency() {

		for (int i = 0 ; i < numberOfCores; i++) {

			if (idx[i] > 0) {
				
				setFrequency(i, availableFrequencies[--idx[i]]);
				
			}
		}
		
	}
	
	/**
	 * Get the frequencies available on the CPU
	 * @return
	 */
	public int[] getAvailableFrequencies() {
		return availableFrequencies;
	}

	/**
	 * Set the frequency of core {@code coreid} to the first available frequency
	 * greater or equal than {@code freq}
	 * @param coreid
	 * @param freq
	 */
	public void setFrequencyGEQ(int coreid, int freq) {
		
		for (int i = 0; i < availableFrequencies.length; i++) {
			
			if (availableFrequencies[i] >= freq) {
				
				setFrequency(coreid, availableFrequencies[i]);
				return;
			}
			
		}
		
		setMaxFrequency(coreid);
		return;
	}
	
	/**
	 * Set the frequency of all the cores to the first available frequency
	 * greater or equal than {@code freq}
	 * @param freq
	 */
	public void setFrequencyGEQ(int freq) {
		
		for (int i = 0; i < numberOfCores; i++)
			setFrequencyGEQ(i, freq);
	}
	
	/**
	 * Set the frequency of core {@code coreid} to the first available frequency
	 * less or equal than {@code freq}
	 * @param coreid
	 * @param freq
	 */
	public void setFrequencyLEQ(int coreid, int freq) {
		
		for (int i = availableFrequencies.length - 1; i >= 0; i--) {
			
			if (availableFrequencies[i] <= freq) {
				
				setFrequency(coreid, availableFrequencies[i]);
				return;
			}
			
		}
		
		setMinFrequency(coreid);
		return;
	}
	
	
	/**
	 * Set the frequency of all the cores to the first available frequency
	 * less or equal than {@code freq}
	 * @param freq
	 */
	public void setFrequencyLEQ(int freq) {
		
		for (int i = 0; i < numberOfCores; i++)
			setFrequencyLEQ(i, freq);
	}


	/**
	 * Get the number of logical cores available on the CPU
	 * @return
	 */
	public int getNumOfCores() {
		
		return numberOfCores;
	}
	
}
