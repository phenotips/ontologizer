package ontologizer.sampling;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParserTest2;
import ontologizer.set.StudySet;
import ontologizer.set.StudySetFactory;
import ontologizer.types.ByteString;

public class StudySetSamplerTest
{
    private StudySet baseStudySet;

    private StudySetSampler studySetSampler;

    private int baseStudySetsize;

    @Before
    public void setUp() throws Exception
    {
        AssociationParserTest2 assocPT = new AssociationParserTest2();
        assocPT.setUp();
        // container = assocPT.container;
        AssociationContainer assocContainer = assocPT.assocContainer;

        Set<ByteString> allAnnotatedGenes = assocContainer.getAllAnnotatedGenes();

        String[] allAnnotatedGenesArray = new String[allAnnotatedGenes.size()];
        int i = 0;
        for (ByteString gene : allAnnotatedGenes) {
            allAnnotatedGenesArray[i++] = gene.toString();
        }

        this.baseStudySet = StudySetFactory.createFromArray(allAnnotatedGenesArray, false);
        this.baseStudySet.setName("baseStudy");
        this.baseStudySetsize = this.baseStudySet.getGeneCount();
        this.studySetSampler = new StudySetSampler(this.baseStudySet);
    }

    @Test
    public void testBasicSampling()
    {
        StudySet sample;
        int ss;

        ss = 10;
        sample = this.studySetSampler.sampleRandomStudySet(ss);
        Assert.assertTrue(sample.getGeneCount() == ss);

        ss = 0;
        sample = this.studySetSampler.sampleRandomStudySet(ss);
        Assert.assertTrue(sample.getGeneCount() == ss);

        sample = this.studySetSampler.sampleRandomStudySet(this.baseStudySetsize);
        Assert.assertTrue(sample.getGeneCount() == this.baseStudySetsize);

        sample = this.studySetSampler.sampleRandomStudySet(this.baseStudySetsize + 1);
        Assert.assertTrue(sample.getGeneCount() == this.baseStudySetsize);
        Assert.assertEquals(sample.getAllGeneNames(), this.baseStudySet.getAllGeneNames());
    }

}
