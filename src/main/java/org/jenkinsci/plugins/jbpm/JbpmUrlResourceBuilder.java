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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;

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

    private final String url;
    private final String processId;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
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
    
    public String getProcessId() {
    	return processId;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {

        // This also shows how you can consult the global configuration of the builder
        if (getDescriptor().useFrench())
            listener.getLogger().println("Bonjour, "+url+"!");
        else
            listener.getLogger().println("Hello, "+url+"!");
        listener.getLogger().println("processId: " + processId);
        
        Properties props = new Properties();
        props.setProperty("drools.dialect.java.compiler", "JANINO");
        KnowledgeBuilderConfiguration config = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration(props);
        
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(config);
        kbuilder.add(ResourceFactory.newUrlResource(url), ResourceType.BPMN2);
        if (kbuilder.hasErrors()) {
            listener.getLogger().println(kbuilder.getErrors());
            return false;
        }
        
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        
        JenkinsJobWorkItemHandler.setListener(listener);
        
        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();
        ksession.getWorkItemManager().registerWorkItemHandler(
        	    "JenkinsJob", new JenkinsJobWorkItemHandler());
        
        Map<String,Object> processVariables = new HashMap<String,Object>();
        
        Map<String,Result> jenkinsJobResults = new HashMap<String,Result>();
        processVariables.put("jenkinsJobResults", jenkinsJobResults);        
        
        Result result = null;
        processVariables.put("jenkinsLastJobResult", result);
        
        ksession.startProcess(processId, processVariables);
        
        return true;
    }
    
    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link JbpmUrlResourceBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private boolean useFrench;
        
        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Invoke a BPMN 2.0 business process";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            
            //processFileName = formData.getString("processFileName");
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         */
        public boolean useFrench() {
            return useFrench;
        }
        
    }
}

