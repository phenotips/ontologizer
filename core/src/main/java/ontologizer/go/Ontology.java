package ontologizer.go;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import sonumina.math.graph.AbstractGraph.INeighbourGrabber;
import sonumina.math.graph.AbstractGraph.IVisitor;
import sonumina.math.graph.DirectedGraph;
import sonumina.math.graph.DirectedGraph.IDistanceVisitor;
import sonumina.math.graph.Edge;
import sonumina.math.graph.SlimDirectedGraphView;

/**
 * An edge in the go graph
 *
 * @author sba
 */
class OntologyEdge extends Edge<Term>
{
    /** Relation always to the parent (source) */
    private TermRelation relation;

    public void setRelation(TermRelation relation)
    {
        this.relation = relation;
    }

    public TermRelation getRelation()
    {
        return this.relation;
    }

    public OntologyEdge(Term source, Term dest, TermRelation relation)
    {
        super(source, dest);

        this.relation = relation;
    }
}

/**
 * Represents the whole ontology.
 *
 * @author Sebastian Bauer
 */
public class Ontology implements Iterable<Term>
{
    private static Logger logger = Logger.getLogger(Ontology.class.getCanonicalName());

    /** This is used to identify Gene Ontology until a better way is found */
    private static HashSet<String> goLevel1TermNames = new HashSet<String>(Arrays.asList("molecular_function",
        "biological_process", "cellular_component"));

    /** The graph */
    private DirectedGraph<Term> graph;

    /** We also pack a TermContainer */
    private TermContainer termContainer;

    /** The (possibly) artificial root term */
    private Term rootTerm;

    /** Level 1 terms */
    private List<Term> level1terms = new ArrayList<Term>();

    /** Available subsets */
    private HashSet<Subset> availableSubsets = new HashSet<Subset>();

    /**
     * Terms often have alternative IDs (mostly from term merges). This map is used by
     * getTermIncludingAlternatives(String termIdString) and initialized there lazily.
     */
    private HashMap<String, String> alternativeId2primaryId;

    /**
     * Construct the GO Graph from the given container.
     *
     * @param termContainer
     */
    public Ontology(TermContainer newTermContainer)
    {
        this.termContainer = newTermContainer;

        this.graph = new DirectedGraph<Term>();

        /* At first add all goterms to the graph */
        for (Term term : newTermContainer) {
            this.graph.addVertex(term);
        }

        int skippedEdges = 0;

        /* Now add the edges, i.e. link the terms */
        for (Term term : newTermContainer)
        {
            if (term.getSubsets() != null) {
                for (Subset s : term.getSubsets()) {
                    this.availableSubsets.add(s);
                }
            }

            for (ParentTermID parent : term.getParents())
            {
                /* Ignore loops */
                if (term.getID().equals(parent.termid))
                {
                    logger.info("Detected self-loop in the definition of the ontology (term " + term.getIDAsString()
                        + "). This link has been ignored.");
                    continue;
                }
                if (newTermContainer.get(parent.termid) == null)
                {
                    /* FIXME: We may want to add a new vertex to graph here instead */
                    logger.info("Could not add a link from term " + term.toString() + " to " + parent.termid.toString()
                        + " as the latter's definition is missing.");
                    ++skippedEdges;
                    continue;
                }
                this.graph.addEdge(new OntologyEdge(newTermContainer.get(parent.termid), term, parent.relation));
            }
        }

        if (skippedEdges > 0) {
            logger.info("A total of " + skippedEdges + " edges were skipped.");
        }
        assignLevel1TermsAndFixRoot();
    }

    /**
     * Returns the induced subgraph which contains the terms with the given ids.
     *
     * @param termIDs
     * @return
     */
    public Ontology getInducedGraph(Collection<TermID> termIDs)
    {
        Ontology subgraph = new Ontology();
        HashSet<Term> allTerms = new HashSet<Term>();

        for (TermID tid : termIDs) {
            for (TermID tid2 : getTermsOfInducedGraph(null, tid)) {
                allTerms.add(getTerm(tid2));
            }
        }

        subgraph.availableSubsets = this.availableSubsets;
        subgraph.graph = this.graph.subGraph(allTerms);
        subgraph.termContainer = this.termContainer;
        subgraph.availableSubsets = this.availableSubsets;

        subgraph.assignLevel1TermsAndFixRoot();

        return subgraph;
    }

