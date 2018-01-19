/*
 * Created on 19.11.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import ontologizer.gui.swt.support.FileGridCompositeWidgets;
import ontologizer.gui.swt.support.SWTUtil;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetList;

/**
 * A simple expander widget.
 *
 * @author Sebastian Bauer
 */
class Expander extends Composite
{
    private Composite control;

    private Button expandButton;

    private boolean visible;

    public Expander(Composite parent, int style)
    {
        super(parent, style);

        this.setLayout(SWTUtil.newEmptyMarginGridLayout(1));

        this.expandButton = new Button(this, 0);
        this.expandButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Expander.this.control.setVisible(!Expander.this.control.getVisible());

                Expander.this.visible = Expander.this.control.getVisible();
                updateButtonText();

            }
        });
        updateButtonText();
    }

    public void setText(String text)
    {
        this.expandButton.setText(text);
    }

    public void setControl(Composite control)
    {
        this.control = control;
        control.setVisible(this.visible);
        control.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));
    }

    private void updateButtonText()
    {
        if (!this.visible) {
            this.expandButton.setText("Show Advanced Options >>");
        } else {
            this.expandButton.setText("<< Hide Advanced Options");
        }
        this.expandButton.pack(true);
        if (getParent() != null) {
            getParent().layout();
        }
    }

    void setExpandedState(boolean visible)
    {
        this.visible = visible;
        this.control.setVisible(visible);
        updateButtonText();
    }
}

/**
 * The Composite of project settings. TODO: Add a ProjectSettings class
 *
 * @author Sebastian Bauer
 */
public class ProjectSettingsComposite extends Composite
{
    /**
     * Private class holding description for evidence codes.
     *
     * @author Sebastian Bauer
     */
    private static class Evidence
    {
        public String name;

        public String description;

        public String cl;

        public Evidence(String name, String description, String cl)
        {
            this.name = name;
            this.description = description;
            this.cl = cl;
        }
    };

    /**
     * Supported evidence codes.
     */
    private static Evidence[] EVIDENCES = new Evidence[] {
    new Evidence("EXP", "Inferred from Experiment", "Experimental Evidence Codes"),
    new Evidence("IDA", "Inferred from Direct Assay", "Experimental Evidence Codes"),
    new Evidence("IPI", "Inferred from Physical Interaction", "Experimental Evidence Codes"),
    new Evidence("IMP", "Inferred from Mutant Phenotype", "Experimental Evidence Codes"),
    new Evidence("IGI", "Inferred from Genetic Interaction", "Experimental Evidence Codes"),
    new Evidence("IEP", "Inferred from Expression Pattern", "Experimental Evidence Codes"),

    new Evidence("ISS", "Inferred from Sequence or Structural Similarity", "Computational Analysis Evidence Codes"),
    new Evidence("ISO", "Inferred from Sequence Orthology", "Computational Analysis Evidence Codes"),
    new Evidence("ISA", "Inferred from Sequence Alignment", "Computational Analysis Evidence Codes"),
    new Evidence("ISM", "Inferred from Sequence Model", "Computational Analysis Evidence Codes"),
    new Evidence("IGC", "Inferred from Genomic Context", "Computational Analysis Evidence Codes"),
    new Evidence("IBA", "Inferred from Biological aspect of Ancestor", "Computational Analysis Evidence Codes"),
    new Evidence("IBD", "Inferred from Biological aspect of Descendant", "Computational Analysis Evidence Codes"),
    new Evidence("IKR", "Inferred from Key Residues", "Computational Analysis Evidence Codes"),
    new Evidence("IRD", "Inferred from Rapid Divergence", "Computational Analysis Evidence Codes"),
    new Evidence("RCA", "Inferred from Reviewed Computational Analysis", "Computational Analysis Evidence Codes"),

    new Evidence("TAS", "Traceable Author Statement", "Author Statement Evidence Codes"),
    new Evidence("NAS", "Non-traceable Author Statement", "Author Statement Evidence Codes"),

    new Evidence("TAS", "Traceable Author Statement", "Author Statement Evidence Codes"),
    new Evidence("NAS", "Non-traceable Author Statement", "Author Statement Evidence Codes"),

    new Evidence("IC", "Inferred by Curator", "Curator Statement Evidence Codes"),
    new Evidence("ND", "No biological Data available", "Curator Statement Evidence Codes"),

    new Evidence("IEA", "Inferred from Electronic Annotation", "Automatically-assigned Evidence Codes"),

    new Evidence("NR", "Not Recorded", "Obsolete Evidence Codes")
    };

