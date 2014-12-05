package ontologizer.go;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides a representation of individual GOTerms. Example:
 *
 * <pre>
 * [Term]
 * id: GO:0000018
 * name: regulation of DNA recombination
 * namespace: biological_process
 * def: "Any process that modulates the frequency\, rate or extent of DNA recombination\, the processes by which a new
 * genotype is formed by reassortment of genes resulting in gene combinations different from those that were present in
 * the parents." [GO:curators, ISBN:0198506732]
 * is_a: GO:0051052 (cardinality 0..n)
 * relationship: part_of GO:0006310 (cardinality 0..n)
 * </pre>
 * <p>
 * Both is_a and part_of refer to child-parent relationships in the GO directed acyclic graph. The Ontologizer does not
 * distinguish between these types of child-parent relationships, but rather places both type of parent in an ArrayList
 * termed parents. This will allow us to traverse the DAG while we are tabulating the counts of functions found in a
 * cluster.
 * </p>
 *
 * @author Peter Robinson, Sebastian Bauer, Sebastian Koehler
 */

public class Term
{
    /** The id ("accession number") of this GO term */
    private TermID id;

    /** The short human readable name of the id */
    private String name;

    /**
     * The definition of this term. This might be null if this information is not available
     */
    private String definition;

    /** The parents of the this term */
    private ParentTermID[] parents;

    /** The term's alternatives */
    private ArrayList<TermID> alternatives;

    /** The term's alternatives */
    private TermID[] equivalents;

    /** The synonyms of this term, as read from the obo file. */
    private String[] synonyms;

    /** The intersections tags of this term, as read from the obo file. */
    private String[] intersections;

    /** The term's subsets */
    private Subset[] subsets;

    /** The term's xrefs */
    private TermXref[] xrefs;

    /** The term's name space */
    private Namespace namespace;

    /** Whether term is declared as obsolete */
    private boolean obsolete;

    private double informationContent = -1;

    /**
     * @param id A term id.
     * @param name A string such as glutathione dehydrogenase.
     * @param namespace A character representing biological_process, cellular_component, or molecular_function or null.
     * @param parentList The parent terms of this term including the relation type. The supplied list can be reused
     *            after the object have been constructed.
     */
    public Term(TermID id, String name, Namespace namespace, ArrayList<ParentTermID> parentList)
    {
        this.parents = new ParentTermID[parentList.size()];
        parentList.toArray(this.parents);
        init(id, name, namespace, this.parents);
    }

    /**
     * @param strId An identifier such as GO:0045174.
     * @param name A string such as glutathione dehydrogenase.
     * @param namespace The name space attribute of the term or null.
     * @param parentList The parent terms of this term including the relation type. The supplied list can be reused
     *            after the object have been constructed.
     * @throws IllegalArgumentException if strId is malformatted.
     */
    public Term(String strId, String name, Namespace namespace, ArrayList<ParentTermID> parentList)
    {
        this.parents = new ParentTermID[parentList.size()];
        parentList.toArray(this.parents);
        init(new TermID(strId), name, namespace, this.parents);
    }

    /**
     * @param id A term id.
     * @param name A string such as glutathione dehydrogenase.
     * @param namespace The name space attribute of the term or null.
     * @param parents The parent terms of this term including the relation type.
     */
    public Term(TermID id, String name, Namespace namespace, ParentTermID... parents)
    {
        init(id, name, namespace, parents);
    }

    /**
     * Here, the namespace is set to UNKOWN.
     *
     * @param id A term id.
     * @param name A string such as glutathione dehydrogenase.
     * @param parents The parent terms of this term including the relation type.
     */
    public Term(TermID id, String name, ParentTermID... parents)
    {
        init(id, name, null, parents);
    }

    /**
     * Here, the namespace is set to UNKOWN.
     *
     * @param strId An identifier such as GO:0045174.
     * @param name A string such as glutathione dehydrogenase.
     * @param parents The parent terms of this term including the relation type.
     * @throws IllegalArgumentException if strId is malformatted.
     */
    public Term(String strId, String name, ParentTermID... parents)
    {
        init(new TermID(strId), name, null, parents);
    }

    /**
     * @param strId An identifier such as GO:0045174.
     * @param name A string such as glutathione dehydrogenase.
     * @param namespace The name space attribute of the term or null.
     * @param parents The parent terms of this term including the relation type.
     * @throws IllegalArgumentException if strId is malformatted.
     */
    public Term(String strId, String name, Namespace namespace, ParentTermID... parents)
    {
        init(new TermID(strId), name, namespace, parents);
    }

    /**
     * Constructor helper.
     *
     * @param strId
     * @param name
     * @param namespace
     * @param parents
     */
    private void init(TermID id, String name, Namespace namespace, ParentTermID[] parents)
    {
        this.id = id;
        this.name = name;
        this.parents = parents;

        if (namespace == null) {
            namespace = Namespace.UNKOWN_NAMESPACE;
        } else {
            this.namespace = namespace;
        }
    }

