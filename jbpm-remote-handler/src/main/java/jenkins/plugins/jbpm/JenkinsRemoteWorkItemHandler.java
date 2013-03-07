/*
 * The MIT License
 *
 * Copyright (c) 2013, Jiri Svitak
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

package jenkins.plugins.jbpm;

import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.drools.command.Context;
import org.drools.command.impl.GenericCommand;
import org.drools.command.impl.KnowledgeCommandContext;
import org.drools.process.instance.impl.WorkItemImpl;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.NodeInstance;
import org.drools.runtime.process.NodeInstanceContainer;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jbpm.workflow.instance.node.WorkItemNodeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Invokes Jenkins job on a remote Jenkins server. Job name is determined from the Jenkins task node name.
 * Handler supports parameterized jobs and returns code of the remote job outcome. 
 *
 */
public class JenkinsRemoteWorkItemHandler implements WorkItemHandler {

    private static Logger logger = LoggerFactory
            .getLogger(JenkinsRemoteWorkItemHandler.class);

    private StatefulKnowledgeSession session;
    
    public JenkinsRemoteWorkItemHandler(StatefulKnowledgeSession session) {
        this.session = session;
    }
    
    public void executeWorkItem(WorkItem workItem,
            WorkItemManager workItemManager) {

        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(
                    "jbpm-remote-handler.properties"));
            String jenkinsUrl = properties.getProperty("jenkins.url", "http://localhost:8080/jenkins");
            String jobName = getNameFromSession(workItem);
            
            ClientRequest request = new ClientRequest(jenkinsUrl + "/job/" + jobName + "/build");
            request.accept("application/json");
            ClientResponse<String> response = request.get(String.class);
            if (response.getStatus() != 200) {
                throw new RuntimeException(
                        "Failed to start job on remote Jenkins server: "
                                + response.getStatus());
            }

        } catch (Exception e) {
            logger.error(e.toString());
            workItemManager.abortWorkItem(workItem.getId());
        }

        Map<String, Object> workItemResults = new HashMap<String, Object>();
        workItemResults.put("result", Result.SUCCESS);
        workItemManager.completeWorkItem(workItem.getId(), workItemResults);

    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {

    }

    private String getNameFromSession(final WorkItem workItem) {
        return session.execute(new GenericCommand<String>() {
            private static final long serialVersionUID = 1L;

            public String execute(Context context) {
                StatefulKnowledgeSession ksession = ((KnowledgeCommandContext) context)
                        .getStatefulKnowledgesession();
                ProcessInstance processInstance = ksession
                        .getProcessInstance(((WorkItemImpl) workItem)
                                .getProcessInstanceId());
                NodeInstance nodeInstance = findWorkItemNodeInstance(workItem
                        .getId(), ((WorkflowProcessInstance) processInstance)
                        .getNodeInstances());
                return nodeInstance.getNodeName();
            }

        });
    }

    private static WorkItemNodeInstance findWorkItemNodeInstance(
            long workItemId, Collection<NodeInstance> nodeInstances) {
        for (NodeInstance nodeInstance : nodeInstances) {
            if (nodeInstance instanceof WorkItemNodeInstance) {
                WorkItemNodeInstance workItemNodeInstance = (WorkItemNodeInstance) nodeInstance;
                if (workItemId == workItemNodeInstance.getWorkItem().getId()) {
                    return workItemNodeInstance;
                }
            }
            if (nodeInstance instanceof NodeInstanceContainer) {
                WorkItemNodeInstance result = findWorkItemNodeInstance(
                        workItemId,
                        ((NodeInstanceContainer) nodeInstance)
                                .getNodeInstances());
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
    
}