    /**
     * Returns terms that have no descendants.
     *
     * @return
     */
    public ArrayList<Term> getLeafTerms()
    {
        ArrayList<Term> leafTerms = new ArrayList<Term>();
        for (Term t : this.graph.getVertices())
        {
            if (this.graph.getOutDegree(t) == 0) {
                leafTerms.add(t);
            }
        }

        return leafTerms;
    }

    /**
     * Returns term id of terms that have no descendants.
     *
     * @return
     */
    public Collection<TermID> getLeafTermIDs()
    {
        ArrayList<TermID> leafTerms = new ArrayList<TermID>();
        for (Term t : this.graph.getVertices())
        {
            if (this.graph.getOutDegree(t) == 0) {
                leafTerms.add(t.getID());
            }
        }

        return leafTerms;
    }

    /**
     * Returns the term in topological order.
     *
     * @return
     */
    public ArrayList<Term> getTermsInTopologicalOrder()
    {
        return this.graph.topologicalOrder();
    }

    /**
     * Returns a slim representation of the ontology.
     *
     * @return
     */
    public SlimDirectedGraphView<Term> getSlimGraphView()
    {
        return new SlimDirectedGraphView<Term>(this.graph);
    }

    /**
     * Finds about level 1 terms and fix the root as we assume here that there is only a single root.
     */
    private void assignLevel1TermsAndFixRoot()
    {
        this.level1terms = new ArrayList<Term>();

        /* Find the terms without any ancestors */
        for (Term goTerm : this.graph)
        {
            if (this.graph.getInDegree(goTerm) == 0 && !goTerm.isObsolete()) {
                this.level1terms.add(goTerm);
            }
        }

        if (this.level1terms.size() > 1)
        {
            StringBuilder level1StringBuilder = new StringBuilder();
            level1StringBuilder.append("\"");
            level1StringBuilder.append(this.level1terms.get(0).getName());
            level1StringBuilder.append("\"");
            for (int i = 1; i < this.level1terms.size(); i++)
            {
                level1StringBuilder.append(" ,\"");
                level1StringBuilder.append(this.level1terms.get(i).getName());
                level1StringBuilder.append("\"");
            }

            String rootName = "root";
            if (this.level1terms.size() == 3)
            {
                boolean isGO = false;
                for (Term t : this.level1terms)
                {
                    if (goLevel1TermNames.contains(t.getName().toLowerCase())) {
                        isGO = true;
                    } else
                    {
                        isGO = false;
                        break;
                    }
                }
                if (isGO) {
                    rootName = "Gene Ontology";
                }
            }

            this.rootTerm = new Term(this.level1terms.get(0).getID().getPrefix().toString() + ":0000000", rootName);

            logger.info("Ontology contains multiple level-one terms: " + level1StringBuilder.toString()
                + ". Adding artificial root term \"" + this.rootTerm.getID().toString() + "\".");

            this.rootTerm.setSubsets(new ArrayList<Subset>(this.availableSubsets));
            this.graph.addVertex(this.rootTerm);

            for (Term lvl1 : this.level1terms)
            {
                this.graph.addEdge(new OntologyEdge(this.rootTerm, lvl1, TermRelation.UNKOWN));
            }
        } else
        {
            if (this.level1terms.size() == 1)
            {
                this.rootTerm = this.level1terms.get(0);
                logger.info("Ontology contains a single level-one term (" + this.rootTerm.toString() + "");
            }
        }
    }

    private Ontology()
    {
    }

    /**
     * Determines whether the given id is the id of the (possible artifactial) root term
     *
     * @return The root vertex as a GOVertex object
     */
    public boolean isRootTerm(TermID id)
    {
        return id.equals(this.rootTerm.getID());
    }

    /**
     * Get (possibly artificial) TermID of the root vertex of graph
     *
     * @return The term representing to root
     */
    public Term getRootTerm()
    {
        return this.rootTerm;
    }

    /**
     * Returns all available subsets.
     *
     * @return
     */
    public Collection<Subset> getAvailableSubsets()
    {
        return this.availableSubsets;
    }

