(ns logdog.middleware
  "Namespace of useful functions for handling the RESTful "
  (:require [clojure.tools.logging :refer [infof warn warnf error errorf]]
            [logdog.session :refer [pull-session reset-session! update-session!
                                    add-session get-session]]
            [logdog.util :refer [ucase safe-merge rename-keys]]))

(defn return-code
  "Creates a ring response for returning the given return code."
  [code]
  {:status code
   :headers {"Content-Type" "application/json; charset=UTF-8"
             "Access-Control-Allow-Origin" "*"}})

(defn wrap-logging
  "Ring middleware to log requests and exceptions. This includes setting
  the session id and request id from the request and making it available
  for the logging and sending out other RESTful requests."
  [handler & [sc]]
  (fn [req]
    (let [how (name (:request-method req))
          uri (:uri req)
          ip (:remote-addr req)
          sess (or (pull-session req) (get-session))
          sdm { :headers (-> (update-session! (if sc
                                                (if (fn? sc) (sc sess) sc)
                                                sess))
                             (rename-keys name)) }]
      (infof "Handling request: %s %s from: %s" (ucase how) uri ip)
      (try
        (add-session (handler (safe-merge req sdm)))
        (catch Throwable t
          (error t "Server error!")
          (return-code 500))
        (finally
          (reset-session!))))))
