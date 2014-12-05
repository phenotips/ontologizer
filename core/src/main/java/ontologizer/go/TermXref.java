package ontologizer.go;

public class TermXref
{

    /**
     * The external database name, e.g. MeSH, ICD-10, UMLS
     */
    private String database;

    /**
     * The ID in the external DB, D012587, C2077312, Q20.4
     */
    private String xrefId;

    /**
     * The name of the referenced entity e.g. "Asymmetric lower limb shortness" for UMLS - C1844734
     */
    private String xrefName;

    public TermXref(String database, String xrefId)
    {

        this.database = database;
        this.xrefId = xrefId;

    }

    public TermXref(String database, String xrefId, String xrefName)
    {

        this.database = database;
        this.xrefId = xrefId;
        this.xrefName = xrefName;
    }

    @Override
    public int hashCode()
    {
        return this.database.hashCode() + this.xrefId.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {

        if (!(obj instanceof TermXref)) {
            return false;
        }

        TermXref otherXref = (TermXref) obj;

        if (this.database.equals(otherXref.database) && this.xrefId.equals(otherXref.xrefId)) {
            return true;
        }

        return false;

    }

    public String getDatabase()
    {
        return this.database;
    }

    public String getXrefId()
    {
        return this.xrefId;
    }

    public String getXrefName()
    {
        return this.xrefName;
    }

    /**
     * Returns 'db' - 'db-ID' if no name was given <br>
     * Returns 'db' - 'db-ID' - 'db-name' if name was given
     *
     * @return
     */
    @Override
    public String toString()
    {
        StringBuffer returnString = new StringBuffer();

        returnString.append(this.database);
        returnString.append(" - ");
        returnString.append(this.xrefId);

        if (this.xrefName != null) {
            returnString.append(" - ");
            returnString.append(this.xrefName);
        }

        return returnString.toString();
    }

}