    /**
     * Return the set of term IDs containing the given term's descendants.
     *
     * @param termID as string
     * @return the set of term ids of children as a set of strings
     */
    public Set<String> getTermChildrenAsStrings(String termID)
    {
        Term goTerm;
        if (termID.equals(this.rootTerm.getIDAsString())) {
            goTerm = this.rootTerm;
        } else {
            goTerm = this.termContainer.get(termID);
        }

        HashSet<String> terms = new HashSet<String>();
        Iterator<Edge<Term>> edgeIter = this.graph.getOutEdges(goTerm);
        while (edgeIter.hasNext()) {
            terms.add(edgeIter.next().getDest().getIDAsString());
        }
        return terms;
    }

    /**
     * Return the set of term IDs containing the given term's ancestors.
     *
     * @param termID - the id as a string
     * @return the set of term ids of parents as a set of strings.
     */
    public Set<String> getTermParentsAsStrings(String termID)
    {
        Term goTerm;
        if (termID.equals(this.rootTerm.getIDAsString())) {
            goTerm = this.rootTerm;
        } else {
            goTerm = this.termContainer.get(termID);
        }

        HashSet<String> terms = new HashSet<String>();

        Iterator<Edge<Term>> edgeIter = this.graph.getInEdges(goTerm);
        while (edgeIter.hasNext()) {
            terms.add(edgeIter.next().getSource().getIDAsString());
        }
        return terms;
    }

    /**
     * Return the set of term IDs containing the given term's children.
     *
     * @param term - the term's id as a TermID
     * @return the set of termID of the descendants as term-IDs
     */
    public Set<TermID> getTermChildren(TermID termID)
    {
        Term goTerm;
        if (this.rootTerm.getID().id == termID.id) {
            goTerm = this.rootTerm;
        } else {
            goTerm = this.termContainer.get(termID);
        }

        HashSet<TermID> terms = new HashSet<TermID>();
        Iterator<Edge<Term>> edgeIter = this.graph.getOutEdges(goTerm);
        while (edgeIter.hasNext()) {
            terms.add(edgeIter.next().getDest().getID());
        }
        return terms;
    }

    /**
     * Return the set of terms containing the given term's children.
     *
     * @param term - the term for which the children should be returned
     * @return the set of terms of the descendants as terms
     */
    public Set<Term> getTermChildren(Term term)
    {
        Term goTerm;
        if (this.rootTerm.getID().id == term.getID().id) {
            goTerm = this.rootTerm;
        } else {
            goTerm = this.termContainer.get(term.getID());
        }

        HashSet<Term> terms = new HashSet<Term>();
        Iterator<Edge<Term>> edgeIter = this.graph.getOutEdges(goTerm);
        while (edgeIter.hasNext()) {
            terms.add(edgeIter.next().getDest());
        }
        return terms;
    }

    /**
     * Return the set of term IDs containing the given term-ID's ancestors.
     *
     * @param Term-ID
     * @return the set of Term-IDs of ancestors
     */
    public Set<TermID> getTermParents(TermID goTermID)
    {
        HashSet<TermID> terms = new HashSet<TermID>();
        if (this.rootTerm.getID().id == goTermID.id) {
            return terms;
        }

        Term goTerm;
        if (goTermID.equals(this.rootTerm.getIDAsString())) {
            goTerm = this.rootTerm;
        } else {
            goTerm = this.termContainer.get(goTermID);
        }

        Iterator<Edge<Term>> edgeIter = this.graph.getInEdges(goTerm);
        while (edgeIter.hasNext()) {
            terms.add(edgeIter.next().getSource().getID());
        }
        return terms;
    }

    /**
     * Return the set of terms that are parents of the given term.
     *
     * @param term
     * @return the set of Terms of parents
     */
    public Set<Term> getTermParents(Term term)
    {
        HashSet<Term> terms = new HashSet<Term>();
        if (this.rootTerm.getID().id == term.getID().id) {
            return terms;
        }

        Term goTerm;
        if (term.getID().equals(this.rootTerm.getIDAsString())) {
            goTerm = this.rootTerm;
        } else {
            goTerm = this.termContainer.get(term.getID());
        }

        Iterator<Edge<Term>> edgeIter = this.graph.getInEdges(goTerm);
        while (edgeIter.hasNext()) {
            terms.add(edgeIter.next().getSource());
        }
        return terms;
    }

