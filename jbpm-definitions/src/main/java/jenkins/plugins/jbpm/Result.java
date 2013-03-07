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

/**
 * 
 * Represents result state of the Jenkins job.
 * 
 */
public class Result {
    static int SUCCESS = 0;
    static int UNSTABLE = 1;
    static int FAILURE = 2;
    static int NOT_BUILT = 3;
    static int ABORTED = 4;
    /**
     * NULL does not exist in Jenkins, used only to describe uninitialized
     * states.
     */
    static int NULL = 5;

    static String[] results = { "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT",
            "ABORTED" };

    /**
     * Returns string representation of the Jenkins build result.
     */
    public static String toString(int code) {
        if (code >= SUCCESS && code <= ABORTED)
            return results[code];
        else
            return "NULL";
    }
}
