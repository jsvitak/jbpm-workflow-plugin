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

/**
 * 
 * Implementation of JenkinsJob work item, which launches jobs and waits for their completion.
 * 
 * @author Jiri Svitak
 *
 */
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

	// extract input variables
        String jenkinsJobName = (String) workItem.getParameter("jenkinsJobName");
        Map<String,Result> jenkinsJobResults = (Map<String,Result>) workItem.getParameter("jenkinsJobResultsInput");
        
        // jenkinsJobName
        getListener().getLogger().println("jenkinsJobName: " + jenkinsJobName);
        System.out.println("jenkinsJobName: " + jenkinsJobName);

        // work item id
        getListener().getLogger().println("work item id: " + String.valueOf(workItem.getId()));
        System.out.println("jenkinsJobName: " + String.valueOf(workItem.getId()));
        
        // jenkinsJobResults
        getListener().getLogger().println("jenkinsJobResultsInput: " + jenkinsJobResults);
        System.out.println("jenkinsJobResults: " + jenkinsJobResults);
        
        // start new job specified by jenkinsJobName
        Hudson h = Hudson.getInstance();
        AbstractProject ap = h
                .getItemByFullName(jenkinsJobName, AbstractProject.class);

        /*
        System.out.println("getItemByFullName: " + ap);
        TopLevelItem ap2 = h.getItem(jenkinsJobName);
        System.out.println("getItem: " + ap2);
        System.out.println("getItemMap" + h.getItemMap());
        System.out.println("getItems" + h.getItems());
	*/

        // schedule a build and wait for completion
        WorkItemCause cause = new WorkItemCause();
        Future future = ap.scheduleBuild2(0, cause);
        synchronized (future) {
            try {
                future.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        // add Jenkins job result to map of Jenkins job results
        Result result = ap.getBuilds().getLastBuild().getResult();
        getListener().getLogger().println("result: " + result);
        System.out.println("result: " + result);
        jenkinsJobResults.put(jenkinsJobName, result);
        
        // add Jenkins job results map to output variables map
        Map<String,Object> workItemResults = new HashMap<String,Object>();
        workItemResults.put("jenkinsJobResultsOutput", jenkinsJobResults);
        
        workItemManager.completeWorkItem(workItem.getId(), workItemResults);
        getListener().getLogger().println("Completed job: " + jenkinsJobName);
    }

    public void abortWorkItem(WorkItem arg0, WorkItemManager arg1) {

    }

}