    /**
     * Return the set of GO term IDs containing the given GO term's ancestors. Includes the type of relationship.
     *
     * @param destID
     * @return
     */
    public Set<ParentTermID> getTermParentsWithRelation(TermID goTermID)
    {
        HashSet<ParentTermID> terms = new HashSet<ParentTermID>();
        if (this.rootTerm.getID().id == goTermID.id) {
            return terms;
        }

        Term goTerm;
        if (goTermID.equals(this.rootTerm.getIDAsString())) {
            goTerm = this.rootTerm;
        } else {
            goTerm = this.termContainer.get(goTermID);
        }

        Iterator<Edge<Term>> edgeIter = this.graph.getInEdges(goTerm);
        while (edgeIter.hasNext())
        {
            OntologyEdge t = (OntologyEdge) edgeIter.next();
            terms.add(new ParentTermID(t.getSource().getID(), t.getRelation()));
        }

        return terms;
    }

    /**
     * Get the relation that relates term to ancestor or null.
     *
     * @param ancestor
     * @param term
     * @return
     */
    public TermRelation getDirectRelation(TermID ancestor, TermID term)
    {
        Set<ParentTermID> parents = getTermParentsWithRelation(term);
        for (ParentTermID p : parents) {
            if (p.termid.equals(ancestor)) {
                return p.relation;
            }
        }
        return null;
    }

    /**
     * Returns the siblings of the term, i.e., terms that are also children of the parents.
     *
     * @param tid
     * @return
     */
    public Set<TermID> getTermsSiblings(TermID tid)
    {
        Set<TermID> parentTerms = getTermParents(tid);
        HashSet<TermID> siblings = new HashSet<TermID>();
        for (TermID p : parentTerms) {
            siblings.addAll(getTermChildren(p));
        }
        siblings.remove(tid);
        return siblings;
    }

    /**
     * Determines if there exists a directed path from sourceID to destID on the GO Graph.
     *
     * @param sourceID
     * @param destID
     */
    public boolean existsPath(TermID sourceID, TermID destID)
    {
        /* Some special cases because of the artificial root */
        if (isRootTerm(destID))
        {
            if (isRootTerm(sourceID)) {
                return true;
            }
            return false;
        }

        /*
         * We walk from the destination to the source against the graph direction. Basically a breadth-depth search is
         * done.
         */

        /* TODO: Make this a method of DirectedGraph (already implemented there) */
        Term source = this.termContainer.get(sourceID);
        Term dest = this.termContainer.get(destID);

        HashSet<Term> visited = new HashSet<Term>();

        LinkedList<Term> queue = new LinkedList<Term>();
        queue.offer(dest);
        visited.add(dest);

        while (!queue.isEmpty())
        {
            /* Remove head of the queue */
            Term head = queue.poll();

            /*
             * Add not yet visited neighbours of old head to the queue and mark them as visited. If such a node is the
             * source, return true (because than there exists a directed path between source and destination)
             */
            Iterator<Edge<Term>> edgeIter = this.graph.getInEdges(head);
            while (edgeIter.hasNext())
            {
                Edge<Term> edge = edgeIter.next();
                Term ancestor = edge.getSource();

                if (ancestor == source) {
                    return true;
                }

                if (!visited.contains(ancestor))
                {
                    visited.add(ancestor);
                    queue.offer(ancestor);
                }
            }
        }
        return false;
    }

    /**
     * This interface is used as a callback mechanisim by the walkToSource() and walkToSinks() methods.
     *
     * @author Sebastian Bauer
     */
    public interface IVisitingGOVertex extends IVisitor<Term>
    {
    };

    /**
     * Starting at the vertex representing goTermID walk to the source of the DAG (ontology vertex) and call the method
     * visiting of given object implementimg IVisitingGOVertex.
     *
     * @param goTermID the TermID to start with (note that visiting() is also called for this vertex)
     * @param vistingVertex
     */
    public void walkToSource(TermID goTermID, IVisitingGOVertex vistingVertex)
    {
        ArrayList<TermID> set = new ArrayList<TermID>(1);
        set.add(goTermID);
        walkToSource(set, vistingVertex);
    }

    /**
     * Convert a collection of termids to a list of terms.
     *
     * @param termIDSet
     * @return
     */
    private ArrayList<Term> termIDsToTerms(Collection<TermID> termIDSet)
    {
        ArrayList<Term> termList = new ArrayList<Term>(termIDSet.size());
        for (TermID id : termIDSet)
        {
            Term t;

            if (isRootTerm(id)) {
                t = this.rootTerm;
            } else {
                t = this.termContainer.get(id);
            }
            if (t == null) {
                throw new IllegalArgumentException("\"" + id + "\" could not be mapped to a known term!");
            }

            termList.add(t);
        }
        return termList;
    }

