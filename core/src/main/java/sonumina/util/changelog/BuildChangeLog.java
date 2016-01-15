package sonumina.util.changelog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;

/**
 * This implementation turns a changelog into an text document. It relies on Maven's checkstyle plugin having executed
 * beforehand.
 *
 * @author Sebastian Bauer
 */
public class BuildChangeLog
{
    /** Xerces configuration parameter for disabling fetching and checking XMLs against their DTD. */
    private static final String DISABLE_DTD_PARAM = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    public static void main(String[] args) throws IOException, InterruptedException
    {
        PrintStream out = System.out;
        File rawChangelog = new File("target/changelog.xml");

        if (args.length > 0) {
            out = new PrintStream(new FileOutputStream(args[0]));
        }
        if (args.length > 1) {
            rawChangelog = new File(args[1]);
        }
        if (!rawChangelog.exists() || !rawChangelog.isFile() || !rawChangelog.canRead()) {
            System.err.println("changelog.xml not found, aborting");
            return;
        }

        /* Process the output */
        Change[] changes = process(new FileInputStream(rawChangelog));
        if (changes == null) {
            // No user-facing changes detected
            return;
        }
        DateFormat fmt = DateFormat.getDateInstance(DateFormat.MEDIUM);
        for (Change change : changes) {
            out.println(fmt.format(change.date));
            out.println(" - " + change.logString + " (" + change.authorString + ")");
            out.println();
        }
    }

    public static Change[] process(InputStream rawChangelog)
    {
        DOMImplementationLS lsp = null;
        try {
            lsp = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS 3.0");
        } catch (Exception ex) {
            System.err.println("Cannot initialize an XML parser: " + ex.getMessage());
            return null;
        }
        LSInput in = lsp.createLSInput();
        in.setByteStream(rawChangelog);
        LSParser p = lsp.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
        p.getDomConfig().setParameter("validate", false);
        if (p.getDomConfig().canSetParameter(DISABLE_DTD_PARAM, false)) {
            p.getDomConfig().setParameter(DISABLE_DTD_PARAM, false);
        }
        Document doc = p.parse(in);

        ArrayList<Change> result = new ArrayList<Change>(100);
        NodeList changes = doc.getElementsByTagName("changelog-entry");

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Pattern userMessage = Pattern.compile(".*\\$foruser\\$\\s*(\\w.*?)\n", Pattern.DOTALL);

        for (int i = 0; i < changes.getLength(); ++i) {
            Element change = (Element) changes.item(i);
            String logString = change.getElementsByTagName("msg").item(0).getTextContent();
            if (logString.indexOf("$foruser$") < 0) {
                continue;
            }
            Matcher match = userMessage.matcher(logString);
            while (match.find()) {
                logString = match.group(1);
                String dateString = change.getElementsByTagName("date").item(0).getTextContent();
                String authorString = change.getElementsByTagName("author").item(0).getTextContent();
                int emailIdx = authorString.indexOf('<');
                if (emailIdx >= 0) {
                    authorString = authorString.substring(0, emailIdx);
                }
                Date date;

                try {
                    date = df.parse(dateString);

                    Change c = new Change();
                    c.authorString = authorString.trim();
                    c.date = date;
                    c.dateString = dateString.trim();
                    c.logString = logString.trim();

                    result.add(c);
                } catch (ParseException e) {
                }
            }
        }
        Change[] c = new Change[result.size()];
        result.toArray(c);
        return c;
    }
}
