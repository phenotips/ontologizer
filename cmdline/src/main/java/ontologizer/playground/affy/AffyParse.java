package ontologizer.playground.affy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AffyParse
{
    private String path;

    private BufferedReader in;

    /*
     * This represents the affymetrix annotation format as of May 15th, 2006. The code uses the following to check that
     * the headers have stayed the same. If anything has changed, then it is worthwhile checking the code again to make
     * sure the code is doing what it thinks it is doing. Therefore, throw an error if something is amiss.
     */
    private static final String[] annot =
        {
        "Probe Set ID", /* 0 */
        "GeneChip Array",
        "Species Scientific Name",
        "Annotation Date",
        "Sequence Type",
        "Sequence Source",
        "Transcript ID(Array Design)",
        "Target Description",
        "Representative Public ID",
        "Archival UniGene Cluster",
        "UniGene ID", /* 10 */
        "Genome Version",
        "Alignments",
        "Gene Title",
        "Gene Symbol",
        "Chromosomal Location",
        "Unigene Cluster Type",
        "Ensembl",
        "Entrez Gene",
        "SwissProt",
        "EC", /* 20 */
        "OMIM",
        "RefSeq Protein ID",
        "RefSeq Transcript ID",
        "FlyBase",
        "AGI",
        "WormBase",
        "MGI Name",
        "RGD Name",
        "SGD accession number",
        "Gene Ontology Biological Process",
        "Gene Ontology Cellular Component",
        "Gene Ontology Molecular Function",
        "Pathway",
        "Protein Families",
        "Protein Domains",
        "InterPro",
        "Trans Membrane",
        "QTL",
        "Annotation Description",
        "Annotation Transcript Cluster",
        "Transcript Assignments",
        "Annotation Notes",
        };

    private Map<String, ArrayList<String>> affy2swiss;

    public AffyParse(String filename)
    {
        this.path = filename;

        this.affy2swiss = new HashMap<>();

        try {
            this.in = new BufferedReader(new FileReader(this.path));
            input();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void input() throws IOException
    {
        String str;
        if ((str = this.in.readLine()) == null) {
            System.err.println("Coud not read a line from " + this.path);
        } else {
            parseHeader(str);

        }

        while ((str = this.in.readLine()) != null) {
            parseDataline(str);
        }
        this.in.close();
    }

    private void parseDataline(String line) throws IOException
    {
        String probeid = null, swiss = null;

        int len = line.length();
        int x, y;
        int idx;
        x = -1;
        idx = 0;
        String item;
        for (int i = 0; i < len; ++i) {
            if (line.charAt(i) == '\"') {
                if (x == -1) {
                    x = i;
                } else {
                    y = i;
                    if (y > x) {
                        item = line.substring(x + 1, y);
                        // System.out.println(idx + ": " + item);
                        if (idx == 0) {
                            probeid = item;
                        } else if (idx == 19) {
                            swiss = item;
                            System.out.println(probeid + ": " + swiss);
                            if (this.affy2swiss.containsKey(probeid)) {
                                System.out.println("Error, affy2swiss already contains id");
                                System.exit(1);
                            } else {
                                enterProbeset(probeid, swiss);
                            }
                        }
                        idx++;
                        x = -1;
                    }
                }

            }
        }
        if (idx != annot.length) {
            System.out.println("LENS DONT MATHC");
            System.exit(1);
        }
    }

    public void debug()
    {
        System.out.println("Affymetrix ");
        int l = this.affy2swiss.size();
        int counts[] = new int[1000];
        int max = 0;
        System.out.println("Es gibt " + l + " probesets");
        Set<String> keys = this.affy2swiss.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String id = it.next();
            ArrayList<String> al = this.affy2swiss.get(id);
            int s = al.size();
            if (s > max) {
                max = s;
            }
            counts[s]++;
        }
        System.out.println("Distribution of swissprots per probeset");
        for (int i = 0; i <= max; ++i) {
            System.out.println("\n" + i + ": " + counts[i] + " probesets");
        }
    }

    private void enterProbeset(String affy, String swiss)
    {
        String[] prot;
        ArrayList<String> al;
        String delim = "///";
        prot = swiss.split(delim);
        al = new ArrayList<>();
        for (String element : prot) {
            String item = element;
            item = item.trim();
            al.add(item);
        }
        this.affy2swiss.put(affy, al);
    }

    private void parseHeader(String line) throws IOException
    {
        String fields[];
        String delim = ",";
        fields = line.split(delim);
        for (int i = 0; i < fields.length; i++) {
            String item = fields[i];
            int x, y; // first and last index of quotation mark
            x = item.indexOf('"') + 1;
            y = item.lastIndexOf('"');
            if (x == 0 && y == (item.length() - 1)) {
                System.out.print("OK");
            }
            item = item.substring(x, y);
            if (!item.equals(annot[i])) {
                System.err.println("Format of affymetrix annotations does " +
                    " not match what I expected.");
                System.err.println("I expected \"" + annot[i] + "\" but I got \"" + item +
                    "\". Exiting, please check...");
                System.exit(1);
            }
        }

    }
}