    private static HashMap<String, Evidence> EVIDENCE_MAP;

    /**
     * Initialize private static data;
     */
    static {
        /* Initialize evidence map */
        EVIDENCE_MAP = new HashMap<>();
        for (Evidence evi : EVIDENCES) {
            EVIDENCE_MAP.put(evi.name, evi);
        }
    }

    private Combo workSetCombo = null;

    private FileGridCompositeWidgets ontologyFileGridCompositeWidgets = null;

    private FileGridCompositeWidgets assocFileGridCompositeWidgets = null;

    private FileGridCompositeWidgets mappingFileGridCompositeWidgets = null;

    private Combo subsetCombo;

    private Combo considerCombo;

    private Table evidenceTable;

    private TableColumn evidenceNameColumn;

    private TableColumn evidenceDescColumn;

    private Composite advancedComposite;

    private Expander advancedExpander;

    private StyledText infoText;

    private Button subsetCheckbox;

    private Button considerCheckbox;

    private ArrayList<ISimpleAction> ontologyChangedList = new ArrayList<>();

    private ArrayList<ISimpleAction> associationChangedList = new ArrayList<>();

    private WorkSetList wsl;

    public ProjectSettingsComposite(Composite parent, int style)
    {
        this(parent, style, true);
    }

    public ProjectSettingsComposite(Composite parent, int style, boolean mapping)
    {
        super(parent, style);

        this.wsl = new WorkSetList();

        this.setLayout(new GridLayout(3, false));

        Label workSetLabel = new Label(this, 0);
        workSetLabel.setText("File Set");
        workSetLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        this.workSetCombo = new Combo(this, SWT.BORDER);
        this.workSetCombo.setToolTipText("Choose files from predefined file sets.");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        this.workSetCombo.setLayoutData(gd);

        this.ontologyFileGridCompositeWidgets = new FileGridCompositeWidgets(this);
        this.ontologyFileGridCompositeWidgets.setLabel("Ontology");
        this.ontologyFileGridCompositeWidgets.setToolTipText(
            "Specifies the ontology file (OBO file format) which defines the GO terms and their structure.");
        this.ontologyFileGridCompositeWidgets.setFilterExtensions(new String[] { "*.obo", "*.*" });
        this.ontologyFileGridCompositeWidgets.setFilterNames(new String[] { "OBO File", "All files" });

        this.assocFileGridCompositeWidgets = new FileGridCompositeWidgets(this);
        this.assocFileGridCompositeWidgets.setLabel("Annotations");
        this.assocFileGridCompositeWidgets.setToolTipText(
            "Specifies the annotation (association) file, which assigns GO terms to the names of the gene products.");
        this.assocFileGridCompositeWidgets
            .setFilterExtensions(new String[] { "gene_association.*", "*.csv", "*.ids", "*.*" });
        this.assocFileGridCompositeWidgets
            .setFilterNames(new String[] { "Association File", "Affymetrix", "All files" });

        /* TODO: Use ExpandableComposite comp of JFace */

        this.advancedExpander = new Expander(this, 0);
        gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.GRAB_HORIZONTAL
            | GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 3;
        this.advancedExpander.setLayoutData(gd);

        this.advancedComposite = new Composite(this.advancedExpander, 0);
        this.advancedComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(3));
        this.advancedExpander.setControl(this.advancedComposite);

