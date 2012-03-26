package org.jenkinsci.plugins.jbpm;

import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.AbstractProject;
import hudson.model.Hudson;

import java.util.concurrent.Future;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;

public class JenkinsJobWorkItemHandler implements WorkItemHandler {

    public void executeWorkItem(WorkItem workItem,
	    WorkItemManager workItemManager) {

	// ((Run)
	// hudson.model.Hudson.getInstance().getItem(itemName))).scheduleBuild();
	// ((AbstractBuild)
	// (hudson.model.Hudson.getInstance().getItem(itemName))).

	String itemName = workItem.getName();
	String jobName = (String) workItem.getParameter("JobNameToLaunch");

	WorkItemCause cause = new WorkItemCause();

	System.out.println("jobName: " + jobName);

	Hudson h = Hudson.getInstance();
	AbstractProject ap = h
		.getItemByFullName(jobName, AbstractProject.class);
	System.out.println("getItemByFullName: " + ap);

	TopLevelItem ap2 = h.getItem(jobName);
	System.out.println("getItem: " + ap2);

	System.out.println("getItemMap" + h.getItemMap());
	System.out.println("getItems" + h.getItems());

	Future future = ap.scheduleBuild2(0, cause);

	synchronized (future) {
	    try {
		future.wait();
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}

    }

    public void abortWorkItem(WorkItem arg0, WorkItemManager arg1) {

    }

}
