/*
 * myException.java
 *
 */

package ontologizer.go;

/**
 * An exception which may be thrown by the OBOParser class.
 *
 * @see OBOParser
 * @author Sebastian Bauer
 */
public class OBOParserException extends Exception
{
    /** Serial UID */
    private static final long serialVersionUID = 1L;

    protected int linenum;

    protected String line;

    protected String filename;

    public OBOParserException(String message, String line, int linenum)
    {
        super(message);
        this.line = line;
        this.linenum = linenum;
        this.filename = "<tempfile>";
    }

    public OBOParserException(String message)
    {
        super(message);
        this.line = "";
        this.linenum = -1;
        this.filename = "<tempfile>";
    }

    public String getLine()
    {
        return this.line;
    }

    public int getLineNum()
    {
        return this.linenum;
    }

    @Override
    public String toString()
    {
        String loc;

        if (this.linenum >= 0) {
            loc = this.filename + ":" + this.linenum;
        } else {
            loc = this.filename;
        }

        if (this.line != null) {
            return loc + " obo parser error: " + getMessage() + " in \"" + this.line + "\".";
        }
        return loc + " obo parser error: " + getMessage() + ".";
    }
}
