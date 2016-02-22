(ns logdog.logging
  "Logging utilities that include performance logging."
  (:require [clojure.tools.logging :refer [log]]
            [logdog.metrics :as dd]
            [robert.hooke :refer [add-hook]]))

(defn now [] (System/currentTimeMillis))

;;
;; Functions to handle efficient sending of data to Datadog/Metrics
;;

(defonce histos (atom {}))

(defn get-histo
  "Function to return a Metrics Histogram object for the active registry
  for the provided name. This will create one if it's not in the `histos`
  map, and then add it to the map for quicker retrieval next time."
  [tag]
  (if (string? tag)
    (or (get @histos tag)
        (let [h (dd/histogram tag)]
          (swap! histos assoc tag h)
          h))))

(defn send-execution-time
  "Function to take a Datadog/Metrics `tag` and elapsed time in msec and post
  it to a Metrics Histogram object for sending to Datadog. This is a simple
  way to get nice remote stats on the execution times of each call without
  having to grep the logs and accumulate the data yourself."
  [tag msec]
  (if (and (string? tag) (number? msec))
    (.update (get-histo tag) msec)))

(defn execution-time-logging-hook
  "Given a config map, returns a hook function that logs execution time."
  [{:keys [level func-name msg msg-fn ns] :or {level :info}}]
  (let [labeler (fn [msg]
                  (str func-name (if msg (str " [" msg "]"))))
        logf (fn [s & args]
               (log ns level nil (apply format s args)))]
    (fn [func & args]
      (let [start (now)]
        (try
          (let [ret (apply func args)
                time-taken (- (now) start)
                label (labeler
                       (cond msg msg
                             msg-fn (try (apply msg-fn ret args)
                                         (catch Throwable t (str "msg-fn error! " t)))
                             :else nil))]
            (send-execution-time (str "timing." ns "." func-name) time-taken)
            (logf "Finished %s in %dms." label time-taken)
            ret)
          (catch Throwable t
            (let [time-taken (- (now) start)]
              (logf "Error in %s after %dms (%s)." (labeler nil) time-taken (.getMessage t)))
            (throw t)))))))

(defmacro log-execution-time!
  "A macro for adding execution time logging to a named
  function. Simply call at the top level with the name of the function
  you want to wrap. As a second argument you may provide an options
  map with possible values:

    {
     :level  ;; defaults to :info
     :msg    ;; some string that is printed with the log messages
     :msg-fn ;; a function that will be called with the return value
             ;; and the arguments, and should return a message for
             ;; inclusion in the log
    }"
  ([var-name] `(log-execution-time! ~var-name {}))
  ([var-name opts]
     `(add-hook (var ~var-name)
                ::execution-time
                (execution-time-logging-hook
                 (assoc ~opts
                   :func-name '~var-name
                   ;; pass in the namespace so the log messages
                   ;; can have the appropriate namespace instead
                   ;; of logdog.logging
                   :ns ~*ns*)))))

