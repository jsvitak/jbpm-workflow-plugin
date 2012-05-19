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

/**
 * 
 * Provides simple logging functionality to the whole plugin.
 * 
 * @author Jiri Svitak
 *
 */
public class Logger {

    private static boolean cliConsoleEnabled;
    private static boolean webConsoleEnabled;
    private static BuildListener listener;
    private static final String prefix = "JBPM: "; 
    
    public static boolean isCliConsoleEnabled() {
	return cliConsoleEnabled;
    }

    public static void setCliConsoleEnabled(boolean cliConsoleEnabled) {
	Logger.cliConsoleEnabled = cliConsoleEnabled;
    }

    public static boolean isWebConsoleEnabled() {
	return webConsoleEnabled;
    }

    public static void setWebConsoleEnabled(boolean webConsoleEnabled) {
	Logger.webConsoleEnabled = webConsoleEnabled;
    }

    public static BuildListener getListener() {
	return listener;
    }

    public static void setListener(BuildListener listener) {
	Logger.listener = listener;
    }    
    
    public static synchronized void log(String message) {
	if (Logger.cliConsoleEnabled)
	    System.out.println(prefix + message);
	if (Logger.webConsoleEnabled)
	    listener.getLogger().println(prefix + message);
    }

}
