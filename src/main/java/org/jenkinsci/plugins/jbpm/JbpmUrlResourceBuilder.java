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
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import net.sf.json.JSONObject;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderConfiguration;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.jbpm.process.workitem.wsht.HornetQHTWorkItemHandler;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * 
 * Implementation of build step which is able to invoke a BPMN 2.0 business
 * process. Uses jBPM 5.
 * 
 * @author Jiri Svitak
 * 
 */
@SuppressWarnings("rawtypes")
public class JbpmUrlResourceBuilder extends Builder {

    /**
     * Business process definition url
     */
    private final String url;
    
    /**
     * Business process identifier
     */
    private final String processId;

    /**
     * Fields in config.jelly must match the parameter names in the
     * "DataBoundConstructor"
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
     * Implements a build step - starts jBPM engine.
     */
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher,
	    BuildListener listener) {

	Logger.setListener(listener);
	Logger.setCliConsoleEnabled(!getDescriptor().disableCliConsoleLogging());
	Logger.setWebConsoleEnabled(!getDescriptor().disableWebConsoleLogging());

	Properties droolsProperties = new Properties();
	droolsProperties.setProperty("drools.dialect.java.compiler", "JANINO");
	KnowledgeBuilderConfiguration config = KnowledgeBuilderFactory
		.newKnowledgeBuilderConfiguration(droolsProperties);

	KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
		.newKnowledgeBuilder(config);

	Authenticator.setDefault(new Authenticator() {
	    @Override
	    protected PasswordAuthentication getPasswordAuthentication() {
		Properties pluginProperties = new Properties();
		try {
		    FileInputStream in;
		    in = new FileInputStream("plugin.properties");
		    pluginProperties.load(in);
		    in.close();
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		}
		return new PasswordAuthentication(pluginProperties.getProperty(
			"guvnor.user", "admin"), pluginProperties.getProperty(
			"guvnor.password", "admin").toCharArray());
	    }
	});

	kbuilder.add(ResourceFactory.newUrlResource(url), ResourceType.BPMN2);
	if (kbuilder.hasErrors()) {
	    Logger.log("Failed to build a business process definition "
		    + processId + " from location " + url.toString()
		    + " due to the following reasons:");
	    Logger.log(kbuilder.getErrors().toString());
	    return false;
	}

	KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
	kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());

	StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();

	ksession.getWorkItemManager().registerWorkItemHandler("JenkinsJob",
		new JenkinsJobWorkItemHandler());
	ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
		new HornetQHTWorkItemHandler(ksession));

	CountDownLatch latch = new CountDownLatch(1);
	CompleteProcessEventListener processEventListener = new CompleteProcessEventListener(
		latch);
	ksession.addEventListener(processEventListener);

	Logger.log("Started business process " + processId);

	ksession.startProcess(processId);
	try {
	    latch.await();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}

	Logger.log("Completed business process " + processId);

	return true;
    }

    /**
     * Not necessary (as we define some properties), but overridden for better
     * safety.
     */
    @Override
    public DescriptorImpl getDescriptor() {
	return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link JbpmUrlResourceBuilder}. Used as a singleton. The
     * class is marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl extends
	    BuildStepDescriptor<Builder> {

	// global configuration fields, persisted by default
	private boolean disableCliConsoleLogging;
	private boolean disableWebConsoleLogging;

	/**
	 * Performs on-the-fly validation of the form field 'url'.
	 * 
	 * @param value
	 *            This parameter receives the url string.
	 * @return Indicates the outcome of the validation. This is sent to the
	 *         browser.
	 */
	public FormValidation doCheckUrl(@QueryParameter String value) {
	    if (value.length() == 0) {
		return FormValidation.error("Please specify a valid URL.");
	    }
	    try {
		URL url = new URL(value);
		URLConnection conn = url.openConnection();
		conn.connect();
	    } catch (MalformedURLException e) {
		return FormValidation.error("The URL is not in a valid form.");
	    } catch (IOException e) {
		return FormValidation
			.error("The connection could not be established to the specified URL.");
	    }
	    return FormValidation.ok();
	}

	/**
	 * Performs on-the-fly validation of the form field 'processId'.
	 * 
	 * @param value
	 *            This parameter receives the processId string.
	 * @return Indicates the outcome of the validation. This is sent to the
	 *         browser.
	 */
	public FormValidation doCheckProcessId(@QueryParameter String value) {
	    if (value.length() == 0) {
		return FormValidation
			.error("Please specify a valid process ID.");
	    }
	    return FormValidation.ok();
	}

	/**
	 * Indicates that this builder can be used with all kinds of project
	 * types.
	 */
	public boolean isApplicable(Class<? extends AbstractProject> aClass) {
	    return true;
	}

	/**
	 * This human readable name is used in the configuration screen when
	 * selecting a new build step.
	 */
	public String getDisplayName() {
	    return "Invoke a jBPM business process";
	}

	/**
	 * Saves global configuration.
	 */
	@Override
	public boolean configure(StaplerRequest req, JSONObject formData)
		throws FormException {
	    disableCliConsoleLogging = formData
		    .getBoolean("disableCliConsoleLogging");
	    disableWebConsoleLogging = formData
		    .getBoolean("disableWebConsoleLogging");
	    save();
	    return super.configure(req, formData);
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
