/*
 * Created on 01.11.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import ontologizer.FileCache;
import ontologizer.FileCache.FileState;
import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.association.Gene2Associations;
import ontologizer.calculation.CalculationRegistry;
import ontologizer.go.Ontology;
import ontologizer.go.Subset;
import ontologizer.go.Term;
import ontologizer.gui.swt.images.Images;
import ontologizer.gui.swt.support.SWTUtil;
import ontologizer.statistics.TestCorrectionRegistry;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetList;
import ontologizer.worksets.WorkSetLoadThread;

class Settings
{
    public String ontologyFileName;

    public String annotationsFileName;

    public String mappingFileName;

    public String subset;

    public String subontology;

    public boolean isClosed;

    public Properties getSettingsAsProperty()
    {
        Properties prop = new Properties();
        prop.setProperty("annotationsFileName", this.annotationsFileName);
        prop.setProperty("ontologyFileName", this.ontologyFileName);
        prop.setProperty("mappingFileName", this.mappingFileName);
        prop.setProperty("isClosed", Boolean.toString(this.isClosed));
        prop.setProperty("subontology", this.subontology);
        prop.setProperty("subset", this.subset);
        return prop;
    }
};

class TreeItemData
{
    public boolean isPopulation;

    public boolean isProjectFolder;

    public boolean isSettings;

    public String filename;

    public String entries;

    public int numEntries;

    public int numKnownEntries = -1;

    public File projectDirectory;

    public Settings settings;
};

/**
 * Ontologizer's main window. TODO: Separate out the data.
 *
 * @author Sebastian Bauer
 */
public class MainWindow extends ApplicationWindow
{
    /** The logger */
    private static Logger logger = Logger.getLogger(MainWindow.class.getCanonicalName());

    public static final String PROJECT_SETTINGS_NAME = ".project";

    /* Manually added attributes */
    private File workspaceDirectory;

    private TreeItem currentSelectedItem = null;

    private String currentImportFileName = null;

    private String currentExportFileName = null;

    private TreeItem treeItemWhenWorkSetIsChanged;

    private Menu menuBar = null;

    private Menu submenu = null;

    private Composite composite = null;

    private SashForm sashForm = null;

    private TreeEditor workspaceTreeEditor = null;

    private Tree workspaceTree = null;

    private Composite rightComposite = null;

    private Composite leftComposite = null;

    private GeneEditor setTextArea = null;

    private Menu submenu1 = null;

    private ProjectSettingsComposite settingsComposite;

    private Text statusText = null;

    private ProgressBar statusProgressBar = null;

    private StackLayout rightStackedLayout;

    /* ToolBar */
    private ToolBar toolbar = null;

    private Combo methodCombo = null;

    private Combo mtcCombo = null;

    private ToolItem removeToolItem = null;

    private ToolItem analyzeToolItem = null;

    private ToolItem newProjectToolItem = null;

    private ToolItem similarityToolItem = null;

    /** Action to be called when a new method is selected */
    private List<ISimpleAction> methodAction = new LinkedList<>();

    /* Menu Items */
    private MenuItem preferencesMenuItem;

    private MenuItem helpContentsMenuItem;

    private MenuItem workSetsMenuItem;

    private MenuItem fileCacheMenutItem;

    private MenuItem helpAboutMenuItem;

    private MenuItem exportMenuItem;

    private MenuItem logMenuItem;

    private MenuItem newProjectItem;

    private MenuItem newPopulationItem;

    private MenuItem newStudyItem;

    /* String constants */
    private final String methodToolTip = "Specifies the calculation method which is used to get the raw p-value.";

    private final String mtcToolTip =
        "Specifies the multiple test correction procedure which is used to adjust the p-value.";

    private final String populationTip =
        "The population set. It consists of all instances that\ncan be selected in an experiment. ";

    private final String studyTip =
        "The study set. It consits of instances that have\nbeen selected due to an experiment. Genes that are\nnot included in the population set are added during\nthe calculation.";

    /* Manually added methods */

