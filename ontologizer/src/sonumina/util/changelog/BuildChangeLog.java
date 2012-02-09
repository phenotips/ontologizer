package sonumina.util.changelog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This implementation turns an svn log into an text document. It calls
 * "svn log" on the current directory..
 * 
 * @author Sebastian Bauer
 */
public class BuildChangeLog
{
	public static Change [] process(String string)
	{
		ArrayList<Change> list = new ArrayList<Change>(100);
		String [] commits = string.split("-+\n");

		Pattern pat = Pattern.compile("r(\\d+)\\s+\\|\\s+(\\w+)\\s+\\|\\s+(.+?)\\s+\\|.*?\\$foruser\\$(.*)",Pattern.DOTALL);

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

		for (int i = 1;i<commits.length;i++)
		{
			Matcher mat = pat.matcher(commits[i]);
			while (mat.find())
			{
				String revisionString = mat.group(1);
				String authorString = mat.group(2);
				String dateString = mat.group(3);
				String logString = mat.group(4);
				Date date;

				try
				{
					date = df.parse(dateString);
					
					Change c = new Change();
					c.authorString = authorString.trim();
					c.date = date;
					c.dateString = dateString.trim();
					c.logString = logString.trim();
					c.revisionString = revisionString.trim();

					list.add(c);
				} catch (ParseException e) {
				}
			
			}
		}
		Change [] c = new Change[list.size()];
		list.toArray(c);
		return c;
	}

	public static void main(String[] args) throws IOException, InterruptedException
	{
		/* Start svn log and read the output */
		Process svnProcess = Runtime.getRuntime().exec(new String[]{"svn","log"});
		BufferedReader br = new BufferedReader(new InputStreamReader(svnProcess.getInputStream()));
		StringBuilder str = new StringBuilder();
		String line;
		while ((line = br.readLine())!=null)
			str.append(line + "\n");
		svnProcess.waitFor();

		/* Process the output */
		Change [] changes = process(str.toString());
		for (Change change : changes)
		{
			System.out.println(DateFormat.getDateInstance(DateFormat.MEDIUM).format(change.date) + " - r" + change.revisionString);
			System.out.println(" - " + change.logString + " (" + change.authorString + ")");
			System.out.println();
		}
	}
}
