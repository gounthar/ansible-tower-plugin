package org.jenkinsci.plugins.ansible_tower;

/*
    This class is the pipeline step
    We simply take the data from Jenkins and call an AnsibleTowerRunner
 */

import com.google.inject.Inject;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.*;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.ansible_tower.util.TowerInstallation;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.File;

public class AnsibleTowerStep extends AbstractStepImpl {
    private String towerServer              = "";
    private String jobTemplate              = "";
    private String extraVars                = "";
    private String limit                    = "";
    private String jobTags                  = "";
    private String inventory                = "";
    private String credential               = "";
    private Boolean verbose                 = false;
    private Boolean importTowerLogs         = false;
    private Boolean removeColor             = false;
    private String templateType             = "job";
    private Boolean importWorkflowChildLogs = false;

    @DataBoundConstructor
    public AnsibleTowerStep(
            @Nonnull String towerServer, @Nonnull String jobTemplate, String extraVars, String jobTags,
            String limit, String inventory, String credential, Boolean verbose, Boolean importTowerLogs,
            Boolean removeColor, String templateType, Boolean importWorkflowChildLogs
    ) {
        this.towerServer = towerServer;
        this.jobTemplate = jobTemplate;
        this.extraVars = extraVars;
        this.jobTags = jobTags;
        this.limit = limit;
        this.inventory = inventory;
        this.credential = credential;
        this.verbose = verbose;
        this.importTowerLogs = importTowerLogs;
        this.removeColor = removeColor;
        this.templateType = templateType;
        this.importWorkflowChildLogs = importWorkflowChildLogs;
    }

    @Nonnull
    public String getTowerServer()              { return towerServer; }
    @Nonnull
    public String getJobTemplate()              { return jobTemplate; }
    public String getExtraVars()                { return extraVars; }
    public String getJobTags()                  { return jobTags; }
    public String getLimit()                    { return limit; }
    public String getInventory()                { return inventory; }
    public String getCredential()               { return credential; }
    public Boolean getVerbose()                 { return verbose; }
    public Boolean getImportTowerLogs()         { return importTowerLogs; }
    public Boolean getRemoveColor()             { return removeColor; }
    public String getTemplateType()             { return templateType; }
    public Boolean getImportWorkflowChildLogs() { return importWorkflowChildLogs; }

    @DataBoundSetter
    public void setTowerServer(String towerServer) { this.towerServer = towerServer; }
    @DataBoundSetter
    public void setJobTemplate(String jobTemplate) { this.jobTemplate = jobTemplate; }
    @DataBoundSetter
    public void setExtraVars(String extraVars) { this.extraVars = extraVars; }
    @DataBoundSetter
    public void setJobTags(String jobTags) { this.jobTags = jobTags; }
    @DataBoundSetter
    public void setLimit(String limit) { this.limit = limit; }
    @DataBoundSetter
    public void setInventory(String inventory) { this.inventory = inventory; }
    @DataBoundSetter
    public void setCredential(String credential) { this.credential = credential; }
    @DataBoundSetter
    public void setVerbose(Boolean verbose) { this.verbose = verbose; }
    @DataBoundSetter
    public void setImportTowerLogs(Boolean importTowerLogs) { this.importTowerLogs = importTowerLogs; }
    @DataBoundSetter
    public void setRemoveColor(Boolean removeColor) { this.removeColor = removeColor; }
    @DataBoundSetter
    public void setTemplateType(String templateType) { this.templateType = templateType; }
    @DataBoundSetter
    public void setImportWorkflowChildLogs(Boolean importWorkflowChildLogs) { this.importWorkflowChildLogs = importWorkflowChildLogs; }

    public boolean isGlobalColorAllowed() {
        System.out.println("Using the class is global color allowed");
        return true;
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        public static final String towerServer              = AnsibleTower.DescriptorImpl.towerServer;
        public static final String jobTemplate              = AnsibleTower.DescriptorImpl.jobTemplate;
        public static final String extraVars                = AnsibleTower.DescriptorImpl.extraVars;
        public static final String limit                    = AnsibleTower.DescriptorImpl.limit;
        public static final String jobTags                  = AnsibleTower.DescriptorImpl.jobTags;
        public static final String inventory                = AnsibleTower.DescriptorImpl.inventory;
        public static final String credential               = AnsibleTower.DescriptorImpl.credential;
        public static final Boolean verbose                 = AnsibleTower.DescriptorImpl.verbose;
        public static final Boolean importTowerLogs         = AnsibleTower.DescriptorImpl.importTowerLogs;
        public static final Boolean removeColor             = AnsibleTower.DescriptorImpl.removeColor;
        public static final String templateType             = AnsibleTower.DescriptorImpl.templateType;
        public static final Boolean importWorkflowChildLogs = AnsibleTower.DescriptorImpl.importWorkflowChildLogs;

        public DescriptorImpl() {
            super(AnsibleTowerStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "ansibleTower";
        }

        @Override
        public String getDisplayName() {
            return "Have Ansible Tower run a job template";
        }

        public ListBoxModel doFillTowerServerItems() {
            ListBoxModel items = new ListBoxModel();
            items.add(" - None -");
            for (TowerInstallation towerServer : AnsibleTowerGlobalConfig.get().getTowerInstallation()) {
                items.add(towerServer.getTowerDisplayName());
            }
            return items;
        }

        public ListBoxModel doFillTemplateTypeItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("job");
            items.add("workflow");
            return items;
        }

        public boolean isGlobalColorAllowed() {
            System.out.println("Using the descriptor is global color allowed");
            return true;
        }
    }


    public static final class AnsibleTowerStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @Inject
        private transient AnsibleTowerStep step;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @StepContextParameter
        private transient Run<?,?> run;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars envVars;

        @StepContextParameter
        private transient Computer computer;



        @Override
        protected Void run() throws Exception {
            if ((computer == null) || (computer.getNode() == null)) {
                throw new AbortException("The Ansible Tower build step requires to be launched on a node");
            }

            AnsibleTowerRunner runner = new AnsibleTowerRunner();

            // Doing this will make the options optional in the pipeline step.
            String extraVars = "";
            if(step.getExtraVars() != null) { extraVars = step.getExtraVars(); }
            String limit = "";
            if(step.getLimit() != null) { limit = step.getLimit(); }
            String tags = "";
            if(step.getJobTags() != null) { tags = step.getJobTags(); }
            String inventory = "";
            if(step.getInventory() != null) { inventory = step.getInventory(); }
            String credential = "";
            if(step.getCredential() != null) { credential = step.getCredential(); }
            boolean verbose = false;
            if(step.getVerbose() != null) { verbose = step.getVerbose(); }
            boolean importTowerLogs = false;
            if(step.getImportTowerLogs() != null) { importTowerLogs = step.getImportTowerLogs(); }
            boolean removeColor = false;
            if(step.getRemoveColor() != null) { removeColor = step.getRemoveColor(); }
            String templateType = "job";
            if(step.getTemplateType() != null) { templateType = step.getTemplateType(); }
            boolean importWorkflowChildLogs = false;
            if(step.getImportWorkflowChildLogs() != null) { importWorkflowChildLogs = step.getImportWorkflowChildLogs(); }

            boolean runResult = runner.runJobTemplate(
                    listener.getLogger(), step.getTowerServer(), step.getJobTemplate(), extraVars,
                    limit, tags, inventory, credential, verbose, importTowerLogs, removeColor, envVars,
                    templateType, importWorkflowChildLogs, ws, run
            );
            if(!runResult) {
                throw new AbortException("Ansible Tower build step failed");
            }
            return null;
        }
    }
}

