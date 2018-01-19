/*
 * Created on 18.03.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt.result;

import org.eclipse.swt.widgets.Composite;

import ontologizer.calculation.svd.SVDGOTermProperties;

public class PValuesSVDGOTermsComposite extends SVDGOTermsComposite
{

    public PValuesSVDGOTermsComposite(Composite parent, int style)
    {
        super(parent, style);
    }

    @Override
    protected String getOrginalDataString(SVDGOTermProperties prop, int i)
    {
        return String.format("%.3g", prop.pVals[i]);
    }

    @Override
    public String getTitle()
    {
        return "PVal PCA";
    }

}
