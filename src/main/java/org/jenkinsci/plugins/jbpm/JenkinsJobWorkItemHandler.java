package org.jenkinsci.plugins.jbpm;

import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.AbstractProject;
import hudson.model.Hudson;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;

public class JenkinsJobWorkItemHandler implements WorkItemHandler {

	private static BuildListener listener;
	
	public synchronized static void setListener(BuildListener _listener) {
		listener = _listener;
	}
	
	public synchronized static BuildListener getListener() {
		return listener;
	}
	
	public void executeWorkItem(WorkItem workItem,
			WorkItemManager workItemManager) {

		String jobName = (String) workItem.getParameter("JobNameToLaunch");
		getListener().getLogger().println("Starting job: " + jobName);
		
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
		
		Result result = ap.getBuilds().getLastBuild().getResult();
		Map<String,Object> results = new HashMap<String,Object> ();
		results.put("Result", result);
		
		workItemManager.completeWorkItem(workItem.getId(), results);
		getListener().getLogger().println("Completed job: " + jobName);
	}

	public void abortWorkItem(WorkItem arg0, WorkItemManager arg1) {

	}

}
