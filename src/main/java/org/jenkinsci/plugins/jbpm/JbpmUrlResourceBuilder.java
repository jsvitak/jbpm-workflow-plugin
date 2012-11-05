/*
 * The MIT License
 *
 * Copyright (c) 2012, Jiri Svitak
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CountDownLatch;

import net.sf.json.JSONObject;

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

        PluginLogger.setListener(listener);

        StatefulKnowledgeSession ksession = SessionUtil.getStatefulKnowledgeSession(SessionUtil.getKnowledgeBase(url));

        ksession.getWorkItemManager().registerWorkItemHandler("JenkinsJob",
                new JenkinsJobWorkItemHandler());
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
                new HornetQHTWorkItemHandler(ksession));

        CountDownLatch latch = new CountDownLatch(1);
        CompleteProcessEventListener processEventListener = new CompleteProcessEventListener(
                latch);
        ksession.addEventListener(processEventListener);

        PluginLogger.info("Started: " + processId);

        ksession.startProcess(processId);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        PluginLogger.info("Completed: " + processId);

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
