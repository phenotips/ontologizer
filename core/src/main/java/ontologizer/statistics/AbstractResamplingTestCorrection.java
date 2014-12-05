package ontologizer.statistics;

public abstract class AbstractResamplingTestCorrection extends AbstractTestCorrection
implements IResampling
{
    /** Specifies the number of resampling steps */
    protected int numberOfResamplingSteps = 500;

    /** Used for progress update */
    private IResamplingProgress progress;

    /**
     * Set the number of resampling steps.
     */
    @Override
    public void setNumberOfResamplingSteps(int n)
    {
        this.numberOfResamplingSteps = n;
    }

    /**
     * Returns the current number of resampling steps.
     */
    @Override
    public int getNumberOfResamplingSteps()
    {
        return this.numberOfResamplingSteps;
    }

    /**
     * Sets the progress update instance used for progress notifications.
     *
     * @param newProgress
     */
    public void setProgressUpdate(IResamplingProgress newProgress)
    {
        this.progress = newProgress;
    }

    /**
     * Used for sub classes.
     *
     * @param max
     */
    protected void initProgress(int max)
    {
        if (this.progress != null) {
            this.progress.init(max);
        }
    }

    protected void updateProgress(int c)
    {
        if (this.progress != null) {
            this.progress.update(c);
        }
    }
}
