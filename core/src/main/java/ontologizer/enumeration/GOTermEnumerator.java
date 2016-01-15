package ontologizer.enumeration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import ontologizer.association.Association;
import ontologizer.association.Gene2Associations;
import ontologizer.go.Ontology;
import ontologizer.go.Ontology.IVisitingGOVertex;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.types.ByteString;

/**
 * This class encapsulates the enumeration of explicit and implicit annotations for an set of genes. You can iterate
 * conveniently over all GO terms where genes have been annotated to. Note that if you are only interested in the gene
 * counts per term you should use the class GOTermCounter as this is much faster.
 *
 * @author Sebastian Bauer
 */
public class GOTermEnumerator implements Iterable<TermID>
{
    public class GOTermAnnotatedGenes
    {
        /** List of directly annotated genes TODO: Make private */
        public List<ByteString> directAnnotated = new ArrayList<ByteString>();

        /** List of genes annotated at whole TODO: Make private */
        public List<ByteString> totalAnnotated = new ArrayList<ByteString>();

        public int directAnnotatedCount()
        {
            return this.directAnnotated.size();
        }

        public int totalAnnotatedCount()
        {
            return this.totalAnnotated.size();
        }
    }

    /** The GO graph */
    private Ontology graph;

    private HashMap<TermID, GOTermAnnotatedGenes> map;

    /** Holds the number of suspicious annotations */
    // private int suspiciousCount;

    /**
     * Construct the enumerator.
     *
     * @param graph the GO graph
     */
    public GOTermEnumerator(Ontology graph)
    {
        this.graph = graph;

        this.map = new HashMap<TermID, GOTermAnnotatedGenes>();
    }

    /**
     * @param geneAssociations
     */
    public void push(Gene2Associations geneAssociations)
    {
        push(geneAssociations, null);
    }

    /**
     * Pushes the given gene association into the enumerator. I.e. add the gene in question to all terms annotating that
     * gene.
     *
     * @param geneAssociations the gene associations
     * @param evidences consider only annotation entries that correspond to the given evidence codes.
     */
    public void push(Gene2Associations geneAssociations, Set<ByteString> evidences)
    {
        ByteString geneName = geneAssociations.name();

        /*
         * Check for suspicious annotations. An annotation i is suspicious if there exists a more specialized annotation
         * orgininating from i. If an annotation isn't suspicious it is valid and placed in validAssocs
         */
        /*
         * LinkedList<Association> validAssocs = new LinkedList<Association>(); for (Association i : geneAssociations) {
         * boolean existsPath = false; for (Association j : geneAssociations) { if (i.getGoID().equals(j.getGoID())) {
         * continue; } if (graph.existsPath(i.getGoID(), j.getGoID())) { suspiciousCount++; existsPath = true; } } if
         * (!existsPath) { // No direct path from i to another assoc exists hence the association is valid
         * validAssocs.add(i); } }
         */
        /*
         * Here we ignore the association qualifier (e.g. colocalized_with) completely.
         */
        HashSet<TermID> termIDSet = new HashSet<TermID>();

        /* At first add the direct counts and remember the terms */
        for (Association association : geneAssociations) {
            TermID termID = association.getTermID();

            if (!this.graph.isRelevantTermID(termID)) {
                continue;
            }

            if (evidences != null) {
                if (!evidences.contains(association.getEvidence())) {
                    continue;
                }
            }

            GOTermAnnotatedGenes termGenes = this.map.get(termID);

            /* Create an entry if it doesn't exist */
            if (termGenes == null) {
                termGenes = new GOTermAnnotatedGenes();
                this.map.put(termID, termGenes);
            }

            termGenes.directAnnotated.add(geneName);

            /* This term is annotated */
            termIDSet.add(association.getTermID());
        }

        /* Then add the total counts */

        /**
         * The term visitor: To all visited terms (which here all terms up from the goTerms of the set) add the given
         * gene.
         *
         * @author Sebastian Bauer
         */
        class VisitingGOVertex implements IVisitingGOVertex
        {
            private ByteString geneName;

            public VisitingGOVertex(ByteString geneName)
            {
                this.geneName = geneName;
            }

            @Override
            public boolean visited(Term term)
            {
                if (GOTermEnumerator.this.graph.isRelevantTermID(term.getID())) {
                    GOTermAnnotatedGenes termGenes = GOTermEnumerator.this.map.get(term.getID());

                    if (termGenes == null) {
                        termGenes = new GOTermAnnotatedGenes();
                        GOTermEnumerator.this.map.put(term.getID(), termGenes);
                    }
                    termGenes.totalAnnotated.add(this.geneName);
                }
                return true;
            }
        }
        ;

        /* Create the visting */
        VisitingGOVertex vistingGOVertex = new VisitingGOVertex(geneName);

        /* Walk from goTerm to source by using vistingGOVertex */
        this.graph.walkToSource(termIDSet, vistingGOVertex);
    }

