/*
 * Created on 19.11.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ontologizer.gui.swt.support.SWTUtil;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetList;

/**
 * Wizard used to create a new project.
 *
 * @author Sebastian Bauer
 */
public class NewProjectWizard extends WizardWindow
{
    private static class NewStudySet
    {
        public String name;

        public String contents;
    }

    private Text projectNameText;

    private ProjectSettingsComposite projectSettingsComposite;

    private GeneEditor populationEditor;

    private GeneEditor studyEditor;

    private Text studyNameText;

    private Label studyNameLabel;

    private List<NewStudySet> studySetList = new LinkedList<>();

    public NewProjectWizard(Display display)
    {
        super(display);

        this.shell.setText("Ontologizer - New Project Wizard");
        this.shell.pack();

        if (this.shell.getClientArea().height < 400) {
            this.shell.setSize(this.shell.getClientArea().width, 400);
        }
    }

    public void open(WorkSetList wsl)
    {
        this.studySetList.clear();
        this.projectSettingsComposite.updateWorkSetList(wsl);
        super.open();
    }

    @Override
    protected void addPages(Composite parent)
    {
        /* First page */
        Composite first = new Composite(parent, 0);
        first.setLayout(new GridLayout(2, false));

        Label l = new Label(first, 0);
        l.setText("Project Name");

        this.projectNameText = new Text(first, SWT.BORDER);
        this.projectNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        this.projectNameText.addModifyListener(new ModifyListener()
        {
            @Override
            public void modifyText(ModifyEvent e)
            {
                verifyProjectName();
            }
        });

        addPage(new SinglePage(first,
            "Welcome! This wizard guides you through the creation of a new Ontologizer project.\n" +
                "First, please specifiy the name of the new project. Then press \"Next\" to proceed to the next page."));

        /* Second page */
        Composite second = new Composite(parent, 0);

        second.setLayout(new GridLayout(1, false));
        this.projectSettingsComposite = new ProjectSettingsComposite(second, 0, false);
        this.projectSettingsComposite
            .setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
        PageCallback secondCallback = new PageCallback()
        {
            @Override
            public boolean completed()
            {
                WorkSet ws = NewProjectWizard.this.projectSettingsComposite.getSelectedWorkset();
                NewProjectWizard.this.populationEditor.setWorkSet(ws);
                NewProjectWizard.this.studyEditor.setWorkSet(ws);
                return true;
            }
        };

        addPage(new SinglePage(second, "Now please select a set of ontology and association files. " +
            "An ontology file contains the plain definitions of  terms and their mutual relation. " +
            "An association file assigns identifiers (e.g., gene names) to the terms and often depends on the organism in question. "
            +
            "You may specify the files manually or via predefined work sets which contain suitable settings of frequently used species.",
            secondCallback));

        /* Third page */
        Composite third = new Composite(parent, 0);
        third.setLayout(new GridLayout());
        this.populationEditor = new GeneEditor(third, 0);
        this.populationEditor.setLayoutData(new GridData(GridData.FILL_BOTH));

        addPage(new SinglePage(third, "Here you are asked to specify the population set of the analysis. " +
            "The population set specifies the identifiers for all instances that are the selectable canditates in an experiment. "
            +
            "For instance, an appropriate population set for a downstream microarray analysis consists of all the genes on the microarray."));

        /* Fourth page */
        Composite fourth = new Composite(parent, 0);
        fourth.setLayout(new GridLayout());
        Composite nameComposite = new Composite(fourth, 0);
        nameComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(2));
        nameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        this.studyNameLabel = new Label(nameComposite, 0);
        this.studyNameLabel.setText("Study Set Name");
        this.studyNameText = new Text(nameComposite, SWT.BORDER);
        this.studyNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        this.studyEditor = new GeneEditor(fourth, 0);
        this.studyEditor.setLayoutData(new GridData(GridData.FILL_BOTH));
        this.studyEditor.addNewNameListener(new GeneEditor.INewNameListener()
        {
            @Override
            public void newName(String name)
            {
                if (name.endsWith(".txt")) {
                    name = name.substring(0, name.length() - 4);
                }
                NewProjectWizard.this.studyNameText.setText(name);
            }
        });

