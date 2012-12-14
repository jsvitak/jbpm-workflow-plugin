package org.jenkinsci.plugins.jbpm;

import java.util.Collection;

import org.drools.process.instance.impl.WorkItemImpl;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.NodeInstance;
import org.drools.runtime.process.NodeInstanceContainer;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.jbpm.workflow.instance.node.WorkItemNodeInstance;

public class WorkItemHandlerUtil {

    private static StatefulKnowledgeSession session = null;
    
    public static void setSession(StatefulKnowledgeSession ksession) {
        session = ksession;
    }
    
    public static String getWorkItemName(WorkItem workItem) {
        return getNodeInstance(workItem).getNodeName();
    }
    
    public static long getProcessInstanceId(WorkItem workItem) {
        return ((WorkItemImpl) workItem).getProcessInstanceId();
    }
    
    public static ProcessInstance getProcessInstance(WorkItem workItem) {
        ProcessInstance processInstance = session.getProcessInstance(getProcessInstanceId(workItem));
        return processInstance;
    }
    
    public static NodeInstance getNodeInstance(WorkItem workItem) {
        ProcessInstance processInstance = getProcessInstance(workItem);
        if (!(processInstance instanceof WorkflowProcessInstance)) {
            return null;
        }
        return findWorkItemNodeInstance(workItem.getId(), ((WorkflowProcessInstance) processInstance).getNodeInstances());
    }
    
    private static WorkItemNodeInstance findWorkItemNodeInstance(long workItemId, Collection<NodeInstance> nodeInstances) {
        for (NodeInstance nodeInstance: nodeInstances) {
            if (nodeInstance instanceof WorkItemNodeInstance) {
                WorkItemNodeInstance workItemNodeInstance = (WorkItemNodeInstance) nodeInstance;
                if (workItemId == workItemNodeInstance.getWorkItem().getId()) {
                    return workItemNodeInstance;
                }
            }
            if (nodeInstance instanceof NodeInstanceContainer) {
                WorkItemNodeInstance result = findWorkItemNodeInstance(workItemId, ((NodeInstanceContainer) nodeInstance).getNodeInstances());
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

}