    /**
     * Starting at the vertices within the goTermIDSet walk to the source of the DAG (ontology vertex) and call the
     * method visiting of given object Implementing IVisitingGOVertex.
     *
     * @param termIDSet the set of go TermsIDs to start with (note that visiting() is also called for those
     *            vertices/terms)
     * @param vistingVertex
     */
    public void walkToSource(Collection<TermID> termIDSet, IVisitingGOVertex vistingVertex)
    {
        this.graph.bfs(termIDsToTerms(termIDSet), true, vistingVertex);
    }

    /**
     * Starting at the vertices within the goTermIDSet walk to the source of the DAG (ontology vertex) and call the
     * method visiting of given object Implementing IVisitingGOVertex. Only relations in relationsToFollow are
     * considered.
     *
     * @param termIDSet
     * @param vistingVertex
     * @param relationsToFollow
     */
    public void walkToSource(Collection<TermID> termIDSet, IVisitingGOVertex vistingVertex,
        final Set<TermRelation> relationsToFollow)
    {
        this.graph.bfs(termIDsToTerms(termIDSet), new INeighbourGrabber<Term>()
            {
            @Override
            public Iterator<Term> grabNeighbours(Term t)
            {
                Iterator<Edge<Term>> inIter = Ontology.this.graph.getInEdges(t);
                ArrayList<Term> termsToConsider = new ArrayList<Term>();
                while (inIter.hasNext())
                {
                    OntologyEdge edge = (OntologyEdge) inIter.next(); /* Ugly cast */
                    if (relationsToFollow.contains(edge.getRelation())) {
                        termsToConsider.add(edge.getSource());
                    }
                }
                return termsToConsider.iterator();
            }
            }, vistingVertex);
    }

    /**
     * Starting at the vertices within the goTermIDSet walk to the sinks of the DAG and call the method visiting of
     * given object implementing IVisitingGOVertex.
     *
     * @param goTermID the TermID to start with (note that visiting() is also called for this vertex)
     * @param vistingVertex
     */

    public void walkToSinks(TermID goTermID, IVisitingGOVertex vistingVertex)
    {
        ArrayList<TermID> set = new ArrayList<TermID>(1);
        set.add(goTermID);
        walkToSinks(set, vistingVertex);
    }

    /**
     * Starting at the vertices within the goTermIDSet walk to the sinks of the DAG and call the method visiting of
     * given object implementing IVisitingGOVertex.
     *
     * @param goTermIDSet the set of go TermsIDs to start with (note that visiting() is also called for those
     *            vertices/terms)
     * @param vistingVertex
     */
    public void walkToSinks(Collection<TermID> goTermIDSet, IVisitingGOVertex vistingVertex)
    {
        this.graph.bfs(termIDsToTerms(goTermIDSet), false, vistingVertex);
    }

    /**
     * Returns the term container attached to this ontology graph. Note that the term container usually contains all
     * terms while the graph object may contain a subset.
     *
     * @return
     */
    public TermContainer getTermContainer()
    {
        return this.termContainer;
    }

    /**
     * Returns the term to a given term string or null.
     *
     * @param term
     * @return
     */
    public Term getTerm(String term)
    {
        Term go = this.termContainer.get(term);
        if (go == null)
        {
            /*
             * GO Term Container doesn't include the root term so we have to handle this case for our own.
             */
            try
            {
                TermID id = new TermID(term);
                if (id.id == this.rootTerm.getID().id) {
                    return this.rootTerm;
                }
            } catch (IllegalArgumentException iea)
            {
            }
        }
        /*
         * In order to avoid the returning of terms that are only in the TermContainer but not in the graph we check
         * here that the term is contained in the graph.
         */
        if (!this.graph.containsVertex(go)) {
            return null;
        }

        return go;
    }