    /**
     * Return genes directly or indirectly annotated to the given goTermID.
     *
     * @param goTermID
     * @return
     */
    public GOTermAnnotatedGenes getAnnotatedGenes(TermID goTermID)
    {
        if (this.map.containsKey(goTermID)) {
            return this.map.get(goTermID);
        } else {
            return new GOTermAnnotatedGenes();
        }
    }

    /**
     * @author Sebastian Bauer
     */
    public class GOTermOftenAnnotatedCount implements Comparable<GOTermOftenAnnotatedCount>
    {
        public TermID term;

        public int counts;

        @Override
        public int compareTo(GOTermOftenAnnotatedCount o)
        {
            /* We sort reversly */
            return o.counts - this.counts;
        }
    };

    /**
     * Returns the terms which shares genes with the given term and neither a ascendant nor descendant.
     *
     * @param goTermID
     * @return
     */
    public GOTermOftenAnnotatedCount[] getTermsOftenAnnotatedWithAndNotOnPath(TermID goTermID)
    {
        ArrayList<GOTermOftenAnnotatedCount> list = new ArrayList<GOTermOftenAnnotatedCount>();

        GOTermAnnotatedGenes goTermIDAnnotated = this.map.get(goTermID);
        if (goTermIDAnnotated == null) {
            return null;
        }

        /* For every term genes are annotated to */
        for (TermID curTerm : this.map.keySet()) {
            /* Ignore terms on the same path */
            if (this.graph.isRootTerm(curTerm)) {
                continue;
            }
            if (curTerm.equals(goTermID)) {
                continue;
            }
            if (this.graph.existsPath(curTerm, goTermID) || this.graph.existsPath(goTermID, curTerm)) {
                continue;
            }

            /* Find out the number of genes which are annotated to both terms */
            int count = 0;
            GOTermAnnotatedGenes curTermAnnotated = this.map.get(curTerm);
            for (ByteString gene : curTermAnnotated.totalAnnotated) {
                if (goTermIDAnnotated.totalAnnotated.contains(gene)) {
                    count++;
                }
            }

            if (count != 0) {
                GOTermOftenAnnotatedCount tc = new GOTermOftenAnnotatedCount();
                tc.term = curTerm;
                tc.counts = count;
                list.add(tc);
            }
        }

        GOTermOftenAnnotatedCount[] termArray = new GOTermOftenAnnotatedCount[list.size()];
        list.toArray(termArray);
        Arrays.sort(termArray);

        return termArray;
    }

    @Override
    public Iterator<TermID> iterator()
    {
        return this.map.keySet().iterator();
    }

    /**
     * Returns the total number of terms to which at least a single gene has been annotated.
     *
     * @return
     */
    public int getTotalNumberOfAnnotatedTerms()
    {
        return this.map.size();
    }

    /**
     * Returns the currently annotated terms as a set.
     *
     * @return
     */
    public Set<TermID> getAllAnnotatedTermsAsSet()
    {
        LinkedHashSet<TermID> at = new LinkedHashSet<TermID>();
        for (TermID t : this) {
            at.add(t);
        }
        return at;
    }

    /**
     * Returns the currently annotated terms as a list.
     *
     * @return
     */
    public List<TermID> getAllAnnotatedTermsAsList()
    {
        ArrayList<TermID> at = new ArrayList<TermID>();
        for (TermID t : this) {
            at.add(t);
        }
        return at;
    }

    /**
     * Returns all genes contained within this set.
     *
     * @return
     */
    public Set<ByteString> getGenes()
    {
        LinkedHashSet<ByteString> genes = new LinkedHashSet<ByteString>();

        for (Entry<TermID, GOTermAnnotatedGenes> ent : this.map.entrySet()) {
            genes.addAll(ent.getValue().totalAnnotated);
        }

        return genes;
    }

    /** Callback to decide whether an term should be removed */
    public static interface IRemover
    {
        /**
         * Returns whether the given term should be removed.
         *
         * @param tag
         * @return
         */
        public boolean remove(TermID tid, GOTermAnnotatedGenes tag);
    }

    /**
     * Removes existing terms from the enumerator according to the remover.
     *
     * @param remove
     */
    public void removeTerms(IRemover remove)
    {
        ArrayList<TermID> toBeRemoved = new ArrayList<TermID>();
        for (Entry<TermID, GOTermAnnotatedGenes> entry : this.map.entrySet()) {
            if (remove.remove(entry.getKey(), entry.getValue())) {
                toBeRemoved.add(entry.getKey());
            }
        }
        for (TermID tid : toBeRemoved) {
            this.map.remove(tid);
        }
    }
}
