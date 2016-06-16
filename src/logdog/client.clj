(ns logdog.client
  "Namespace for handling all the RESTful calls to the other services with the
  session maintenance being done by the middleware and picking it up here."
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.tools.logging :refer [error errorf info infof warnf debugf]]
            [logdog.session :refer [get-session set-session! pull-session]]
            [logdog.util :refer [update-existing-keys parse-string safe-merge]]))

;; ## Simplified HTTP requests for the obvious calls

(defn mk-hdrs
  "Function to build up the headers for a RESTful call based on the map of
  data passed in. If the map is the `session data`, then build up the right
  headers based on this data. If not, then assume the map *is* the complete
  map of headers, and leave it alone. This allows folks to pass in either
  the session data or the complete headers and it Just. Works."
  [m]
  (if (and (map? m) (contains? m :x-session-id) (contains? m :x-request-id))
    (update-existing-keys m [:x-session-id :x-request-id] str)
    m))

(defn do-get*
  "Function to use the connection manager to save time, but NOT retry
  if we fail on getting the data - we just don't have the time. If we get
  something, then we need to parse it and return the body of the response to
  the caller. But if we exception, then we expect it to be caught by the
  calling scope."
  [cm url & [opts]]
  (when (and cm url)
    (let [resp (http/get url (safe-merge {:connection-manager cm
                                          :headers (mk-hdrs (get-session))
                                          :socket-timeout 10000
                                          :conn-timeout 2000}
                                         opts))]
      (set-session! (pull-session resp))
      (parse-string (:body resp)))))

(defn do-get
  "Function to use the connection manager to save time, but NOT retry
  if we fail on getting the data - we just don't have the time. If we get
  something, then we need to parse it and return the body of the response to
  the caller. This version catches the exceptions and logs them."
  [cm url & [opts]]
  (when (and cm url)
    (try
      (do-get* cm url opts)
      (catch Throwable t
        (infof t "Unable to hit '%s' for data!" url)))))

(defn do-post*
  "Function to use the connection manager to save time, but NOT retry
  if we fail on getting the data - we just don't have the time. If we get
  something, then we need to parse it and return the body of the response to
  the caller. But if we exception, then we expect it to be caught by the
  calling scope."
  [cm url body & [opts]]
  (when (and cm url)
    (let [resp (http/post url (safe-merge {:body (json/generate-string body)
                                           :headers (mk-hdrs (get-session))
                                           :content-type :json
                                           :accept :json
                                           :connection-manager cm
                                           :socket-timeout 10000
                                           :conn-timeout 2000}
                                          opts))]
      (set-session! (pull-session resp))
      (parse-string (:body resp)))))

(defn do-post
  "Function to use the connection manager to save time, but NOT retry
  if we fail on getting the data - we just don't have the time. If we get
  something, then we need to parse it and return the body of the response to
  the caller. This version catches the exceptions and logs them."
  [cm url body & [opts]]
  (when (and cm url)
    (try
      (do-post* cm url body opts)
      (catch Throwable t
        (infof t "Unable to post '%s' for data!" url)))))

(defn do-put*
  "Function to use the connection manager to save time, but NOT retry
  if we fail on getting the data - we just don't have the time. If we get
  something, then we need to parse it and return the body of the response to
  the caller. But if we exception, then we expect it to be caught by the
  calling scope."
  [cm url body & [opts]]
  (when (and cm url)
    (let [resp (http/put url (safe-merge {:body (json/generate-string body)
                                          :headers (mk-hdrs (get-session))
                                          :content-type :json
                                          :accept :json
                                          :connection-manager cm
                                          :socket-timeout 10000
                                          :conn-timeout 2000}
                                         opts))]
      (set-session! (pull-session resp))
      (parse-string (:body resp)))))

(defn do-put
  "Function to use the connection manager to save time, but NOT retry
  if we fail on getting the data - we just don't have the time. If we get
  something, then we need to parse it and return the body of the response to
  the caller. This version catches the exceptions and logs them."
  [cm url body & [opts]]
  (when (and cm url)
    (try
      (do-put* cm url body opts)
      (catch Throwable t
        (infof t "Unable to put '%s' for data!" url)))))

(defn do-delete*
  "Function to use the connection manager to save time, but NOT retry
  if we fail on getting the data - we just don't have the time. If we get
  something, then we need to parse it and return the body of the response to
  the caller. But if we exception, then we expect it to be caught by the
  calling scope."
  [cm url & [body opts]]
  (when (and cm url)
    (let [resp (http/delete url (safe-merge {:body (if body (json/generate-string body))
                                             :headers (mk-hdrs (get-session))
                                             :content-type :json
                                             :accept :json
                                             :connection-manager cm
                                             :socket-timeout 10000
                                             :conn-timeout 2000}
                                            opts))]
      (set-session! (pull-session resp))
      (parse-string (:body resp)))))

(defn do-delete
  "Function to use the connection manager to save time, but NOT retry
  if we fail on getting the data - we just don't have the time. If we get
  something, then we need to parse it and return the body of the response to
  the caller. This version catches the exceptions and logs them."
  [cm url & [body opts]]
  (when (and cm url)
    (try
      (do-delete* cm url body opts)
      (catch Throwable t
        (infof t "Unable to delete '%s'!" url)))))
