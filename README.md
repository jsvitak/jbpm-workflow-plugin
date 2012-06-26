jBPM 5 workflow plugin for Jenkins CI
=====================================
This plugin allows you to define order of launching of your Jenkins jobs
by using [jBPM](http://www.jboss.org/jbpm) workflow.
jBPM is a flexible lightweight business process suite based on the BPMN 2.0 standard.

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
created first. For this purpose it is recomended to install and use jBPM. This suite contains
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

Setting up a Jenkins job
========================
It is assumed that you already have some jobs in your Jenkins. To control their launching using
business process you have to create a free style Jenkins job and add a new build step
*Invoke a jBPM business process*. In this build step you have to specify a URL
to your business process definition. To ease this, see attached help with a sample URL, which
points to Guvnor location at `localhost` server.
Also it is necessary to fill in a process identifier. It is useful to adhere a convention
like: `my.package.com.process1`.


