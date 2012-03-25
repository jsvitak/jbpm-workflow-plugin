package org.jenkinsci.plugins.jbpm;

import hudson.model.Cause;

public class WorkItemCause extends Cause {

	@Override
	public String getShortDescription() {
		return "jBPM work item invocation of Jenkins job";
	}

}