        this.subsetCheckbox = new Button(this.advancedComposite, SWT.CHECK);
        this.subsetCheckbox.setText("Use Subset of Ontology");
        this.subsetCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        this.subsetCheckbox.setEnabled(false);
        this.subsetCheckbox.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updateSubsetEnabled();
            }
        });

        this.subsetCombo = new Combo(this.advancedComposite, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gd.horizontalSpan = 2;
        this.subsetCombo.setLayoutData(gd);
        this.subsetCombo.setEnabled(false);

        this.considerCheckbox = new Button(this.advancedComposite, SWT.CHECK);
        this.considerCheckbox.setText("Consider Terms from");
        this.considerCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        this.considerCheckbox.setEnabled(false);
        this.considerCheckbox.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updateConsiderEnabled();
            }
        });
        this.considerCombo = new Combo(this.advancedComposite, SWT.BORDER);
        this.considerCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        Label subOntologyLabel = new Label(this.advancedComposite, 0);
        subOntologyLabel.setText("Subontology");
        this.considerCombo.setEnabled(false);

        if (mapping) {
            this.mappingFileGridCompositeWidgets = new FileGridCompositeWidgets(this.advancedComposite, true);
            this.mappingFileGridCompositeWidgets.setLabel("Mapping");
            this.mappingFileGridCompositeWidgets.setToolTipText(
                "Specifies an additional mapping file in which each line consits of a single name mapping. The name of the first column is mapped to the name of the second column before the annotation process begins. Columns should be tab-separated.");
            this.mappingFileGridCompositeWidgets.setFilterExtensions(new String[] { "*.*" });
            this.mappingFileGridCompositeWidgets.setFilterNames(new String[] { "All files" });
        }

        Label evidenceLabel = new Label(this.advancedComposite, 0);
        evidenceLabel.setText("Evidences");
        evidenceLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        this.evidenceTable = new Table(this.advancedComposite, SWT.BORDER | SWT.CHECK);
        this.evidenceTable.setLayoutData(gd);
        this.evidenceTable.setEnabled(false);
        this.evidenceNameColumn = new TableColumn(this.evidenceTable, SWT.NONE);
        this.evidenceDescColumn = new TableColumn(this.evidenceTable, SWT.NONE);

        /* If a new work set has been selected */
        this.workSetCombo.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String currentWorkSet = getSelectedWorksetName();
                if (ProjectSettingsComposite.this.wsl != null && currentWorkSet.length() > 0) {
                    WorkSet ws = ProjectSettingsComposite.this.wsl.get(currentWorkSet);
                    if (ws != null) {
                        setAssociationsFileString(ws.getAssociationPath());
                        setDefinitonFileString(ws.getOboPath());

                        for (ISimpleAction act : ProjectSettingsComposite.this.ontologyChangedList) {
                            act.act();
                        }
                        for (ISimpleAction act : ProjectSettingsComposite.this.associationChangedList) {
                            act.act();
                        }
                    }
                }
            }
        });

        createInfoText(this.advancedComposite);
    }

    /**
     * Makes the info styled text visible (if not done)
     */
    private void createInfoText(Composite parent)
    {
        if (this.infoText != null) {
            return;
        }

        this.infoText = new StyledText(parent, SWT.WRAP);
        this.infoText.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        GridData gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH);
        gd.horizontalSpan = 3;
        gd.minimumHeight = 20;
        this.infoText.setLayoutData(gd);
        this.layout(true);
    }

    /**
     * Updates the enable status of the subset widget in accordance to the current state of the checkbox.
     */
    private void updateSubsetEnabled()
    {
        this.subsetCombo.setEnabled(this.subsetCheckbox.getSelection() && this.subsetCombo.getItemCount() > 0);
    }

    /**
     * Updates the enable status of the consider widget in accordance to the current state of the checkbox.
     */
    private void updateConsiderEnabled()
    {
        this.considerCombo.setEnabled(this.considerCheckbox.getSelection() && this.considerCombo.getItemCount() > 0);
    }

    public String getDefinitionFileString()
    {
        return this.ontologyFileGridCompositeWidgets.getPath();
    }

    /**
     * Sets the definition file string.
     *
     * @param string
     */
    public void setDefinitonFileString(String string)
    {
        this.ontologyFileGridCompositeWidgets.setPath(string != null ? string : "");
    }

    /**
     * Returns the mapping file string.
     *
     * @return
     */
    public String getMappingFileString()
    {
        return this.mappingFileGridCompositeWidgets.getPath();
    }

    /**
     * Sets the mapping file string.
     *
     * @param string
     */
    public void setMappingFileString(String string)
    {
        string = string != null ? string : "";
        this.mappingFileGridCompositeWidgets.setPath(string);
    }

    /**
     * Returns the currently selected subset.
     *
     * @return
     */
    public String getSubsetString()
    {
        if (!this.subsetCheckbox.getSelection()) {
            return "";
        }
        int idx = this.subsetCombo.getSelectionIndex();
        if (idx >= 0) {
            return this.subsetCombo.getItems()[idx];
        }
        return this.subsetCombo.getText();
    }

    /**
     * Returns the currently selected subontology.
     *
     * @return
     */
    public String getSubontologyString()
    {
        if (!this.considerCheckbox.getSelection()) {
            return "";
        }

        int idx = this.considerCombo.getSelectionIndex();
        if (idx >= 0) {
            return this.considerCombo.getItem(idx);
        }

        return this.considerCombo.getText();

    }

    /**
     * Returns the association file string.
     *
     * @return
     */
    public String getAssociationsFileString()
    {
        return this.assocFileGridCompositeWidgets.getPath();
    }

    public void setAssociationsFileString(String string)
    {
        this.assocFileGridCompositeWidgets.setPath(string != null ? string : "");
    }

    public void updateWorkSetList(WorkSetList wsl)
    {
        this.workSetCombo.removeAll();
        this.wsl.clear();

        for (WorkSet ws : wsl) {
            this.wsl.add(ws.clone());
            this.workSetCombo.add(ws.getName());
        }
    }

    public String getSelectedWorksetName()
    {
        return this.workSetCombo.getText();
    }

    public WorkSet getSelectedWorkset()
    {
        WorkSet ws = new WorkSet(this.workSetCombo.getText());
        ws.setAssociationPath(this.assocFileGridCompositeWidgets.getPath());
        ws.setOboPath(this.ontologyFileGridCompositeWidgets.getPath());
        return ws;
    }

    public void setRestrictionChoices(String[] choices)
    {
        if (choices == null) {
            choices = new String[0];
        }
        this.subsetCheckbox.setEnabled(choices.length > 0);
        // subsetCombo.setEnabled(choices.length > 0);
        this.subsetCombo.setItems(choices);
        updateSubsetEnabled();
    }

    public void setConsiderChoices(String[] choices)
    {
        if (choices == null) {
            choices = new String[0];
        }
        this.considerCheckbox.setEnabled(choices.length > 0);
        this.considerCombo.setItems(choices);
        updateConsiderEnabled();
    }

    public void setConsider(String subontology)
    {
        this.considerCombo.setText(subontology);
        this.considerCheckbox.setSelection(subontology.length() > 0);
        updateConsiderEnabled();

        if (subontology.length() > 0) {
            this.advancedExpander.setExpandedState(true);
        }
    }

    public void setRestriction(String subset)
    {
        this.subsetCombo.setText(subset);
        this.subsetCheckbox.setSelection(subset.length() > 0);
        updateSubsetEnabled();

        if (subset.length() > 0) {
            this.advancedExpander.setExpandedState(true);
        }
    }

    /**
     * Sets the given text to the information.
     *
     * @param text
     */
    public void setInfoText(String text)
    {
        this.infoText.setText(text);
    }

    /**
     * Sets the available evidences.
     *
     * @param evidences
     */
    public void setEvidences(Collection<String> evidences)
    {
        this.evidenceTable.removeAll();
        ArrayList<String> sortedEvidences = new ArrayList<>(evidences);
        Collections.sort(sortedEvidences);

        for (String ev : sortedEvidences) {
            TableItem evi = new TableItem(this.evidenceTable, 0);
            evi.setText(0, ev);
            Evidence realEvidence = EVIDENCE_MAP.get(ev);
            if (realEvidence != null) {
                evi.setText(1, realEvidence.description);
            } else {
                evi.setText(1, "Unknown");
            }
            evi.setChecked(true);
        }
        this.evidenceNameColumn.pack();
        this.evidenceDescColumn.pack();
        layout();
        this.evidenceTable.setEnabled(true);
    }

    /**
     * Clears the evidences.
     */
    public void clearEvidences()
    {
        this.evidenceTable.removeAll();
        this.evidenceTable.setEnabled(false);
    }

    /**
     * Returns the selected evidences.
     *
     * @return
     */
    public Collection<String> getCheckedEvidences()
    {
        ArrayList<String> selectedEvidences = new ArrayList<>();
        for (int i = 0; i < this.evidenceTable.getItemCount(); i++) {
            if (this.evidenceTable.getItem(i).getChecked()) {
                selectedEvidences.add(this.evidenceTable.getItem(i).getText());
            }
        }
        return selectedEvidences;
    }

    /**
     * Sets the error string of the ontology field.
     *
     * @param error can be null or "" to indicate a no-error state.
     */
    public void setOntologyErrorString(String error)
    {
        this.ontologyFileGridCompositeWidgets.setErrorString(error);
    }

    /**
     * Add an action which is invoked when the ontology file is changed.
     *
     * @param act
     */
    public void addOntologyChangedAction(ISimpleAction act)
    {
        this.ontologyFileGridCompositeWidgets.addTextChangedAction(act);
        this.ontologyChangedList.add(act);
    }

    /**
     * Sets the association error string.
     *
     * @param error can be null or "" to indicate a no-error state.
     */
    public void setAssociationErrorString(String error)
    {
        this.assocFileGridCompositeWidgets.setErrorString(error);
    }

    /**
     * Adds the action that is invoked when the association file is changed.
     *
     * @param act
     */
    public void addAssociationChangedAction(ISimpleAction act)
    {
        this.assocFileGridCompositeWidgets.addTextChangedAction(act);
        this.associationChangedList.add(act);
    }
}
