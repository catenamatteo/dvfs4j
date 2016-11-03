package eu.nicecode.dvfs4j;

public class DVFSFactory {

	static DVFS dvfs;
	
	/**
	 * Get an instance of {@link eu.nicecode.dvfs4j.DVFS} (singleton) 
	 * @return
	 */
	public static DVFS getDVFS() {
		
		if (dvfs == null)
			return dvfs = new DVFS();
		else
			return dvfs;
		
	}
	
	
}