    /**
     * A method to get a term using the term-ID as string. If no term with the given primary ID is found all alternative
     * IDs are used. If still no term is found null is returned.
     *
     * @param term ID as string
     * @return
     */
    public Term getTermIncludingAlternatives(String termIdString)
    {

        // try using the primary id
        Term term = getTerm(termIdString);
        if (term != null) {
            return term;
        }

        /*
         * no term with this primary id exists -> use alternative ids
         */

        // do we already have a mapping between alternative ids and primary ids ?
        if (this.alternativeId2primaryId == null) {
            setUpMappingAlternativeId2PrimaryId();
        }

        // try to find a mapping to a primary term-id
        if (this.alternativeId2primaryId.containsKey(termIdString)) {
            String primaryId = this.alternativeId2primaryId.get(termIdString);
            term = this.termContainer.get(primaryId);
        }

        // term still null?
        if (term == null)
        {
            /*
             * GO Term Container doesn't include the root term so we have to handle this case for our own.
             */
            try
            {
                TermID id = new TermID(termIdString);
                if (id.id == this.rootTerm.getID().id) {
                    return this.rootTerm;
                }
            } catch (IllegalArgumentException iea)
            {
            }
        }
        return term;
    }

    private void setUpMappingAlternativeId2PrimaryId()
    {
        this.alternativeId2primaryId = new HashMap<String, String>();
        for (Term t : this.termContainer) {
            String primaryId = t.getIDAsString();
            for (TermID alternativeTermId : t.getAlternatives()) {
                this.alternativeId2primaryId.put(alternativeTermId.toString(), primaryId);
            }
        }

    }

    /**
     * Returns the full-fledged term.
     *
     * @param id
     * @return
     */
    public Term getTerm(TermID id)
    {
        Term go = this.termContainer.get(id);
        if (go == null && id.id == this.rootTerm.getID().id) {
            return this.rootTerm;
        }
        return go;
    }

    /**
     * Returns whether the given term is included in the graph.
     *
     * @param term
     * @return
     */
    public boolean termExists(TermID term)
    {
        return this.graph.getOutDegree(getTerm(term)) != -1;
    }

    /**
     * Returns the set of terms given from the set of term ids.
     *
     * @param termIDs
     * @return
     */
    public Set<Term> getSetOfTermsFromSetOfTermIds(Set<TermID> termIDs)
    {
        HashSet<Term> termSet = new HashSet<Term>();
        for (TermID tid : termIDs) {
            termSet.add(getTerm(tid));
        }
        return termSet;
    }

    /**
     * Returns a set of induced terms that are the terms of the induced graph. Providing null as root-term-ID will
     * induce all terms up to the root to be included.
     *
     * @param rootTerm the root term (all terms up to this are included). if you provide null all terms up to the
     *            original root term are included.
     * @param term the inducing term.
     * @return
     */
    public Set<TermID> getTermsOfInducedGraph(final TermID rootTermID, TermID termID)
    {
        HashSet<TermID> nodeSet = new HashSet<TermID>();

        /**
         * Visitor which simply add all nodes to the nodeSet.
         *
         * @author Sebastian Bauer
         */
        class Visitor implements IVisitingGOVertex
        {
            public Ontology graph;

            public HashSet<TermID> nodeSet;

            @Override
            public boolean visited(Term term)
            {
                if (rootTermID != null && !this.graph.isRootTerm(rootTermID))
                {
                    /*
                     * Only add the term if there exists a path from the requested root term to the visited term. TODO:
                     * Instead of existsPath() implement walkToGoTerm() to speed up the whole stuff
                     */
                    if (term.getID().equals(rootTermID) || this.graph.existsPath(rootTermID, term.getID())) {
                        this.nodeSet.add(term.getID());
                    }
                } else {
                    this.nodeSet.add(term.getID());
                }

                return true;
            }
        }
        ;

        Visitor visitor = new Visitor();
        visitor.nodeSet = nodeSet;
        visitor.graph = this;

        walkToSource(termID, visitor);

        return nodeSet;
    }

    /**
     * Returns all level 1 terms.
     *
     * @return
     */
    public Collection<Term> getLevel1Terms()
    {
        return this.level1terms;
    }

    /**
     * Returns the parents shared by both t1 and t2.
     *
     * @param t1
     * @param t2
     * @return
     */
    public Collection<TermID> getSharedParents(TermID t1, TermID t2)
    {
        final Set<TermID> p1 = getTermsOfInducedGraph(null, t1);

        final ArrayList<TermID> sharedParents = new ArrayList<TermID>();

        walkToSource(t2, new IVisitingGOVertex()
        {
            @Override
            public boolean visited(Term t2)
            {
                if (p1.contains(t2.getID())) {
                    sharedParents.add(t2.getID());
                }
                return true;
            }
        });

        /* The unoptimized algorithm */
        if (false)
        {
            Set<TermID> p2 = getTermsOfInducedGraph(null, t2);
            p1.retainAll(p2);
            return p1;
        }

        return sharedParents;
    }

