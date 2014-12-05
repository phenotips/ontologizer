
package ontologizer;

import java.util.ResourceBundle;

/**
 * Simple class to get some info about the current
 * Ontologizer instance. 
 * 
 * @author Sebastian Bauer
 *
 */
public class BuildInfo
{
	private static String revisionNumber="NA";
	private static String date ="NA";
	private static String version = "NA";
	private static String copyright = "2005-2012";
	private static boolean infoExtracted = false;

	/**
	 * Extract the info stored in {@code ontologizer.properties}.
	 */
	private static void extractInfo()
	{
		if (infoExtracted)
			return;
		
		ResourceBundle properties = ResourceBundle.getBundle("ontologizer");
		revisionNumber = properties.getString("build.revision");
		date = properties.getString("build.date");
		copyright = copyright.substring(0, copyright.indexOf('-')) + '-' + date.substring(0, date.indexOf('-'));
		version = properties.getString("application.version");
		infoExtracted = true;
	}
	
	/**
	 * Returns the revision number.
	 * 
	 * @return
	 */
	public static String getRevisionNumber()
	{
		extractInfo();
		
		return revisionNumber;
	}
	
	/**
	 * Returns the compilation date.
	 * 
	 * @return
	 */
	public static String getDate()
	{
		extractInfo();

		return date;
	}
	
	/**
	 * Returns the version string.
	 * 
	 * @return
	 */
	public static String getVersion()
	{
		return version;
	}

	/**
	 * Returns the copyright years.
	 * 
	 * @return
	 */
	public static String getCopyright()
	{
		return copyright;
	}
	
	/**
	 * Returns the build string.
	 * 
	 * @return
	 */
	public static String getBuildString()
	{
		return BuildInfo.getDate() + "-" + BuildInfo.getRevisionNumber();
	}
}
