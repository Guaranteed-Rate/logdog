(defproject com.guaranteedrate/logdog "0.2.0"
  :description "Library for simplified logging, timing, and metrics collection"
  :url "http://github.com/Guaranteed-Rate/logdog"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src"]
  :min-lein-version "2.3.4"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 ;; command line option processing
                 [org.clojure/tools.cli "0.2.2"]
                 ;; logging with log4j
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [log4j/log4j "1.2.17"]
                 [org.clojure/tools.logging "0.2.6"]
                 [robert/hooke "1.3.0"]
                 ;; metrics and DataDog tools
                 [io.dropwizard.metrics/metrics-core "3.1.0" :exclusions [org.slf4j/slf4j-api]]
                 [org.coursera/dropwizard-metrics-datadog "1.0.2" :exclusions [com.fasterxml.jackson.core/jackson-databind
                                                                               org.slf4j/slf4j-api]]
                 ;; JSON parsing library
                 [cheshire "5.5.0"]
                 ;; nice HTTP client library
                 [clj-http "2.0.0"]]
  :aot [clojure.tools.logging.impl logdog.main]
  :main logdog.main)
