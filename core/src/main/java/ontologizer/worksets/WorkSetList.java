/*
 * Created on 11.11.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.worksets;

import java.util.ArrayList;
import java.util.Iterator;

public class WorkSetList implements Iterable<WorkSet>
{
    private ArrayList<WorkSet> wsl = new ArrayList<WorkSet>();

    @Override
    public Iterator<WorkSet> iterator()
    {
        return this.wsl.iterator();
    }

    public void add(String name, String obo, String association)
    {
        WorkSet ws = new WorkSet(name);
        ws.setOboPath(obo);
        ws.setAssociationPath(association);
        this.wsl.add(ws);
    }

    public void add(WorkSet ws)
    {
        this.wsl.add(ws);
    }

    public WorkSet get(String currentWorkSet)
    {
        for (WorkSet ws : this.wsl) {
            if (ws.getName().equalsIgnoreCase(currentWorkSet)) {
                return ws;
            }
        }
        return null;
    }

    public void clear()
    {
        this.wsl.clear();
    }
}