        PageCallback fourthCallback = new PageCallback()
        {
            @Override
            public boolean completed()
            {
                NewStudySet sset = new NewStudySet();
                sset.name = NewProjectWizard.this.studyNameText.getText();
                sset.contents = NewProjectWizard.this.studyEditor.getText();

                if (NewProjectWizard.this.currentPage - 3 < NewProjectWizard.this.studySetList.size()) {
                    NewProjectWizard.this.studySetList.set(NewProjectWizard.this.currentPage - 3, sset);
                } else {
                    NewProjectWizard.this.studySetList.add(sset);
                }

                return true;
            }
        };

        addPage(new SinglePage(fourth, "Now please specify the study set. " +
            "A study set contains the identifiers for all instances that actually were selected due to the experiment. "
            +
            "For instance, an appropriate study set for a downstream microarray analysis are all the genes that were identfied to be differentially expressed. "
            +
            "Note that the study set should be a subset of the population set, otherwise the population set gets automatically extended during the calculation. "
            +
            "If you wish to specify another study set, please press \"Next\" otherwise \"Finish\".",
            fourthCallback));

    }

    /**
     * Verify the project's name.
     */
    private void verifyProjectName()
    {
        if (this.currentPage == 0) {
            String txt = this.projectNameText.getText();
            if (txt.length() == 0) {
                displayError(null);
            } else {
                if (!Ontologizer.isProjectNameValid(txt)) {
                    displayError("A project with name \"" + txt + "\" already exists.");
                } else {
                    clearError();
                }
            }
        }
    }

    @Override
    protected int getDisplayedPageNumber(int which)
    {
        if (which > 3) {
            return 3;
        }
        return which;
    }

    @Override
    protected void showPage(int which)
    {
        if (which == 0) {
            this.projectNameText.forceFocus();
        }
        /*
         * This is called when a page is about to be shown. We clear set up new defaults.
         */
        if (which > 2) {
            int w = which - 3;
            if (w < this.studySetList.size()) {
                NewStudySet studySet = this.studySetList.get(w);
                this.studyNameText.setText(studySet.name);
                this.studyEditor.setText(studySet.contents);
            } else {
                this.studyNameText.setText("Study " + (w + 1));
                this.studyEditor.clear();
            }

            this.studyNameLabel.setText("Name of study set " + (w + 1));
            this.studyNameLabel.getParent().layout();
        }
        super.showPage(which);
    }

    @Override
    protected boolean finish()
    {
        File projectDrawer = new File(Ontologizer.getWorkspace(), this.projectNameText.getText());
        try {
            projectDrawer.mkdirs();
            File populationFile = new File(projectDrawer, "Population");
            PrintWriter pw = new PrintWriter(populationFile);
            pw.write(this.populationEditor.getText());
            pw.close();

            Properties prop = new Properties();
            prop.setProperty("annotationsFileName", this.projectSettingsComposite.getAssociationsFileString());
            prop.setProperty("ontologyFileName", this.projectSettingsComposite.getDefinitionFileString());
            FileOutputStream fos = new FileOutputStream(new File(projectDrawer, MainWindow.PROJECT_SETTINGS_NAME));
            prop.storeToXML(fos, "Ontologizer Project File");
            fos.close();

            for (NewStudySet nss : this.studySetList) {
                File studyFile = new File(projectDrawer, nss.name);
                pw = new PrintWriter(studyFile);
                pw.write(nss.contents);
                pw.close();
            }

            Ontologizer.newProject(projectDrawer);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Ontologizer.newProject(projectDrawer);
            return false;
        }
        return true;
    }

    @Override
    protected void reset()
    {
        this.projectNameText.setText("");
        verifyProjectName();
    }
}