    /**
     * Determines all tupels of terms which all are unrelated, meaning that the terms are not allowed to be in the same
     * lineage.
     *
     * @param baseTerms
     * @param tupelSize
     * @return
     */
    public ArrayList<HashSet<TermID>> getUnrelatedTermTupels(
        HashSet<TermID> baseTerms, int tupelSize)
        {
        ArrayList<HashSet<TermID>> unrelatedTupels = new ArrayList<HashSet<TermID>>();

        // TODO: Not sure what to implement here...

        return unrelatedTupels;
        }

    static public class GOLevels
    {
        private HashMap<Integer, HashSet<TermID>> level2terms = new HashMap<Integer, HashSet<TermID>>();

        private HashMap<TermID, Integer> terms2level = new HashMap<TermID, Integer>();

        private int maxLevel = -1;

        public void putLevel(TermID tid, int distance)
        {
            HashSet<TermID> levelTerms = this.level2terms.get(distance);
            if (levelTerms == null)
            {
                levelTerms = new HashSet<TermID>();
                this.level2terms.put(distance, levelTerms);
            }
            levelTerms.add(tid);
            this.terms2level.put(tid, distance);

            if (distance > this.maxLevel) {
                this.maxLevel = distance;
            }
        }

        /**
         * Returns the level of the given term.
         *
         * @param tid
         * @return the level or -1 if the term is not included.
         */
        public int getTermLevel(TermID tid)
        {
            Integer level = this.terms2level.get(tid);
            if (level == null) {
                return -1;
            }
            return level;
        }

        public Set<TermID> getLevelTermSet(int level)
        {
            return this.level2terms.get(level);
        }

        public int getMaxLevel()
        {
            return this.maxLevel;
        }
    };

    /**
     * Returns the levels of the given terms starting from the root. Considers only the relevant terms.
     *
     * @param termids
     * @return
     */
    public GOLevels getGOLevels(final Set<TermID> termids)
    {
        DirectedGraph<Term> transGraph;
        Term transRoot;

        if ((getRelevantSubontology() != null && !isRootTerm(getRelevantSubontology())) || getRelevantSubset() != null)
        {
            Ontology ontologyTransGraph = getOntlogyOfRelevantTerms();
            transGraph = ontologyTransGraph.graph;
            transRoot = ontologyTransGraph.getRootTerm();
        } else
        {
            transGraph = this.graph;
            transRoot = this.rootTerm;
        }

        final GOLevels levels = new GOLevels();

        transGraph.singleSourceLongestPath(transRoot, new IDistanceVisitor<Term>()
            {
            @Override
            public boolean visit(Term vertex, List<Term> path,
                int distance)
            {
                if (termids.contains(vertex.getID())) {
                    levels.putLevel(vertex.getID(), distance);
                }
                return true;
            }
            });
        return levels;
    }

    /**
     * Returns the number of terms in this ontology
     *
     * @return the number of terms.
     */
    public int getNumberOfTerms()
    {
        return this.graph.getNumberOfVertices();
    }

    /**
     * Returns the highest term id used in this ontology.
     *
     * @return
     */
    public int maximumTermID()
    {
        int id = 0;

        for (Term t : this.termContainer)
        {
            if (t.getID().id > id) {
                id = t.getID().id;
            }
        }

        return id;
    }

    /**
     * Returns an iterator to iterate over all terms
     */
    @Override
    public Iterator<Term> iterator()
    {
        return this.graph.getVertexIterator();
    }

    private Subset relevantSubset;

    private Term relevantSubontology;

    /**
     * Sets the relevant subset.
     *
     * @param subsetName
     */
    public void setRelevantSubset(String subsetName)
    {
        System.out.println(subsetName);

        for (Subset s : this.availableSubsets)
        {
            if (s.getName().equals(subsetName))
            {
                this.relevantSubset = s;
                return;
            }
        }

        this.relevantSubset = null;
        throw new IllegalArgumentException("Subset \"" + subsetName + "\" couldn't be found!");
    }

    /**
     * Returns the current relevant subject.
     *
     * @return
     */
    public Subset getRelevantSubset()
    {
        return this.relevantSubset;
    }

