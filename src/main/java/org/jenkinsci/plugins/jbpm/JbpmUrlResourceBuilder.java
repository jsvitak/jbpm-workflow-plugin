/*
    Copyright 2012 Jiri Svitak 
  
    This file is part of jBPM workflow plugin.

    jBPM workflow plugin is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    jBPM workflow plugin is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with jBPM workflow plugin.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.jenkinsci.plugins.jbpm;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sf.json.JSONObject;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderConfiguration;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * 
 * Implementation of build step which is able to invoke a BPMN 2.0 business process.
 * Uses jBPM 5.
 * 
 * @author Jiri Svitak
 *
 */
public class JbpmUrlResourceBuilder extends Builder {

    // business process url
    private final String url;
    // business process identifier
    private final String processId;

    /**
     * Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
     */
    @DataBoundConstructor
    public JbpmUrlResourceBuilder(String url, String processId) {
        this.url = url;
        this.processId = processId;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getUrl() {
        return url;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getProcessId() {
    	return processId;
    }

    /**
     * Implements behavior of our build step.
     */
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {

	Logger.setListener(listener);
	Logger.setCliConsoleEnabled(!getDescriptor().disableCliConsoleLogging());
	Logger.setWebConsoleEnabled(!getDescriptor().disableWebConsoleLogging());
        
        Properties props = new Properties();
        props.setProperty("drools.dialect.java.compiler", "JANINO");
        KnowledgeBuilderConfiguration config = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration(props);
        
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(config);
        kbuilder.add(ResourceFactory.newUrlResource(url), ResourceType.BPMN2);
        if (kbuilder.hasErrors()) {
            Logger.log("Building of a business process definition " + processId + " from location " + url.toString() + "has failed due to following reasons:");
            Logger.log(kbuilder.getErrors().toString());
            return false;
        }
        
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        
        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();
        ksession.getWorkItemManager().registerWorkItemHandler(
        	    "JenkinsJob", new JenkinsJobWorkItemHandler());
        
        Map<String,Object> processVariables = new HashMap<String,Object>();
        
        Map<String,Result> jenkinsJobResults = new HashMap<String,Result>();
        processVariables.put("jenkinsJobResults", jenkinsJobResults);        
        
        Result result = null;
        processVariables.put("jenkinsLastJobResult", result);
        
        Logger.log("Starting business process " + processId);
        ksession.startProcess(processId, processVariables);
        
        return true;
    }
    
    /**
     * Not necessary (as we define some properties), but overridden for better safety.
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link JbpmUrlResourceBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

	// global configuration fields, persisted by default
        private boolean disableCliConsoleLogging;
        private boolean disableWebConsoleLogging;
	
        /**
         * Performs on-the-fly validation of the form field 'url'.
         * @param value
         * 	This parameter receives the url string.
         * @return
         * 	Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckUrl(@QueryParameter String value) {
            try {
        	URL url = new URL(value);
        	URLConnection conn = url.openConnection();
        	conn.connect();
            } catch (MalformedURLException e) {
        	return FormValidation.error("The URL is not in a valid form.");
            } catch (IOException e) {
        	return FormValidation.error("The connection could not be established to specified URL.");
            }
            return FormValidation.ok();
        }
        
        /**
         * Performs on-the-fly validation of the form field 'processId'.
         * @param value
         * 	This parameter receives the processId string.
         * @return
         * 	Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckProcessId(@QueryParameter String value) {
            if (value.length() == 0) {
        	return FormValidation.error("Please specify a valid process ID.");
            }
            return FormValidation.ok();
        }

        /**
         * Indicates that this builder can be used with all kinds of project types.
         */
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * This human readable name is used in the configuration screen
         * when selecting a new build step.
         */
        public String getDisplayName() {
            return "Invoke a BPMN 2.0 business process";
        }

        /**
         * Saves global configuration.
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            disableCliConsoleLogging = formData.getBoolean("disableCliConsoleLogging");
            disableWebConsoleLogging = formData.getBoolean("disableWebConsoleLogging");
            save();
            return super.configure(req,formData);
        }

        /**
         * Used to load current value for global configuration screen.
         */
        public boolean disableCliConsoleLogging() {
            return disableCliConsoleLogging;
        }
 
        /**
         * Used to load current value for global configuration screen.
         */
        public boolean disableWebConsoleLogging() {
            return disableWebConsoleLogging;
        }
        
    }
}

