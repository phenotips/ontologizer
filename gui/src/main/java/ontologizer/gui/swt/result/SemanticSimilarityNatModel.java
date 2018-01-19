package ontologizer.gui.swt.result;

import net.sourceforge.nattable.config.DefaultBodyConfig;
import net.sourceforge.nattable.config.DefaultColumnHeaderConfig;
import net.sourceforge.nattable.config.DefaultRowHeaderConfig;
import net.sourceforge.nattable.config.SizeConfig;
import net.sourceforge.nattable.data.IColumnHeaderLabelProvider;
import net.sourceforge.nattable.data.IDataProvider;
import net.sourceforge.nattable.model.DefaultNatTableModel;
import net.sourceforge.nattable.renderer.DefaultRowHeaderRenderer;
import ontologizer.types.ByteString;

public class SemanticSimilarityNatModel extends DefaultNatTableModel
{
    private double[][] values;

    private ByteString[] names;

    public SemanticSimilarityNatModel()
    {
        IDataProvider dataProvider = new IDataProvider()
        {
            @Override
            public int getColumnCount()
            {
                if (SemanticSimilarityNatModel.this.values == null) {
                    return 0;
                }
                return SemanticSimilarityNatModel.this.values[0].length;
            };

            @Override
            public int getRowCount()
            {
                if (SemanticSimilarityNatModel.this.values == null) {
                    return 0;
                }
                return SemanticSimilarityNatModel.this.values.length;
            };

            @Override
            public Object getValue(int x, int y)
            {
                return String.format("%g", SemanticSimilarityNatModel.this.values[x][y]);
            }
        };
        DefaultBodyConfig dbc = new DefaultBodyConfig(dataProvider);
        SizeConfig sc = new SizeConfig();
        sc.setDefaultSize(75);
        sc.setDefaultResizable(true);
        dbc.setColumnWidthConfig(sc);
        setBodyConfig(dbc);

        DefaultRowHeaderConfig rowHeaderConfig = new DefaultRowHeaderConfig();
        rowHeaderConfig.setRowHeaderColumnCount(1);
        rowHeaderConfig.setCellRenderer(new DefaultRowHeaderRenderer()
        {
            @Override
            public String getDisplayText(int row, int col)
            {
                return SemanticSimilarityNatModel.this.names[row].toString();
            }
        });
        setRowHeaderConfig(rowHeaderConfig);

        DefaultColumnHeaderConfig columnHeaderConfig = new DefaultColumnHeaderConfig(new IColumnHeaderLabelProvider()
        {
            @Override
            public String getColumnHeaderLabel(int col)
            {
                if (SemanticSimilarityNatModel.this.names == null) {
                    return "";
                }
                return SemanticSimilarityNatModel.this.names[col].toString();
            }
        });
        setColumnHeaderConfig(columnHeaderConfig);
    }

    public void setValues(double[][] values)
    {
        this.values = values;
    }

    public void setNames(ByteString[] names)
    {
        this.names = names;
    }

    public double getValue(int x, int y)
    {
        if (x < 0 || y < 0) {
            return Double.NaN;
        }
        return this.values[x][y];
    }
}
