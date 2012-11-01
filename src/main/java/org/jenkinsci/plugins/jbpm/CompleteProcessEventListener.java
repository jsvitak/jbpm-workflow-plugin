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