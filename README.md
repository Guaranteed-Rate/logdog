
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

# logdog - Automatic Session Id Tracking

## Setup

Add the necessary dependency to your project:
```clojure
[com.guaranteedrate/logdog "0.2.0"]
```

In your `log4j.properties` file use `%X{sess-id}` and `%X{req-id}` in your `ConversionPattern`.

Example:
```
log4j.appender.C1.layout.ConversionPattern=[%d{yyyy-MM-dd HH:mm:ss.SSS}:%t:%X{sess-id}/%X{req-id}] %-5p %c - %m%n
```


## Usage

### Service definition

Set up the `wrap-logging` middleware in your Ring/Compojure service route definitions.

```clojure
(ns my-ns.server
  (:require [clojure.tools.logging :refer [infof warn warnf error errorf]]
            [compojure
             [core :refer [defroutes POST]]]
            [logdog.middleware :refer [wrap-logging]]))

(defroutes app-routes
  (POST "/my/service" [:as {body :body}]
    ;; process body
    ))

(def app
  "The actual ring handler that is run -- this is the routes above
   wrapped in various middlewares."
  (-> app-routes
      ;; ...
      ;; other middleware
      ;; ...
      wrap-logging))
```

### Calling other services

Use the `logdog.client` namespace to call services.

```clojure
(ns my-ns.core
  (:require [clj-http.conn-mgr :as conn]
            [clojure.tools.logging :refer [infof warn warnf error errorf]]
            [logdog.client :refer [do-post*]]))

(defonce cm (conn/make-reusable-conn-manager {:timeout 120
                                              :threads 20
                                              :default-per-route 6}))

(defn service-call
  [data]
  (if (map? data)
    (let [ep "https://my.endpoint.com"
          resp (do-post* cm ep data)]
      (if resp
        {:status "OK" :body (if (= "null" resp) nil resp)}
        {:status "Error"}))))

```

If you want to overtly set a session-id and request-id then set the header in your request.
You can do this by passing a map as the 4th argument to the do-post* call.

Ex.
```clojure
(do-post* cm ep data {:headers {:x-session-id "klaatu-barada-nikto" :x-request-id 1}})
```

Ex. (JavaScript)
```javascript
  $.ajaxSetup({
    beforeSend: function (xhr)
    {
      xhr.setRequestHeader("x-session-id", "thx-1138");
      xhr.setRequestHeader("x-request-id", 1);
    }
  });

```
