package ontologizer;

import java.util.ArrayList;
import java.util.Iterator;

import ontologizer.calculation.EnrichedGOTermsResult;

public class StudySetResultList implements Iterable<EnrichedGOTermsResult>
{
    private String name = new String();

    private ArrayList<EnrichedGOTermsResult> list = new ArrayList<EnrichedGOTermsResult>();

    public void addStudySetResult(EnrichedGOTermsResult studySetRes)
    {
        this.list.add(studySetRes);
    }

    public ArrayList<EnrichedGOTermsResult> getStudySetResults()
    {
        return this.list;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public Iterator<EnrichedGOTermsResult> iterator()
    {
        return this.list.iterator();
    }

    public int size()
    {
        return this.list.size();
    }
}
