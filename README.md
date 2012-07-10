jBPM 5 workflow plugin for Jenkins CI
=====================================
This plugin allows you to define an order of launching your Jenkins jobs
by using a [jBPM](http://www.jboss.org/jbpm) workflow.
The jBPM 5 is a flexible lightweight business process suite based on the BPMN 2.0 standard.

Quick start
===========
Plugin is built by Maven 3. It is also recommended to use Oracle JDK 6.

    $ mvn clean package -DskipTests

This will create `target` directory which contains our package `jbpm-workflow-plugin.hpi`.
This file needs to be deployed into `$JENKINS_HOME/plugins` directory. For testing
you can also download and launch testing Jenkins instance:

    $ mvn hpi:run -Djetty.port=8090

We should specify a different port than 8080, because
port 8080 will be likely used by jBPM applications like Drools Guvnor, Designer or jBPM console.

Business processes
==================
Plugin can be used to start BPMN 2.0 compliant business process definitions, but they have to be
created first. For this purpose it is recommended to install and use jBPM. This suite contains
web application Guvnor, which stores and makes available your business process definitions to your
Jenkins master. Guvnor is bundled with the Designer, a web tool for business process draft.

Integration of jBPM business process with Jenkins is done via service tasks (work items).
So you have to save your work item definition in Guvnor which describes interface of the new
service task type. Then it is possible to draft business processes and use the new tasks on the
canvas of the Designer. Service tasks represent asynchronous launch of Jenkins job.

While drafting a business processes you may use every jBPM feature. Tasks representing Jenkins jobs
are supplied with a job name and they return a job result. So make sure, that you have
* declared correctly all process variables
* set up input and output parameters of selected service task (it is not necessary
to declare all of them, only the ones which are really used)
* assigned/mapped constants and process variables to parameters of selected service task

Several examples are provided to see how to configure most typical use cases in the Designer:

Setting up a job name
---------------------
Select your work item of JenkinsJob type and go to properties panel on the right. Set a name,
it is useful to adhere a convention, so for example set the same name as a corresponding
job in Jenkins.
Open data input editor by clicking on *DataInputSet* field in *More Properties*. Add a new
data input type with name `jobName` and type `String`.

    jobName:String
   
Open assignments editor by clicking on *Assignments* field. You have to set *From Object*
as `jobName`, *Assignment type* as `is equal to` and *To Value* exactly as job name in your Jenkins
system.

    jobName=your-job-name
    
That is all, no other properties need to be specified.
Once the jBPM process is invoked (by launching Jenkins job
with jBPM plugin build step) and the process flow enters JenkinsJob work item, which was
already configured, then Jenkins job with name `jobName` will be launched.

Retrieving a job result
-----------------------
To be able to launch a Jenkins job, you have to specify a job name first. After that you are
able to retrieve a job result. First add a new import for Jenkins result
representation:

    hudson.model.Result

Add a process variable `result` of *Standard Type*
`Object` in the *Variable Definitions* field of the process *Properties* panel.
*Custom Type* `Result` is not necessary.

    result:Object

Select a JenkinsJob work item and open data output editor by clicking on *DataOutputSet* field
in *More Properties* of *Properties* panel. Add a new data output type with name `jobResult`
and type `Object`.

    jobResult:Object
    
Open assignments editor by clicking on *Assignments* field in *More properties* of *Properties*
panel. Set *From Object* as `jobResult`, *Assignment Type* as `is mapped to` and
*To Object* as `result`. Your final assignments might look like:

    TaskName=JenkinsJob,jobName=your-job-name,jobResult->result
   
Now you are able to evaluate results of finished Jenkins jobs. This can be done for example
in constraints of gateway branches. If the constraint expression is evaluated as true, then
the process flow continues in satisfied branches. This can be done in the following
example ways:

    return ((Result)(kcontext.getVariable("result"))).isBetterThan(Result.FAILURE);

or

    return ((Result)(kcontext.getVariable("result"))).isWorseOrEqualTo(Result.FAILURE);
    
If you do not want to specify branch constraints, then you can also pass
the `result` object for example to a human task to let the tester decide
in which flow branch the engine should continue.

Know that [Result](http://javadoc.jenkins-ci.org/hudson/model/Result.html)
is Jenkins class for representing result and contains static instances
of all result types - `SUCCESS`, `UNSTABLE`, `FAILURE`, `NOT_BUILT`, `ABORTED`. 
Their ordering is important. You
can easily compare against these result types using following methods:

    public boolean isWorseThan(Result that)
    public boolean isWorseOrEqualTo(Result that)
    public boolean isBetterThan(Result that)
    public boolean isBetterOrEqualTo(Result that)

Setting up a parameterized job
------------------------------
In the process configuration add several imports, just click on *Add Import* button and fill in:

    hudson.model.StringParameterValue
    java.util.List
    java.util.ArrayList

Also set process variables, at least variable `parameters` which is of type `Object`,
you do not have to set *Custom Type*.

    parameters:Object

Write a Java code which fills this `parameters` process variable with a data
that should be supplied to a parameterized Jenkins job.

    List params = new ArrayList();
    params.add(new StringParameterValue("VERSION", "5.3.0"));
    kcontext.setVariable("parameters", params);

Now the `parameters` will be a process variable of type `List` which can be mapped
to an input of a work item which launches a Jenkins job.
Since the jBPM workflow plugin uses simple Janino Java
compiler, you cannot use generics, like in the example above.


Setting up a Jenkins job driven by jBPM
=======================================
It is assumed that you already have some jobs in your Jenkins. To control their launching using
business process you have to create a free style Jenkins job and add a new build step
*Invoke a jBPM business process*. In this build step specify a URL
to your business process definition. To ease this, see attached help with a sample URL, which
points to Guvnor location at `localhost` server, but you cas easily change to your own
hostname.

It is also necessary to fill in a process identifier. It is useful to adhere a convention
like: `my.package.com.process1`.

TODO
====
* Modularization - to allow simple using of other user defined work items in their business processes
* Persistence - to allow persisting process session info, process instance info and work item statuses on the fly


