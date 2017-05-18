# dvfs4j
dvfs4j permits to control the operating frequency of CPU cores in Linux systems, from within a Java application.</br>
To work, it requires the Linux <tt>[acpi_cpufreq](https://www.kernel.org/doc/Documentation/cpu-freq/index.txt)</tt> driver to be enabled.
Also, it requires access permissions on the files managed by <tt>acpi_cpufreq</tt>. 
This can be achieved, for instance, via:</br>
<tt>sudo chown owner:ownergroup /sys/devices/system/cpu/cpu\*/cpufreq/\*</tt></br>

dvfs4j currently supports only systems equipped with a single multi-core CPU.

To get an instance of the DVFS class, please use the DVFSFactory.
