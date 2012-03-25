package org.jenkinsci.plugins.jbpm;

import hudson.model.AbstractProject;
import hudson.model.Hudson;

import java.util.concurrent.Future;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;

public class JenkinsJobWorkItemHandler implements WorkItemHandler {

	public void executeWorkItem(WorkItem workItem, WorkItemManager workItemManager) {

		
		//((Run) hudson.model.Hudson.getInstance().getItem(itemName))).scheduleBuild();
		//((AbstractBuild) (hudson.model.Hudson.getInstance().getItem(itemName))).
		
		String itemName = workItem.getName();
	
		WorkItemCause cause = new WorkItemCause();
		
		Future future = Hudson.getInstance().getItemByFullName(itemName, AbstractProject.class).scheduleBuild2(0, cause);

		try {
			future.wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
		
	public void abortWorkItem(WorkItem arg0, WorkItemManager arg1) {
		
	}

}