    /**
     * Returns the Term ID as a string (uses StringBuilder)
     *
     * @return term:id
     */
    public String getIDAsString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.id.getPrefix());
        buffer.append(":");
        if (this.id.id < 10) {
            buffer.append("000000");
        } else if (this.id.id < 100) {
            buffer.append("00000");
        } else if (this.id.id < 1000) {
            buffer.append("0000");
        } else if (this.id.id < 10000) {
            buffer.append("000");
        } else if (this.id.id < 100000) {
            buffer.append("00");
        } else if (this.id.id < 1000000) {
            buffer.append("0");
        }
        buffer.append(this.id.id);
        return buffer.toString();
    }

    /**
     * Returns the GO ID as TermID object.
     *
     * @return the id
     */
    public TermID getID()
    {
        return this.id;
    }

    /**
     * @return go:name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * gets the namespace of the term as a Namespace enum
     *
     * @return
     */
    public Namespace getNamespace()
    {
        if (this.namespace == null) {
            return Namespace.UNKOWN_NAMESPACE;
        }
        return this.namespace;
    }

    /**
     * gets a single letter String representation of the term's namespace
     *
     * @return
     */
    public String getNamespaceAsString()
    {
        if (this.namespace == null) {
            return Namespace.UNKOWN_NAMESPACE.getName();
        }
        return this.namespace.getName();
    }

    /**
     * Returns the abbreviated string for the term's namespace if possible.
     *
     * @return
     */
    public String getNamespaceAsAbbrevString()
    {
        if (this.namespace == null) {
            return Namespace.UNKOWN_NAMESPACE.getName();
        }
        String nameSpace = this.namespace.getName();

        if (nameSpace.equalsIgnoreCase("biological_process")) {
            return "B";
        }
        if (nameSpace.equalsIgnoreCase("molecular_function")) {
            return "M";
        }
        if (nameSpace.equalsIgnoreCase("cellular_component")) {
            return "C";
        }
        return nameSpace;
    }

    /**
     * Returns the parent terms including the relation.
     *
     * @return
     */
    public ParentTermID[] getParents()
    {
        return this.parents;
    }

    @Override
    public String toString()
    {
        return this.name + " (" + this.id.toString() + ")";
    }

    @Override
    public int hashCode()
    {
        /* We take the hash code of the id */
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Term) {
            Term goTerm = (Term) obj;
            return goTerm.id.equals(this.id);
        }
        return super.equals(obj);
    }

    /**
     * Sets the obsolete state of this term
     *
     * @param currentObsolete
     */
    protected void setObsolete(boolean currentObsolete)
    {
        this.obsolete = currentObsolete;
    }

    /**
     * @return whether term is declared as obsolete
     */
    public boolean isObsolete()
    {
        return this.obsolete;
    }

    /**
     * Returns the definition of this term. Might be null if none is available.
     *
     * @return the definition or null.
     */
    public String getDefinition()
    {
        return this.definition;
    }

    /**
     * Sets the definition of this term.
     *
     * @param definition defines the definition ;)
     */
    public void setDefinition(String definition)
    {
        this.definition = definition;
    }

    public void setEquivalents(ArrayList<TermID> currentEquivalents)
    {
        this.equivalents = new TermID[currentEquivalents.size()];
        int i = 0;
        for (TermID t : currentEquivalents) {
            this.equivalents[i++] = t;
        }
    }

    public TermID[] getEquivalents()
    {
        return this.equivalents;
    }

    /**
     * This sets the alternatives of the term.
     *
     * @param altList
     */
    public void setAlternatives(List<TermID> altList)
    {
        this.alternatives = new ArrayList<TermID>();
        this.alternatives.addAll(altList);
    }

    /**
     * Returns the alternatives of this term.
     *
     * @return
     */
    public ArrayList<TermID> getAlternatives()
    {
        return this.alternatives;
    }

    /**
     * Sets the subsets.
     *
     * @param newSubsets
     */
    public void setSubsets(ArrayList<Subset> newSubsets)
    {
        this.subsets = new Subset[newSubsets.size()];
        newSubsets.toArray(this.subsets);
    }

    /**
     * Returns the subsets.
     *
     * @return
     */
    public Subset[] getSubsets()
    {
        return this.subsets;
    }

    public void setSynonyms(ArrayList<String> currentSynonyms)
    {

        if (currentSynonyms.size() > 0) {
            this.synonyms = new String[currentSynonyms.size()];
            currentSynonyms.toArray(this.synonyms);
        }
    }

    public String[] getSynonyms()
    {
        return this.synonyms;
    }

    public void setXrefs(ArrayList<TermXref> currentXrefs)
    {
        if (currentXrefs.size() > 0) {
            this.xrefs = new TermXref[currentXrefs.size()];
            currentXrefs.toArray(this.xrefs);
        }
    }

    public TermXref[] getXrefs()
    {
        return this.xrefs;
    }

    public void setIntersections(ArrayList<String> currentIntersections)
    {
        if (currentIntersections.size() > 0) {
            this.intersections = new String[currentIntersections.size()];
            currentIntersections.toArray(this.intersections);
        }

    }

    public String[] getIntersections()
    {
        return this.intersections;
    }

    public void addAlternativeId(TermID id2)
    {
        if (this.alternatives == null) {
            this.alternatives = new ArrayList<TermID>();
        }
        this.alternatives.add(id2);
    }

    public void setInformationContent(double informationContent)
    {
        this.informationContent = informationContent;
    }

    public double getInformationContent()
    {
        return this.informationContent;
    }

}