    /**
     * Sets the workspace to the given file.
     *
     * @param newWorkspaceDirectory must point to a directory.
     */
    public void setWorkspace(File newWorkspaceDirectory)
    {
        if (!newWorkspaceDirectory.isDirectory()) {
            throw new IllegalArgumentException();
        }

        this.workspaceDirectory = newWorkspaceDirectory;

        String[] projects = newWorkspaceDirectory.list(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                if (name.startsWith(".")) {
                    return false;
                }
                return true;
            }
        });
        Arrays.sort(projects, String.CASE_INSENSITIVE_ORDER);

        for (String project : projects) {
            if (project.equals(".cache")) {
                continue;
            }

            addProject(new File(newWorkspaceDirectory, project));
        }
    }

    /**
     * Adds a new project.
     *
     * @param projectDirectory
     */
    public void addProject(File projectDirectory)
    {
        String[] names = projectDirectory.list();

        if (names == null) {
            logger.warning("Listing the contents of " + projectDirectory.getPath() + " failed");
            return;
        }

        TreeItem projectTreeItem = newProjectItem(projectDirectory, projectDirectory.getName());

        for (String name : names) {
            if (name.equalsIgnoreCase("Population")) {
                newPopItem(projectTreeItem, name);
                continue;
            }

            if (name.equals(PROJECT_SETTINGS_NAME)) {
                Properties prop = new Properties();
                try {
                    FileInputStream fis = new FileInputStream(new File(projectDirectory, PROJECT_SETTINGS_NAME));
                    prop.loadFromXML(fis);
                    fis.close();
                    TreeItemData tid = getTreeItemData(projectTreeItem);
                    tid.settings.annotationsFileName =
                        prop.getProperty("annotationsFileName", getAssociationsFileString());
                    tid.settings.ontologyFileName = prop.getProperty("ontologyFileName", getDefinitionFileString());
                    tid.settings.mappingFileName = prop.getProperty("mappingFileName", getMappingFileString());
                    tid.settings.subontology = prop.getProperty("subontology", getSubontologyString());
                    tid.settings.subset = prop.getProperty("subset", getSubsetString());
                    tid.settings.isClosed = Boolean.parseBoolean(prop.getProperty("isClosed", "false"));
                } catch (InvalidPropertiesFormatException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                continue;
            }

            newStudyItem(projectTreeItem, name);
        }

        TreeItemData tid = getTreeItemData(projectTreeItem);
        projectTreeItem.setExpanded(!tid.settings.isClosed);
    }

    /**
     * Create a new project item using the given name.
     *
     * @param directory
     * @param name
     * @return
     */
    private TreeItem newProjectItem(File directory, String name)
    {
        TreeItemData newItemData = new TreeItemData();
        newItemData.isProjectFolder = true;
        newItemData.projectDirectory = directory;
        newItemData.settings = new Settings();
        newItemData.settings.annotationsFileName = getAssociationsFileString();
        newItemData.settings.ontologyFileName = getDefinitionFileString();
        newItemData.settings.subontology = getSubontologyString();
        newItemData.settings.subset = getSubsetString();

        TreeItem newItem = new TreeItem(this.workspaceTree, 0);
        newItem.setData(newItemData);
        updateTextOfItem(newItem);
        return newItem;
    }

    /**
     * Create a new popitem using the given name and reads in the file given by name.
     *
     * @param directory
     * @param name
     * @return
     */
    private TreeItem newPopItem(TreeItem parent, String name)
    {
        if (getPopulationItem(parent) != null) {
            return null;
        }

        File directory = getTreeItemData(parent).projectDirectory;
        TreeItemData newItemData = new TreeItemData();
        newItemData.isPopulation = true;
        newItemData.projectDirectory = directory;
        newItemData.filename = name;
        File f = new File(directory, name);
        try {
            BufferedReader is;
            String line;
            StringBuilder str = new StringBuilder();

            is = new BufferedReader(new FileReader(f));
            while ((line = is.readLine()) != null) {
                str.append(line);
                str.append("\n");
                newItemData.numEntries++;
            }
            is.close();

            newItemData.entries = str.toString();
        } catch (IOException e) {
        }

        TreeItem newItem = new TreeItem(parent, 0);
        newItem.setData(newItemData);
        updateTextOfItem(newItem);
        return newItem;
    }

    /**
     * Create a new study set item using the given name. If the given name exits within in the directory, it is read in.
     *
     * @param parent the parent, i.e. where this study set belongs to.
     * @param name the name of the study set (equals the filename)
     * @return
     */
    private TreeItem newStudyItem(TreeItem parent, String name)
    {
        File directory = getTreeItemData(parent).projectDirectory;
        TreeItemData newItemData = new TreeItemData();
        newItemData.isPopulation = false;
        newItemData.projectDirectory = directory;
        newItemData.filename = name;
        File f = new File(directory, name);
        try {
            BufferedReader is;
            String line;
            StringBuilder str = new StringBuilder();

            is = new BufferedReader(new FileReader(f));
            while ((line = is.readLine()) != null) {
                str.append(line);
                str.append("\n");
                newItemData.numEntries++;
            }
            is.close();

            newItemData.entries = str.toString();
        } catch (IOException e) {
        }

        TreeItem newItem = new TreeItem(parent, 0);
        newItem.setData(newItemData);
        updateTextOfItem(newItem);
        return newItem;
    }

    /**
     * Updates the text of the tree item.
     *
     * @param item
     */
    private void updateTextOfItem(TreeItem item)
    {
        TreeItemData tid = getTreeItemData(item);
        if (isTreeItemProject(item)) {
            item.setText(tid.projectDirectory.getName());
        } else {
            /* Workaround to ensure that the item is really redrawn */
            item.setText(tid.filename + " ");
            item.setText(tid.filename);
        }
    }

    /**
     * Tries to rename the given item.
     *
     * @param item
     * @param name
     * @return
     */
    private boolean renameItem(TreeItem item, String name)
    {
        TreeItemData tid = getTreeItemData(item);

        if (isTreeItemProject(item)) {
            File dest = new File(tid.projectDirectory.getParentFile(), name);
            if (dest.exists()) {
                return false;
            }

            if (!tid.projectDirectory.renameTo(dest)) {
                return false;
            }

            tid.projectDirectory = dest;

            TreeItem[] children = item.getItems();
            for (TreeItem element : children) {
                TreeItemData tidChild = getTreeItemData(element);
                tidChild.projectDirectory = dest;
            }

        } else {
            File src = new File(tid.projectDirectory, tid.filename);
            File dest = new File(tid.projectDirectory, name);

            if (dest.exists()) {
                return false;
            }

            if (!src.renameTo(dest)) {
                return false;
            }

            if (tid.filename.equalsIgnoreCase("Population") && !name.equalsIgnoreCase("Population")) {
                tid.isPopulation = false;
            } else {
                if (!tid.filename.equalsIgnoreCase("Population") && name.equalsIgnoreCase("Population")) {
                    tid.isPopulation = true;
                }
            }

            tid.filename = name;
        }
        updateTextOfItem(item);
        return true;
    }

    /**
     * Removes the given tree item and the associated files and directories
     *
     * @param item the item which is going to be removed.
     */
    private boolean removeItem(TreeItem item)
    {
        TreeItemData tid = getTreeItemData(item);
        if (isTreeItemProject(item)) {
            TreeItem[] items = item.getItems();
            for (TreeItem i : items) {
                if (!removeItem(i)) {
                    return false;
                }
            }

            File f = new File(tid.projectDirectory, PROJECT_SETTINGS_NAME);
            if (f.exists()) {
                f.delete();
            }

            if (!(tid.projectDirectory.delete())) {
                return false;
            }
        } else {
            File f = new File(tid.projectDirectory, tid.filename);
            if (!f.delete()) {
                return false;
            }
        }
        item.dispose();
        return true;
    }

    /**
     * Returns a list of the names of all projects.
     *
     * @return
     */
    public List<String> getProjectNames()
    {
        List<String> l = new LinkedList<>();
        TreeItem[] tis = this.workspaceTree.getItems();
        for (TreeItem ti : tis) {
            TreeItemData tid = getTreeItemData(ti);
            l.add(tid.projectDirectory.getName());
        }
        return l;
    }

    /**
     * Stores the current genes (context of the setTextArea) within the current selected StudySet
     */
    protected void storeGenes()
    {
        if (this.currentSelectedItem != null) {
            TreeItemData tid;

            tid = getTreeItemData(this.currentSelectedItem);
            if (tid.isProjectFolder) {
                tid.settings.annotationsFileName = getAssociationsFileString();
                tid.settings.ontologyFileName = getDefinitionFileString();
                tid.settings.mappingFileName = getMappingFileString();
                tid.settings.isClosed = !this.currentSelectedItem.getExpanded();
                tid.settings.subontology = getSubontologyString();
                tid.settings.subset = getSubsetString();
                storeProjectSettings(tid);
                return;
            }

            tid.entries = this.setTextArea.getText();
            tid.numEntries = this.setTextArea.getNumberOfEntries();
            tid.numKnownEntries = this.setTextArea.getNumberOfKnownEntries();
            updateTextOfItem(this.currentSelectedItem);

            try {
                /* TODO: Add an explicit saving mechanism */
                File out = new File(tid.projectDirectory, tid.filename);
                BufferedWriter fw = new BufferedWriter(new FileWriter(out));
                fw.write(tid.entries);
                fw.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Stores the project settings of item represented by the TreeItemData.
     *
     * @param tid
     */
    private void storeProjectSettings(TreeItemData tid)
    {
        if (!tid.isProjectFolder) {
            return;
        }

        Properties prop = tid.settings.getSettingsAsProperty();
        try {
            FileOutputStream fos = new FileOutputStream(new File(tid.projectDirectory, PROJECT_SETTINGS_NAME));
            prop.storeToXML(fos, "Ontologizer Project File");
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the project item of the given item. If item is the project item, item is returned.
     *
     * @param item
     * @return
     */
    private TreeItem getProjectItem(TreeItem item)
    {
        TreeItem parent = item;
        if (!getTreeItemData(parent).isProjectFolder) {
            parent = parent.getParentItem();
        }
        return parent;
    }

    /**
     * Returns the population item.
     *
     * @param item
     * @return
     */
    private TreeItem getPopulationItem(TreeItem item)
    {
        TreeItem[] items = item.getItems();
        for (TreeItem item2 : items) {
            if (getTreeItemData(item2).isPopulation) {
                return item2;
            }
        }
        return null;
    }

    private static WorkSet currentWorkSet;

    /**
     * Update the setTextArea to the genes of the current selected set.
     */
    private void updateGenes()
    {
        TreeItem[] items = this.workspaceTree.getSelection();
        if (items != null && items.length > 0) {
            this.currentSelectedItem = items[0];

            TreeItem projectItem = getProjectItem(this.currentSelectedItem);
            if (projectItem != null) {
                Settings settings = getTreeItemData(projectItem).settings;

                setAssociationsFileString(settings.annotationsFileName);
                setDefinitonFileString(settings.ontologyFileName);
                setMappingFileString(settings.mappingFileName);
                setSubontology(settings.subontology);
                setSubset(settings.subset);

                final String subontology = settings.subontology;
                final String subset = settings.subset;

                if (currentWorkSet != null) {
                    WorkSetLoadThread.releaseDatafiles(currentWorkSet);
                }
                currentWorkSet = this.settingsComposite.getSelectedWorkset();

                this.settingsComposite.setRestrictionChoices(null);
                this.settingsComposite.setConsiderChoices(null);

                StringBuilder info = new StringBuilder();

                FileState fs = FileCache.getState(currentWorkSet.getOboPath());
                if (fs == FileState.CACHED) {
                    info.append("Remote definition file was downloaded at "
                        + FileCache.getDownloadTime(currentWorkSet.getOboPath()) + ". ");
                }
                fs = FileCache.getState(currentWorkSet.getAssociationPath());
                if (fs == FileState.CACHED) {
                    info.append("Remote annotation file was downloaded at "
                        + FileCache.getDownloadTime(currentWorkSet.getAssociationPath()) + ". ");
                }

                this.settingsComposite.setInfoText(info.toString());

                WorkSetLoadThread.obtainDatafiles(currentWorkSet,
                    new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            MainWindow.this.settingsComposite.getDisplay().asyncExec(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Ontology graph = WorkSetLoadThread.getGraph(currentWorkSet.getOboPath());
                                    AssociationContainer assoc =
                                        WorkSetLoadThread.getAssociations(currentWorkSet.getAssociationPath());

                                    if (graph != null) {
                                        String[] subsetChoices = new String[graph.getAvailableSubsets().size()];
                                        int i = 0;
                                        for (Subset s : graph.getAvailableSubsets()) {
                                            subsetChoices[i++] = s.getName();
                                        }
                                        MainWindow.this.settingsComposite.setRestrictionChoices(subsetChoices);
                                        MainWindow.this.settingsComposite.setRestriction(subset);

                                        String[] subontologyChoices = new String[graph.getLevel1Terms().size()];
                                        i = 0;
                                        for (Term t : graph.getLevel1Terms()) {
                                            subontologyChoices[i++] = t.getName();
                                        }
                                        MainWindow.this.settingsComposite.setConsiderChoices(subontologyChoices);
                                        MainWindow.this.settingsComposite.setConsider(subontology);
                                        MainWindow.this.settingsComposite.setOntologyErrorString(null);
                                    } else {
                                        MainWindow.this.settingsComposite.setRestrictionChoices(new String[] {});
                                        MainWindow.this.settingsComposite.setConsiderChoices(new String[] {});
                                        MainWindow.this.settingsComposite
                                            .setOntologyErrorString("Error in obtaining the definition file.");
                                    }

                                    if (assoc == null) {
                                        MainWindow.this.settingsComposite
                                            .setAssociationErrorString("Error in obtaining the association file.");
                                    } else {
                                        HashSet<String> evidences = new HashSet<>();
                                        for (Gene2Associations g2a : assoc) {
                                            for (Association a : g2a) {
                                                evidences.add(a.getEvidence().toString());
                                            }
                                        }

                                        MainWindow.this.settingsComposite.setEvidences(evidences);
                                        MainWindow.this.settingsComposite.setAssociationErrorString(null);
                                    }
                                }
                            });
                        }
                    });
            }

            if (isTreeItemProject(this.currentSelectedItem)) {
                if (this.rightStackedLayout.topControl != this.settingsComposite) {
                    this.rightStackedLayout.topControl = this.settingsComposite;
                    this.settingsComposite.getParent().layout();
                }
            } else {
                String genes = getTreeItemData(this.currentSelectedItem).entries;
                if (genes == null) {
                    genes = "";
                }

                if (getTreeItemData(this.currentSelectedItem).isPopulation) {
                    this.setTextArea.setToolTipText(this.populationTip);
                } else {
                    this.setTextArea.setToolTipText(this.studyTip);
                }

                this.setTextArea.setText(genes);
                this.treeItemWhenWorkSetIsChanged = this.currentSelectedItem;
                this.setTextArea.setWorkSet(this.settingsComposite.getSelectedWorkset(),
                    this.settingsComposite.getMappingFileString());
                if (this.rightStackedLayout.topControl != this.rightComposite) {
                    this.rightStackedLayout.topControl = this.rightComposite;
                    this.rightComposite.getParent().layout();
                }
            }

            this.removeToolItem.setEnabled(true);
        } else {
            this.currentSelectedItem = null;
            this.setTextArea.setText("");
            this.setTextArea.setToolTipText(null);

            this.removeToolItem.setEnabled(false);
        }
    }

    private String[] split(String str)
    {
        String[] strs = str.split("\n");
        for (int i = 0; i < strs.length; i++) {
            strs[i] = strs[i].trim();
        }
        return strs;
    }

    /**
     * Returns the entries of the current selected population/study set.
     *
     * @return
     */
    public String[] getCurrentSetEntries()
    {
        return split(this.setTextArea.getText());
    }

    /**
     * Returns the definition file string.
     *
     * @return
     */
    public String getDefinitionFileString()
    {
        return this.settingsComposite.getDefinitionFileString();
    }

    /**
     * Sets the definition file string.
     *
     * @param string
     */
    public void setDefinitonFileString(String string)
    {
        this.settingsComposite.setDefinitonFileString(string);
    }

    /**
     * Retunrs the mapping file string.
     *
     * @return
     */
    public String getMappingFileString()
    {
        return this.settingsComposite.getMappingFileString();
    }

    /**
     * Sets the mapping file string.
     *
     * @param string
     */
    public void setMappingFileString(String string)
    {
        this.settingsComposite.setMappingFileString(string);
    }

    /**
     * Returns the association file string.
     *
     * @return
     */
    public String getAssociationsFileString()
    {
        return this.settingsComposite.getAssociationsFileString();
    }

    public void setAssociationsFileString(String string)
    {
        this.settingsComposite.setAssociationsFileString(string);
    }

    public class Set
    {
        public String[] entries;

        public String name;
    };

    /**
     * Get all the set entries of the current selected population. This includes the population which is always the
     * first entry accessible via the iterator. If no population was given the set is empty.
     *
     * @return might return null if no set is selected
     */
    public List<Set> getSetEntriesOfCurrentPopulation()
    {
        /* Store current genes */
        storeGenes();

        if (this.currentSelectedItem != null) {
            LinkedList<Set> list = new LinkedList<>();

            /* Find proper population and add to the list */
            TreeItem project = getProjectItem(this.currentSelectedItem);
            TreeItemData projectData = getTreeItemData(project);
            TreeItem pop = getPopulationItem(project);

            if (pop != null) {
                String entries = getTreeItemData(pop).entries;
                if (entries == null) {
                    entries = "";
                }
                Set set = new Set();
                set.name = pop.getText();
                set.entries = split(entries);
                list.add(set);
            } else {
                /* Empty population */
                Set set = new Set();
                set.name = projectData.projectDirectory.getName();
                set.entries = new String[0];
                list.add(set);
            }

            TreeItem children[] = project.getItems();
            for (TreeItem element : children) {
                if (getTreeItemData(element).isPopulation) {
                    continue;
                }

                String entries = getTreeItemData(element).entries;
                if (entries == null) {
                    entries = "";
                }
                Set set = new Set();
                set.name = element.getText();
                set.entries = split(entries);
                list.add(set);
            }

            return list;
        }
        return null;
    }

    /**
     * Returns the tree item data of the given tree item.
     *
     * @param ti
     * @return
     */
    static private TreeItemData getTreeItemData(TreeItem ti)
    {
        return (TreeItemData) ti.getData();
    }

    /**
     * Return whether the given tree item is a population item.
     *
     * @param pop
     * @return
     */
    static private boolean isTreeItemPopulation(TreeItem pop)
    {
        return getTreeItemData(pop).isPopulation;
    }

    /**
     * Return whether the given tree item is a project item.
     *
     * @param project
     * @return
     */
    static private boolean isTreeItemProject(TreeItem project)
    {
        return getTreeItemData(project).isProjectFolder;
    }

    /**
     * Add a new action which is executed when the "Ontologize" button is pressed.
     *
     * @param ba
     */
    public void addAnalyseAction(final ISimpleAction ba)
    {
        this.analyzeToolItem.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter()
        {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
            {
                ba.act();
            }
        });
    }

    /**
     * Add a new action which is executed when the "Similarity" button is pressed.
     *
     * @param ba
     */
    public void addSimilarityAction(final ISimpleAction ba)
    {
        this.similarityToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                ba.act();
            }
        });
    }

    /**
     * Add a new action which is executed when the new project menu item is pressed.
     *
     * @param a
     */
    public void addNewProjectAction(ISimpleAction a)
    {
        addSimpleSelectionAction(this.newProjectToolItem, a);
        addSimpleSelectionAction(this.newProjectItem, a);
    }

    /**
     * Add a new action which is executed on the window's disposal.
     *
     * @param a the action
     */
    public void addDisposeAction(final ISimpleAction a)
    {
        this.shell.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                a.act();
            }
        });
    }

    /**
     * Add a new action which is executed on selecting the preferences window menu item.
     *
     * @param a
     */
    public void addOpenPreferencesAction(final ISimpleAction a)
    {
        this.preferencesMenuItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                a.act();
            }
        });
    }

    /**
     * Add a new action which is executed on selecting the workset window menu item.
     *
     * @param a
     */
    public void addOpenFileCacheAction(final ISimpleAction a)
    {
        this.fileCacheMenutItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                a.act();
            }
        });
    }

    /**
     * Add a new action which is executed on selecting the workset window menu item.
     *
     * @param a
     */
    public void addOpenWorkSetAction(final ISimpleAction a)
    {
        this.workSetsMenuItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                a.act();
            }
        });
    }

    /**
     * Add a new action which is executed on selecting the log window menu item.
     *
     * @param a
     */
    public void addOpenLogWindowAction(final ISimpleAction a)
    {
        this.logMenuItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                a.act();
            }
        });
    }

    /**
     * Add a new action which is executed on selecting the help contents menu item.
     *
     * @param a
     */
    public void addOpenHelpContentsAction(final ISimpleAction a)
    {
        this.helpContentsMenuItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                a.act();
            }
        });
    }

    /**
     * Add a new action which is executed on selecting the about menu item.
     *
     * @param a
     */
    public void addOpenAboutAction(final ISimpleAction a)
    {
        this.helpAboutMenuItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                a.act();
            }
        });
    }

    /**
     * Disable the analyse button.
     */
    public void disableAnalyseButton()
    {
        this.analyzeToolItem.setEnabled(false);
    }

    /**
     * Enable the analyse button.
     */
    public void enableAnalyseButton()
    {
        this.analyzeToolItem.setEnabled(true);
    }

    public void appendLog(String str)
    {
        System.out.println(str);
    }

    /**
     * Sets the currently selected method to the method with the given name.
     *
     * @param string
     */
    public void setSelectedMethodName(String string)
    {
        String[] items = this.methodCombo.getItems();
        for (String item : items) {
            if (item.equalsIgnoreCase(string)) {
                this.methodCombo.setText(item);
                break;
            }
        }
    }

    /**
     * Add a new action that is called when a new method is selected.
     *
     * @param action
     */
    public void addMethodAction(ISimpleAction action)
    {
        this.methodAction.add(action);
    }

    /**
     * Returns the name of the currently selected method.
     *
     * @return
     */
    public String getSelectedMethodName()
    {
        return this.methodCombo.getItem(this.methodCombo.getSelectionIndex());
    }

    public String getSelectedMTCName()
    {
        return this.mtcCombo.getItem(this.mtcCombo.getSelectionIndex());
    }

    public void setSelectedMTCName(String string)
    {
        String[] items = this.mtcCombo.getItems();
        for (String item : items) {
            if (item.equalsIgnoreCase(string)) {
                this.mtcCombo.setText(item);
                break;
            }
        }
    }

    /* Generated methods */

    /**
     * This method initializes sShell
     *
     * @param display
     */
    private void createSShell(Display display)
    {
        this.shell.setText("Ontologizer");
        this.shell.setLayout(new FillLayout());
        createComposite();
        this.shell.pack();

        this.menuBar = new Menu(this.shell, SWT.BAR);

        /* Project menu */
        MenuItem submenuItem = new MenuItem(this.menuBar, SWT.CASCADE);
        submenuItem.setText("Project");
        this.submenu = new Menu(submenuItem);
        submenuItem.setMenu(this.submenu);
        MenuItem newMenuItem = new MenuItem(this.submenu, SWT.CASCADE);
        newMenuItem.setText("New");
        Menu newMenu = new Menu(this.shell, SWT.DROP_DOWN);
        newMenuItem.setMenu(newMenu);
        this.newProjectItem = new MenuItem(newMenu, SWT.PUSH);
        this.newProjectItem.setText("Project...");
        this.newProjectItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                storeGenes();
            }
        });
        new MenuItem(newMenu, SWT.SEPARATOR);
        this.newPopulationItem = new MenuItem(newMenu, SWT.PUSH);
        this.newPopulationItem.setText("Population Set");
        this.newPopulationItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                newPopulationAction();
            }
        });
        this.newStudyItem = new MenuItem(newMenu, SWT.PUSH);
        this.newStudyItem.setText("Study Set");
        this.newStudyItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                newStudyAction();
            }
        });

        new MenuItem(this.submenu, SWT.SEPARATOR);
        MenuItem importMenuItem = new MenuItem(this.submenu, SWT.PUSH);
        importMenuItem.setText("Import...");
        this.exportMenuItem = new MenuItem(this.submenu, SWT.PUSH);
        this.exportMenuItem.setText("Export...");
        new MenuItem(this.submenu, SWT.SEPARATOR);
        MenuItem quitMenuItem = new MenuItem(this.submenu, SWT.PUSH);
        quitMenuItem.setText("Quit");

        /* Window sub menu */
        MenuItem windowSubMenuItem = new MenuItem(this.menuBar, SWT.CASCADE);
        windowSubMenuItem.setText("Window");
        Menu windowSubMenu = new Menu(windowSubMenuItem);
        windowSubMenuItem.setMenu(windowSubMenu);
        this.fileCacheMenutItem = new MenuItem(windowSubMenu, SWT.PUSH);
        this.fileCacheMenutItem.setText("File Cache...");
        this.workSetsMenuItem = new MenuItem(windowSubMenu, SWT.PUSH);
        this.workSetsMenuItem.setText("File Sets...");
        this.preferencesMenuItem = new MenuItem(windowSubMenu, SWT.PUSH);
        this.preferencesMenuItem.setText("Preferences...");
        this.logMenuItem = new MenuItem(windowSubMenu, SWT.PUSH);
        this.logMenuItem.setText("Log...");

        /* Help sub menu */
        MenuItem submenuItem1 = new MenuItem(this.menuBar, SWT.CASCADE);
        submenuItem1.setText("Help");
        this.submenu1 = new Menu(submenuItem1);
        submenuItem1.setMenu(this.submenu1);
        this.helpContentsMenuItem = new MenuItem(this.submenu1, SWT.PUSH);
        this.helpContentsMenuItem.setText("Help Contents...");
        new MenuItem(this.submenu1, SWT.SEPARATOR);
        this.helpAboutMenuItem = new MenuItem(this.submenu1, SWT.PUSH);
        this.helpAboutMenuItem.setText("About Ontologizer...");

        /* Listener */
        importMenuItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                FileDialog fileDialog = new FileDialog(MainWindow.this.shell, SWT.OPEN);
                fileDialog.setFilterExtensions(new String[] { "*.onto", "*.*" });
                if (MainWindow.this.currentImportFileName != null) {
                    File f = new File(MainWindow.this.currentImportFileName);
                    fileDialog.setFileName(f.getName());
                    fileDialog.setFilterPath(f.getParent());
                }

                String zipName = fileDialog.open();
                if (zipName != null) {
                    try {
                        ZipFile zipFile = new ZipFile(zipName);
                        Enumeration<? extends ZipEntry> entries = zipFile.entries();

                        boolean overwrite = false;
                        boolean projectExisted = false;

                        String projectName = null;

                        while (entries.hasMoreElements()) {
                            ZipEntry target = entries.nextElement();
                            String targetName = target.getName();
                            File f = new File(MainWindow.this.workspaceDirectory.getAbsolutePath(), target.getName());

                            if (target.isDirectory() && projectName == null) {
                                projectName = target.getName().replace("/", "");
                            }

                            if (f.exists() && !overwrite) {
                                MessageBox mb = new MessageBox(getShell(), SWT.YES | SWT.NO);
                                if (target.isDirectory()) {
                                    projectExisted = true;
                                    mb.setMessage("The project \"" + projectName + "\" already " +
                                        "exists. Do you really wish to import the selected project? Note, that this may overwrite "
                                        +
                                        "existing settings and study sets with the project.");
                                    if (mb.open() == SWT.NO) {
                                        break;
                                    }
                                    overwrite = true;
                                } else {
                                    if (targetName.equals(".settings")) {
                                        mb.setMessage("Do you really which to overwrite the settings of the project \""
                                            + projectName + "\"?");
                                    } else {
                                        mb.setMessage("Do you really which to overwrite study set " + target.getName()
                                            + " of project \"" + projectName + "\"?");
                                    }
                                    if (mb.open() == SWT.NO) {
                                        continue;
                                    }
                                }
                            }
                            saveEntry(zipFile, target, MainWindow.this.workspaceDirectory.getAbsolutePath());
                        }

                        /* Finally, add the new project */
                        if (!projectExisted && projectName != null) {
                            addProject(new File(MainWindow.this.workspaceDirectory, projectName));
                        }
                        MainWindow.this.currentImportFileName = zipName;
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }
        });
        this.exportMenuItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                TreeItem projectItem = getProjectItem(MainWindow.this.currentSelectedItem);
                if (projectItem == null) {
                    return;
                }
                TreeItemData projectItemData = getTreeItemData(projectItem);
                File projectDirectory = projectItemData.projectDirectory;

                FileDialog fileDialog = new FileDialog(MainWindow.this.shell, SWT.SAVE);
                fileDialog.setFilterExtensions(new String[] { "*.onto", "*.*" });
                if (MainWindow.this.currentExportFileName != null) {
                    File f = new File(MainWindow.this.currentExportFileName);
                    fileDialog.setFilterPath(f.getParent());
                }

                fileDialog.setFileName(projectItemData.projectDirectory.getName() + ".onto");

                String newName = fileDialog.open();
                if (newName != null) {
                    try {
                        /* Write out a zip archive containing all the data of the project */
                        String projectName = projectDirectory.getName();
                        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(newName));
                        ZipEntry entry = new ZipEntry(projectName + "/");
                        zip.putNextEntry(entry);
                        zip.closeEntry();

                        byte[] buffer = new byte[4096];

                        String[] names = projectDirectory.list();
                        for (String name : names) {
                            File f = new File(projectDirectory, name);
                            try (FileInputStream in = new FileInputStream(f)) {
                                /* Add zip entry to the output stream */
                                zip.putNextEntry(new ZipEntry(projectName + "/" + name));
                                int len;
                                while ((len = in.read(buffer)) > 0) {
                                    zip.write(buffer, 0, len);
                                }
                                zip.closeEntry();
                            }
                        }

                        zip.close();
                    } catch (Exception e1) {
                        MessageBox mb = new MessageBox(getShell());
                        mb.setMessage(e1.getLocalizedMessage());
                        mb.setText("Error");
                        mb.open();
                    }
                    MainWindow.this.currentExportFileName = newName;
                }
            }
        });
        quitMenuItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                MainWindow.this.shell.dispose();
            }
        });

        this.shell.setMenuBar(this.menuBar);
    }

    /**
     * This method initializes composite
     */
    private void createComposite()
    {
        Composite mainComposite = new Composite(this.shell, SWT.NONE);
        GridLayout gl = SWTUtil.newEmptyMarginGridLayout(1);
        gl.marginTop = 3;
        gl.marginBottom = 3;
        mainComposite.setLayout(gl);

        createToolBar(mainComposite);

        this.composite = new Composite(mainComposite, SWT.NONE);
        gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginTop = 2;

        this.composite.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = this.shell.getDisplay().getClientArea().height / 2;
        this.composite.setLayoutData(gd);
        createSashForm();

        Composite statusComp = new Composite(mainComposite, 0);
        statusComp.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
        statusComp.setLayout(new GridLayout(2, false));

        this.statusText = new Text(statusComp, SWT.READ_ONLY);
        gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        gd.horizontalIndent = 2;
        this.statusText.setLayoutData(gd);
        this.statusText.setBackground(this.shell.getBackground());

        this.statusProgressBar = new ProgressBar(statusComp, 0);
        gd = new GridData();
        gd.widthHint = 150;
        this.statusProgressBar.setLayoutData(gd);
        this.statusProgressBar.setVisible(false);
    }

    /**
     * Constructor.
     *
     * @param display
     */
    public MainWindow(Display display)
    {
        super(display);

        createSShell(display);

        this.workspaceTree.setFocus();

        /* Update states */
        updateGenes();
    }

    public Shell getShell()
    {
        return this.shell;
    }

    /**
     * @param parent
     */
    private void createToolBar(Composite parent)
    {
        this.toolbar = new ToolBar(parent, SWT.FLAT);
        this.newProjectToolItem = new ToolItem(this.toolbar, 0);
        this.newProjectToolItem.setText("New Project");
        this.newProjectToolItem.setImage(Images.loadImage("projects.png"));
        this.newProjectToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                storeGenes();
            }
        });

        ToolItem newPopToolItem = new ToolItem(this.toolbar, 0);
        newPopToolItem.setText("New Population");
        newPopToolItem.setImage(Images.loadImage("newpop.png"));
        newPopToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                newPopulationAction();
            }
        });

        ToolItem newStudyItem = new ToolItem(this.toolbar, 0);
        newStudyItem.setText("New Study");
        newStudyItem.setImage(Images.loadImage("newstudy.png"));
        newStudyItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                newStudyAction();
            }
        });

        this.removeToolItem = new ToolItem(this.toolbar, 0);
        this.removeToolItem.setText("Remove");
        this.removeToolItem.setImage(Images.loadImage("delete_obj.gif"));
        this.removeToolItem.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                TreeItem[] items = MainWindow.this.workspaceTree.getSelection();

                if (items != null && items.length > 0) {
                    removeItem(items[0]);
                }
                updateGenes();
            }
        });

        new ToolItem(this.toolbar, SWT.SEPARATOR);
        this.similarityToolItem = new ToolItem(this.toolbar, 0);
        this.similarityToolItem.setText("Similarity");
        this.similarityToolItem.setImage(Images.loadImage("sim.png"));
        this.similarityToolItem.setToolTipText("Calculates the Semantic Similarity");

        new ToolItem(this.toolbar, SWT.SEPARATOR);

        this.analyzeToolItem = new ToolItem(this.toolbar, 0);
        this.analyzeToolItem.setText("Ontologize");
        this.analyzeToolItem.setImage(Images.loadImage("ontologize.png"));

        new ToolItem(this.toolbar, SWT.SEPARATOR);

        ToolItem methodToolItem = new ToolItem(this.toolbar, SWT.SEPARATOR);
        this.methodCombo = new Combo(this.toolbar, SWT.READ_ONLY);
        this.methodCombo.setItems(CalculationRegistry.getAllRegistered());
        this.methodCombo.setText(CalculationRegistry.getDefault().getName());
        this.methodCombo.setToolTipText(this.methodToolTip);
        this.methodCombo.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                for (ISimpleAction act : MainWindow.this.methodAction) {
                    act.act();
                }
            }
        });
        this.methodCombo.pack();
        methodToolItem.setWidth(this.methodCombo.getSize().x);
        methodToolItem.setControl(this.methodCombo);

        new ToolItem(this.toolbar, SWT.SEPARATOR);

        ToolItem mtcToolItem = new ToolItem(this.toolbar, SWT.SEPARATOR);
        this.mtcCombo = new Combo(this.toolbar, SWT.READ_ONLY);
        this.mtcCombo.setItems(TestCorrectionRegistry.getRegisteredCorrections());
        this.mtcCombo.setText(TestCorrectionRegistry.getDefault().getName());
        this.mtcCombo.setToolTipText(this.mtcToolTip);
        this.mtcCombo.pack();
        mtcToolItem.setWidth(Math.min(this.mtcCombo.getSize().x, 200));
        mtcToolItem.setControl(this.mtcCombo);
    }

    /**
     * This method initializes sashForm.
     */
    private void createSashForm()
    {
        this.sashForm = new SashForm(this.composite, SWT.NONE);
        this.sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
        createLeftComposite();
        createRightGroup();
        this.sashForm.setWeights(new int[] { 2, 3 });
    }

    /**
     * This method initializes right composite which displays the details of the current selection.
     */
    private void createRightGroup()
    {
        final CTabFolder detailsFolder = new CTabFolder(this.sashForm, SWT.BORDER);
        detailsFolder.setSingle(true);
        detailsFolder.setMaximizeVisible(true);
        detailsFolder
            .setSelectionBackground(this.leftComposite.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        detailsFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        /* First details page */
        CTabItem genesCTabItem = new CTabItem(detailsFolder, 0);
        genesCTabItem.setText("Currently Selected Set");
        detailsFolder.setSelection(0);

        Composite stackComposite = new Composite(detailsFolder, SWT.NONE);
        this.rightStackedLayout = new StackLayout();
        stackComposite.setLayout(this.rightStackedLayout);
        genesCTabItem.setControl(stackComposite);

        this.rightComposite = new Composite(stackComposite, SWT.NONE);
        this.rightComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(1));
        this.rightStackedLayout.topControl = this.rightComposite;

        this.setTextArea = new GeneEditor(this.rightComposite, 0);
        this.setTextArea.setLayoutData(new GridData(GridData.FILL_BOTH));
        this.setTextArea.setToolTipText("");
        this.setTextArea.addNewNameListener(new GeneEditor.INewNameListener()
        {
            @Override
            public void newName(String name)
            {
                if (MainWindow.this.currentSelectedItem != null
                    && !isTreeItemPopulation(MainWindow.this.currentSelectedItem)) {
                    renameItem(MainWindow.this.currentSelectedItem, name.replaceFirst("\\..*", ""));
                }
            }
        });
        this.setTextArea.addDatafilesLoadedListener(new ISimpleAction()
        {
            @Override
            public void act()
            {
                /*
                 * Update the number of known entries of the current selection, if the current selection is still the
                 * same as when the workset was changed.
                 */
                if (MainWindow.this.currentSelectedItem != null
                    && MainWindow.this.currentSelectedItem == MainWindow.this.treeItemWhenWorkSetIsChanged) {
                    TreeItemData tid = getTreeItemData(MainWindow.this.currentSelectedItem);
                    if (!tid.isProjectFolder) {
                        if (tid.numKnownEntries == -1) {
                            tid.numKnownEntries = MainWindow.this.setTextArea.getNumberOfKnownEntries();
                            updateTextOfItem(MainWindow.this.currentSelectedItem);
                        }
                    }
                }
            }
        });

        /* Second details page */
        GridData gridData10 = new org.eclipse.swt.layout.GridData();
        gridData10.grabExcessHorizontalSpace = true;
        gridData10.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        gridData10.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
        GridData settingsSeparatorLineGridData = new GridData();
        settingsSeparatorLineGridData.grabExcessHorizontalSpace = true;
        settingsSeparatorLineGridData.horizontalAlignment = GridData.FILL;
        settingsSeparatorLineGridData.horizontalSpan = 3;

        this.settingsComposite = new ProjectSettingsComposite(stackComposite, SWT.NONE);
        this.settingsComposite.setLayoutData(gridData10);

        /* TODO: Move the controller logic into Ontologizer class */
        this.settingsComposite.addOntologyChangedAction(new ISimpleAction()
        {
            @Override
            public void act()
            {
                storeGenes();
                updateGenes();
            }
        });
        this.settingsComposite.addAssociationChangedAction(new ISimpleAction()
        {
            @Override
            public void act()
            {
                storeGenes();
                updateGenes();
            }
        });
    }

    /**
     * This method initializes left composite.
     */
    private void createLeftComposite()
    {
        GridLayout gridLayout2 = new GridLayout();
        gridLayout2.marginWidth = 0;
        gridLayout2.marginHeight = 0;
        gridLayout2.numColumns = 1;
        gridLayout2.verticalSpacing = 5;
        gridLayout2.horizontalSpacing = 5;
        this.leftComposite = new Composite(this.sashForm, SWT.NONE);
        this.leftComposite.setLayout(gridLayout2);
        createWorkspaceGroup();
    }

    /**
     * This method initializes workspace composite.
     */
    private void createWorkspaceGroup()
    {
        /* Workspace Composite */
        final CTabFolder workspaceFolder = new CTabFolder(this.leftComposite, SWT.BORDER);
        workspaceFolder.setSingle(true);
        workspaceFolder.setMaximizeVisible(true);
        workspaceFolder
            .setSelectionBackground(this.leftComposite.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        workspaceFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        CTabItem workspaceCTabItem = new CTabItem(workspaceFolder, 0);
        workspaceCTabItem.setText("Workspace");
        workspaceFolder.setSelection(0);

        Composite workspaceComposite = new Composite(workspaceFolder, 0);
        workspaceComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(1));
        workspaceCTabItem.setControl(workspaceComposite);

        this.workspaceTree = new Tree(workspaceComposite, SWT.BORDER);
        this.workspaceTree.setLayoutData(new GridData(GridData.FILL_BOTH));
        this.workspaceTree.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
            {
                storeGenes();
                updateGenes();
            }
        });
        this.workspaceTree.addTreeListener(new TreeListener()
        {
            @Override
            public void treeExpanded(TreeEvent e)
            {
                TreeItemData tid = getTreeItemData((TreeItem) e.item);
                if (tid != null && tid.isProjectFolder) {
                    tid.settings.isClosed = false;
                    storeProjectSettings(tid);
                }
            }

            @Override
            public void treeCollapsed(TreeEvent e)
            {
                TreeItemData tid = getTreeItemData((TreeItem) e.item);
                if (tid != null && tid.isProjectFolder) {
                    tid.settings.isClosed = true;
                    storeProjectSettings(tid);
                }
            }
        });
        this.workspaceTree.addListener(SWT.PaintItem, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                TreeItem item = (TreeItem) event.item;
                TreeItemData tid = getTreeItemData(item);

                if (!tid.isProjectFolder) {
                    int itemHeight = MainWindow.this.workspaceTree.getItemHeight();
                    int x = event.x;
                    int y = event.y + (itemHeight - event.gc.getFontMetrics().getHeight()) / 2;

                    event.gc
                        .setForeground(MainWindow.this.workspaceTree.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
                    if (tid.numKnownEntries != -1) {
                        event.gc.drawText(tid.numKnownEntries + "/" + tid.numEntries, x + event.width + 5, y, true);
                    } else {
                        event.gc.drawText(tid.numEntries + "", x + event.width + 5, y, true);
                    }
                }
            }
        });

        this.workspaceTreeEditor = new TreeEditor(this.workspaceTree);
        this.workspaceTreeEditor.grabHorizontal = true;
        this.workspaceTreeEditor.horizontalAlignment = SWT.LEFT;
        this.workspaceTreeEditor.minimumWidth = 50;
        this.workspaceTree.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseDoubleClick(MouseEvent ev)
            {
                /* Clean up any previous editor control */
                Control oldEditor = MainWindow.this.workspaceTreeEditor.getEditor();
                if (oldEditor != null) {
                    oldEditor.dispose();
                }

                /* Identify the selected row */
                TreeItem[] items = MainWindow.this.workspaceTree.getSelection();
                if (items.length == 0) {
                    return;
                }

                /* The control that will be the editor must be a child of the Tree */
                final Text text = new Text(MainWindow.this.workspaceTree, SWT.NONE);
                final TreeItem item = items[0];

                TreeItemData tid = getTreeItemData(item);

                if (tid.isProjectFolder) {
                    text.setText(tid.projectDirectory.getName());
                } else {
                    text.setText(tid.filename);
                }

                text.addFocusListener(new FocusAdapter()
                {
                    @Override
                    public void focusLost(FocusEvent ev)
                    {
                        renameItem(item, text.getText());
                        text.dispose();
                    }
                });
                text.addTraverseListener(new TraverseListener()
                {
                    @Override
                    public void keyTraversed(TraverseEvent ev)
                    {
                        switch (ev.detail) {
                            case SWT.TRAVERSE_RETURN:
                                renameItem(item, text.getText());
                                /* FALL THROUGH */
                            case SWT.TRAVERSE_ESCAPE:
                                text.dispose();
                                ev.doit = false;
                                break;
                        }
                    }
                });
                text.selectAll();
                text.setFocus();

                MainWindow.this.workspaceTreeEditor.setEditor(text, items[0]);
            }
        });
    }

    /**
     * Save entry.
     *
     * @param zf
     * @param target
     * @param dest
     * @throws ZipException
     * @throws IOException
     */
    public static void saveEntry(ZipFile zf, ZipEntry target, String dest) throws ZipException, IOException
    {
        File file = new File(dest, target.getName());
        if (target.isDirectory()) {
            file.mkdirs();
        } else {
            InputStream is = zf.getInputStream(target);
            BufferedInputStream bis = new BufferedInputStream(is);
            // new File(dest,file.getParent()).mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            final int EOF = -1;
            for (int c; (c = bis.read()) != EOF;) {
                bos.write((byte) c);
            }
            bos.close();
            fos.close();
        }
    }

    /**
     * Sets the status text.
     *
     * @param txt
     */
    public void setStatusText(String txt)
    {
        this.statusText.setText(txt);
    }

    public void updateWorkSetList(WorkSetList wsl)
    {
        this.settingsComposite.updateWorkSetList(wsl);
    }

    public void initProgressBar(int max)
    {
        this.statusProgressBar.setMaximum(max);
    }

    public void updateProgressBar(int current)
    {
        this.statusProgressBar.setSelection(current);
    }

    public void showProgressBar()
    {
        this.statusProgressBar.setVisible(true);
    }

    public void hideProgressBar()
    {
        this.statusProgressBar.setVisible(false);
    }

    public String getSelectedWorkSet()
    {
        return this.settingsComposite.getSelectedWorksetName();
    }

    private void newPopulationAction()
    {
        TreeItem[] items = this.workspaceTree.getSelection();
        if (items != null && items.length > 0) {
            storeGenes();

            /* Find proper parent which must be a project */
            TreeItem parent = getProjectItem(items[0]);
            File projectDirectory = getTreeItemData(parent).projectDirectory;

            File newPopFile = new File(projectDirectory, "Population");
            if (!newPopFile.exists()) {
                try {
                    newPopFile.createNewFile();
                    TreeItem newPopItem = newPopItem(parent, "Population");
                    this.workspaceTree.setSelection(new TreeItem[] { newPopItem });
                    parent.setExpanded(true);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                updateGenes();
            }
        }
    }

    private void newStudyAction()
    {
        TreeItem[] items = this.workspaceTree.getSelection();
        if (items != null && items.length > 0) {
            storeGenes();

            /* Find proper parent which must be a project */
            TreeItem parent = getProjectItem(items[0]);

            File projectDirectory = getTreeItemData(parent).projectDirectory;

            /* Create a new study set, ensure that it doesn't exists before */
            File f;
            int i = 1;
            do {
                String name = "New Study Set";
                if (i != 1) {
                    name += " (" + i + ")";
                }
                f = new File(projectDirectory, name);
                i++;
            } while (f.exists());

            File newStudyFile = new File(projectDirectory, f.getName());
            try {
                newStudyFile.createNewFile();
                TreeItem newStudyItem = newStudyItem(parent, f.getName());
                this.workspaceTree.setSelection(new TreeItem[] { newStudyItem });
                parent.setExpanded(true);
            } catch (IOException e1) {
                e1.printStackTrace();
                f.delete();
            }

            updateGenes();
        }
    }

    /**
     * Returns the selected working set.
     *
     * @return
     */
    public WorkSet getSelectedWorkingSet()
    {
        return this.settingsComposite.getSelectedWorkset();
    }

    /**
     * Returns the currently selected subontology.
     *
     * @return
     */
    public String getSubontologyString()
    {
        return this.settingsComposite.getSubontologyString();
    }

    /**
     * Set the selected subset ontology string.
     *
     * @param subontology
     */
    public void setSubontology(String subontology)
    {
        this.settingsComposite.setConsider(subontology);
    }

    public void setSubset(String subset)
    {
        this.settingsComposite.setRestriction(subset);
    }

    public String getSubsetString()
    {
        return this.settingsComposite.getSubsetString();
    }

    /**
     * Sets whether the MTC selection is enabled.
     *
     * @param supportsTestCorrection
     */
    public void setMTCEnabled(boolean supportsTestCorrection)
    {
        this.mtcCombo.setEnabled(supportsTestCorrection);
    }

    /**
     * Returns the currently selected evidences.
     *
     * @return
     */
    public Collection<String> getCheckedEvidences()
    {
        return this.settingsComposite.getCheckedEvidences();
    }
}
