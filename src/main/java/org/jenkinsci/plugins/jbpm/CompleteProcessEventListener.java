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

import java.util.concurrent.CountDownLatch;

import org.drools.event.process.ProcessCompletedEvent;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.process.ProcessNodeLeftEvent;
import org.drools.event.process.ProcessNodeTriggeredEvent;
import org.drools.event.process.ProcessStartedEvent;
import org.drools.event.process.ProcessVariableChangedEvent;

/**
 * 
 * Implements a listener that uses a latch to wait for the end of the business process.
 * 
 * @author Jiri Svitak
 *
 */
public class CompleteProcessEventListener implements ProcessEventListener {
    
    private CountDownLatch latch;
    
    public CompleteProcessEventListener(CountDownLatch latch) {
        this.latch = latch;
    }    
    
    public void beforeProcessStarted(ProcessStartedEvent event) {
    }

    public void afterProcessStarted(ProcessStartedEvent event) {
        
    }

    public void beforeProcessCompleted(ProcessCompletedEvent event) {
        latch.countDown();        
    }

    public void afterProcessCompleted(ProcessCompletedEvent event) {             

    }

    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {

    }

    public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
  
    }

    public void beforeNodeLeft(ProcessNodeLeftEvent event) {

    }

    public void afterNodeLeft(ProcessNodeLeftEvent event) {

    }

    public void beforeVariableChanged(ProcessVariableChangedEvent event) {
        
    }

    public void afterVariableChanged(ProcessVariableChangedEvent event) {
        
    }

}