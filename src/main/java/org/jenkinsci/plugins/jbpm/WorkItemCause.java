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

import hudson.model.Cause;

/**
 * 
 * Implements description of a cause, which is necessary for scheduling a job.
 * 
 * @author Jiri Svitak
 *
 */
public class WorkItemCause extends Cause {

	@Override
	public String getShortDescription() {
		return "jBPM work item invocation of Jenkins job";
	}

}
