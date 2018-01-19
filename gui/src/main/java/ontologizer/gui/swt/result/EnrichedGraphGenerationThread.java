package ontologizer.gui.swt.result;

import java.io.File;
import java.util.HashSet;

import org.eclipse.swt.widgets.Display;

import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.gui.swt.support.IGraphGenerationFinished;
import ontologizer.gui.swt.support.IGraphGenerationSupport;
import ontologizer.gui.swt.support.NewGraphGenerationThread;

/**
 * Generates the graph by executing DOT. When finished the finished method of the specified constructor argument is
 * executed in the context of the GUI thread.
 *
 * @author Sebastian Bauer
 */
public class EnrichedGraphGenerationThread extends NewGraphGenerationThread
{
    public Ontology go;

    public Term emanatingTerm;

    public double significanceLevel;

    public HashSet<TermID> leafTerms = new HashSet<>();

    public EnrichedGOTermsResult result;

    private IGraphGenerationFinished finished;

    private IGraphGenerationSupport support = new IGraphGenerationSupport()
    {
        @Override
        public void writeDOT(File dotFile)
        {
            EnrichedGraphGenerationThread.this.result.writeDOT(EnrichedGraphGenerationThread.this.go, dotFile,
                EnrichedGraphGenerationThread.this.significanceLevel, true,
                EnrichedGraphGenerationThread.this.emanatingTerm != null
                    ? EnrichedGraphGenerationThread.this.emanatingTerm.getID() : null,
                EnrichedGraphGenerationThread.this.leafTerms);
        }

        @Override
        public void layoutFinished(boolean success, String msg, File pngFile,
            File dotFile)
        {
            EnrichedGraphGenerationThread.this.finished.finished(success, msg, pngFile, dotFile);
        }
    };

    public EnrichedGraphGenerationThread(Display display, String dotCMDPath, IGraphGenerationFinished finished)
    {
        super(display, dotCMDPath);

        setSupport(this.support);

        this.finished = finished;
    }
};
