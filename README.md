
# logdog - Simplified Logging and Metrics Sending

In order to make performance logging simpler and streamline the capture of the
timing metrics, we have created a library that makes this all _very_ easy to do -
it's basically a little set-up, and then one function call. Simple.

We are using [Datadog](https://www.datadoghq.com/) to gather the performance
stats, and make them simple to view. They have a simple Agent that runs on the
box, that accepts UDP messages from processes on that box, and then packages
these updates up, and sends them to the back-end at
[Datadog](https://www.datadoghq.com/) for displaying. So we only really need
to interface with this process, and the Agent will take care of the rest.

## Usage

Add the necessary dependency to your project:
```clojure
[com.guaranteedrate/logdog "0.1.0"]
```
And setup your namespace import:
```clojure
(ns my-app
  (:require [logdog.logging :refer [log-execution-time!]]))
```

## Basic Usage

First, set up your host with [Datadog](https://www.datadoghq.com/), and the
Datadog Agent. Once that's done, it's time to simply include the proper
instrumentation, and the rest will take care of itself.

For instance, say you had a function like this:
```clojure
(defn pull-supers
  "Function to pull the superusers from the back-end. The output is a
  sequence of email addresses - just as they appear in the database table."
  []
  (db/query ["select user_email
                from cdoc_superusers"]
            :row-fn #(lcase (:user_email %))))
```
You could then instrument this function with the following line:
```clojure
(log-execution-time! pull-supers)
```
so that each time this function is called, a line would show up in the log
file:
```
[2016-02-22 10:58:37.826:qtp1754096028-26] INFO  trident.core - Finished pull-supers in 76ms.
```

Additionally, Datadog would receive a _Histogram_ metric for the execution time
under the name:
```
timing.trident.core.pull_supers
```
and that can be used with Datadog's internal stats functions to look at the mean,
or any of several percentiles for the time from one - or more, boxes in the group
that's running this code.

### Details

The details of the mapping of the function name to the Datadog metric name is
in the middle of `execution-time-logging-hook` in building the arguments to
`send-execution-time`. Datadog doesn't allow hyphens, so all hyphens and _bang_
marks are turned to underscores and shipped. It's inconvenient, but those are
the limitations of Datadog.

The installation and configuration of the Datadog agent is [here](http://docs.datadoghq.com/guides/basic_agent_usage/).
