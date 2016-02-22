(ns logdog.metrics
  "This is the code that handles the metrics and events through the Dropwizard
  Metrics core library, which, in turn, will ship it over UDP to the DataDog
  Agent running on localhost."
  (:require [clojure.tools.logging :refer [infof debugf warnf errorf]])
  (:import [com.codahale.metrics MetricRegistry]
           [org.coursera.metrics.datadog DatadogReporter]
           [org.coursera.metrics.datadog.transport UdpTransportFactory
                                                   UdpTransport]
           [java.util.concurrent TimeUnit]))

;; Create a simple MetricRegistry - but make it only when it's needed
(defonce def-registry
  (delay
    (let [reg (MetricRegistry.)
          udp (.build (UdpTransportFactory.))
          rpt (-> (DatadogReporter/forRegistry reg)
                (.withTransport udp)
                (.withHost "localhost")
                (.convertDurationsTo TimeUnit/MILLISECONDS)
                (.convertRatesTo TimeUnit/SECONDS)
                (.build))]
      (.start rpt 5 TimeUnit/SECONDS)
      reg)))

;; Somewhat faking java.jdbc's original *connection* behavior so that
;; we don't have to pass one around.
(def ^:dynamic *registry* nil)

(defn registry
  "Function to return either the externally provided MetricRegistry, or the
  default one that's constructed when it's needed, above. This allows the user
  the flexibility to live with the default - or make one just for their needs."
  []
  (or *registry* @def-registry))

;;
;; Functions to create/locate the different Metrics instruments available
;;

(defn meter
  "Function to return a Meter for the registry with the provided tag (a String)."
  [tag]
  (if (string? tag)
    (.meter (registry) tag)))

(defn counter
  "Function to return a Counter for the registry with the provided tag (a String)."
  [tag]
  (if (string? tag)
    (.counter (registry) tag)))

(defn histogram
  "Function to return a Histogram for the registry with the provided tag (a String)."
  [tag]
  (if (string? tag)
    (.histogram (registry) tag)))

(defn timer
  "Function to return a Timer for the registry with the provided tag (a String)."
  [tag]
  (if (string? tag)
    (.timer (registry) tag)))
