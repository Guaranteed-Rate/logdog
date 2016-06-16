(ns logdog.session
  "This namespace is all about the ring middleware session state for extracing
  and providing the session variables `session-id` and `request-id` from the
  client and through all calls to other RESTful clients."
  (:require [clojure.string :as cs]
            [clojure.set :refer [rename-keys]])
  (:import java.util.UUID
           org.slf4j.MDC))

(declare set-mdc!)

(defn ->int
  "Parses a string into an int, expecting \"Inf\" for infinity. A nil is parsed
  as 0 - similar to ruby's `to_i` method."
  [x]
  (cond
    (nil? x) 0
    (string? x) (cond
                  (empty? x) 0
                  :else (try
                          (Integer/parseInt (cs/trim x))
                          (catch java.lang.NumberFormatException nfe
                            0)))
    :else x))

(def ^:private session
  "This is the session map that will be set at the start of a call
  and then referenced in logging and subsequent outgoing calls as well"
  (ThreadLocal.))

(defn set-session!
  "Function to set the thread-local version of the session data for this
  request. This will be called in `wrap-session` to make sure that we have
  one, and that we pass it on to all the logging and RESTful call methods.
  This function returns the argument - after setting it in the thread-local
  storage."
  [m]
  (when m
    (.set session m)
    (set-mdc!))
  m)

(defn reset-session!
  "Simple convenience function to reset the session data as might be called
  when the RESTful call in the ring system is done processing. This then will
  clear out the session data for this thread and allow it to be reused with -
  or without session data."
  []
  (.set session nil)
  (set-mdc!))

(defn get-session
  "Function to get the thread-local session data so that it can be used in
  the logging and subsequent calls to the other RESTful services so that we
  can track this session data from service to service."
  []
  (.get session))

(defn update-session!
  "Ensures session is updated so that there is a :x-session-id
  and that the :x-request-id is incremented."
  [& [sd]]
  (let [sess (or sd (get-session))
        sid (or (:x-session-id sess) (:session-id sess) (UUID/randomUUID))
        rid (->int (or (:x-request-id sess) (:request-id sess)))]
    (-> sess
        (dissoc :session-id :request-id)
        (assoc :x-session-id sid :x-request-id (inc rid))
        (set-session!))))

(defn pull-session
  "Function to extract the session identifiers from the request so that we
  can carry on with the session ID that is passed to us from the caller."
  [req]
  (let [hdrs (:headers req)
        sid (not-empty (hdrs "x-session-id"))
        rid (not-empty (hdrs "x-request-id"))]
    (if (and sid rid)
      {:x-session-id sid, :x-request-id rid})))

(defn add-session
  "Function to add in the session ID and request ID from the thread-local
  storage into this response by merging them into the `:header` data that
  this response should already have."
  [req]
  (if (map? req)
    (let [sess (get-session)]
      (assoc req :headers (-> (:headers req)
                              (assoc "x-session-id" (str (:x-session-id sess))
                                     "x-request-id" (str (:x-request-id sess))))))))

(defn session-headers
  "Simple function to make the headers for the given session as a simple map for
  inclusion with the clj-http calls that are not in logdog.client."
  []
  (if-let [sess (get-session)]
    { "x-session-id" (str (:x-session-id sess))
     "x-request-id" (str (:x-request-id sess)) }))

(defn set-mdc!
  "Set the session and request ids in the MDC. Use this prior to calling
  any clojure.tools.logging log macro or function."
  []
  (let [{:keys [x-session-id x-request-id]} (get-session)
        sid (not-empty (str x-session-id))
        rid (not-empty (str x-request-id))]
    (MDC/put "sess-id" (or sid "-"))
    (MDC/put "req-id" (or rid "-"))))
