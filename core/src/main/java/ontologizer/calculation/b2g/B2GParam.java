package ontologizer.calculation.b2g;

/**
 * A basic class to represent different settings parameter.
 *
 * @author sba
 */
abstract public class B2GParam
{
    static public enum Type
    {
        FIXED,
        EM,
        MCMC
    }

    private Type type;

    B2GParam(Type type)
    {
        this.type = type;
    }

    B2GParam(B2GParam p)
    {
        this.type = p.type;
    }

    public Type getType()
    {
        return this.type;
    }

    public boolean isFixed()
    {
        return this.type == Type.FIXED;
    }

    public boolean isMCMC()
    {
        return this.type == Type.MCMC;
    }

    public boolean isEM()
    {
        return this.type == Type.EM;
    }

    public void setType(Type type)
    {
        this.type = type;
    }
}