    /**
     * Sets the relevant subontology.
     *
     * @param subontologyName
     */
    public void setRelevantSubontology(String subontologyName)
    {
        /* FIXME: That's so slow */
        for (Term t : this.termContainer)
        {
            if (t.getName().equals(subontologyName))
            {
                this.relevantSubontology = t;
                return;
            }
        }
        throw new IllegalArgumentException("Subontology \"" + subontologyName + "\" couldn't be found!");
    }

    /**
     * Gets the relevant subontology.
     *
     * @return
     */
    public TermID getRelevantSubontology()
    {
        if (this.relevantSubontology != null) {
            return this.relevantSubontology.getID();
        }
        return this.rootTerm.getID();
    }

    /**
     * Returns whether the given term is relevant (i.e., is contained in a relevant sub ontology and subset).
     *
     * @param term
     * @return
     */
    public boolean isRelevantTerm(Term term)
    {
        if (this.relevantSubset != null)
        {
            boolean found = false;
            for (Subset s : term.getSubsets())
            {
                if (s.equals(this.relevantSubset))
                {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        if (this.relevantSubontology != null)
        {
            if (term.getID().id != this.relevantSubontology.getID().id) {
                if (!(existsPath(this.relevantSubontology.getID(), term.getID()))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns whether the given term is relevant (i.e., is contained in a relevant sub ontology and subset).
     *
     * @param goTermID
     * @return
     */
    public boolean isRelevantTermID(TermID goTermID)
    {
        Term t;
        if (isRootTerm(goTermID)) {
            t = this.rootTerm;
        } else {
            t = this.termContainer.get(goTermID);
        }

        return isRelevantTerm(t);
    }

    /**
     * Returns a redundant relation to this term.
     *
     * @param t
     * @return null, if there is no redundant relation
     */
    public TermID findARedundantISARelation(Term t)
    {
        /*
         * We implement a naive algorithm which results straight-forward from the definition: A relation is redundant if
         * it can be removed without having a effect on the reachability of the nodes.
         */
        Set<TermID> parents = getTermParents(t.getID());

        Set<TermID> allInducedTerms = getTermsOfInducedGraph(null, t.getID());

        for (TermID p : parents)
        {
            HashSet<TermID> thisInduced = new HashSet<TermID>();

            for (TermID p2 : parents)
            {
                /* Leave out the current parent */
                if (p.equals(p2)) {
                    continue;
                }

                thisInduced.addAll(getTermsOfInducedGraph(null, p2));
            }

            if (thisInduced.size() == allInducedTerms.size() - 1) {
                return p;
            }

        }

        return null;
    }

    /**
     * Returns redundant is a relations.
     */
    public void findRedundantISARelations()
    {
        for (Term t : this)
        {
            TermID redundant = findARedundantISARelation(t);
            if (redundant != null)
            {
                System.out.println(t.getName() + " (" + t.getIDAsString() + ") -> " + getTerm(redundant).getName()
                    + "(" + redundant.toString() + ")");
            }
        }
    }

    /**
     * Returns the graph of relevant terms.
     *
     * @return
     */
    public Ontology getOntlogyOfRelevantTerms()
    {
        HashSet<Term> terms = new HashSet<Term>();
        for (Term t : this) {
            if (isRelevantTerm(t)) {
                terms.add(t);
            }
        }

        DirectedGraph<Term> trans = this.graph.pathMaintainingSubGraph(terms);

        Ontology g = new Ontology();
        g.graph = trans;
        g.termContainer = this.termContainer;
        g.assignLevel1TermsAndFixRoot();

        /* TODO: Add real GOEdges */

        return g;
    }

    public DirectedGraph<Term> getGraph()
    {
        return this.graph;
    }

    /**
     * Merges equivalent terms. The first term given to this method will be the representative of this
     * "equivalence-cluster".
     *
     * @param t1
     * @param eqTerms
     */
    public void mergeTerms(Term t1, HashSet<Term> eqTerms)
    {

        HashSet<TermID> t1ExistingAlternatives = new HashSet<TermID>(t1.getAlternatives());
        for (Term t : eqTerms) {
            TermID tId = t.getID();

            if (t1ExistingAlternatives.contains(tId)) {
                continue;
            }

            t1.addAlternativeId(tId);
        }

        this.graph.mergeVertices(t1, eqTerms);

    }

}
