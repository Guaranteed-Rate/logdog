(ns logdog.main
  "Main (simple) client for the library. This doesn't do a lot (yet), but it's
  here in case we decide that we want it to do something in the future."
  (:require [clojure.tools.cli :refer [cli]]
            [clojure.tools.logging :refer [error info infof]])
  (:gen-class))

(defn- error-msg
  "Prints a format string to stderr and logs it."
  [fmt & args]
  (let [s (apply format fmt args)]
    (.println System/err (str "CLI error: " s))
    (error s)))

(defn wrap-error-handling
  [func]
  (try (func)
       (catch Throwable t
         (.println System/err (str "Error in main: " t))
         (error t "Error in main")
         (throw t))))

(defmacro with-error-handling
  [& body]
  `(wrap-error-handling (fn [] ~@body)))

(defn handle-args
  "Function to parse the arguments to the main entry point of this project and
  do what it's asking. By the time we return, it's all done and over."
  [args]
  (let [[params [target action]] (cli args
             ["-v" "--verbose" :flag true])]
    (info "Welcome to logdog!")
    (println "Welcome to logdog!")))

(defn -main
  "Function to kick off everything and clean up afterwards"
  [& args]
  (with-error-handling (handle-args args)))
