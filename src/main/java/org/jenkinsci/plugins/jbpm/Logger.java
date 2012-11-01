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
