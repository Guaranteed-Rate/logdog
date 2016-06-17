(ns logdog.util
  "Collection of utility functions that are either missing in clojure, or have
  some very nice default behaviors, but in all make writing code a lot simpler."
  (:require [cheshire.core :as json]
            [clojure.tools.logging :refer [error errorf info infof warnf debugf]]
            [clojure.string :as cs]))

(defn is-json-string?
  "Predicate function that looks at the provided argument, and if it's a string,
  does it start with '{' and end with '}' - and if it does, then we're going to
  say 'true', otherwise, false. This is going to be a key tool in the _deep_
  parsing of the JSON we get from many sources."
  [s]
  (if (and (string? s) (pos? (count s)))
    (let [fc (.charAt s 0)
          lc (.charAt s (dec (count s)))]
      (or (= \" fc lc)
          (and (= \{ fc) (= \} lc))
          (and (= \[ fc) (= \] lc))))
    false))

(defn- safe-parse-string
  "Function to safely parse the provided string by enclosing the code in a
  try/catch block and logging any parsing errors as opposed to letting it
  crash the system. That would be bad."
  [s & [keys?]]
  (cond
    (not (string? s))
    (do
      (debugf "JSON parse problem with: %s => it's not a string!" s)
      s)
    (empty? s)
    nil
    (not (is-json-string? s))
    s
    :else
    (try
      (let [ans (json/parse-string s keys?)]
        ;; certain data causes unusual exception in Jackson, this makes
        ;; sure that when those occur, we return the string unmodified.
        (if (and (not= "[]" s) (seq? ans) (empty? ans))
          s
          ans))
      (catch com.fasterxml.jackson.core.JsonParseException jpe
        (debugf "JSON parse exception on: %s => %s" s (.getMessage jpe))
        s))))

(defn parse-string
  "Function to mimic the cheshire 'parse-string' but because of the way that a
  lot of the data in nested maps is encoded as strings, we need to have an option
  to _deep_ decode the incoming string. That means that we need to decode the
  string, and look at all the resulting keys and values, and if any are strings
  starting with '{' and ending with '}' - then we repeat the process and replace
  that string with the deeply decoded map. This makes it possible to deal with
  a lot more of the JSON data we get around here."
  [s]
  (cond
    (empty? s) nil
    :else (let [m (safe-parse-string s true)]
            (cond
              (map? m)
              (into {} (for [[k v] m
                             :let [nk (if (is-json-string? k) (parse-string k) k)
                                   nv (if (is-json-string? v) (parse-string v) v)]]
                         [nk nv]))
              :else m))))

(defn update-existing-keys
  "Similar to update-keys (above), but operates only on existing keys
  in the map. Note this is different from update-in (though they have
  the same argument signature), which uses the list of keys as a path
  into the a nested data structure. For update-existing-keys, all the
  keys are top-level, and ignored if they arn't present."
  [m ks f & args]
  (reduce #(if (contains? %1 %2) (apply update %1 %2 f args) %1)
          m
          ks))

(defn deep-merge
  "Recursively merges maps. If vals are not maps, the last value wins. Every
  value needs to be non-nil or the result will be nil."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(def not-nil? (complement nil?))

(defn compact
  "Simple convenience function to filter the nils out of a collection, but leave
  everything else alone - even a nil. This is just like the ruby compact method,
  and it's really quite useful for sums and operations that will choke on nils."
  [s]
  (if (coll? s) (filter not-nil? s) s))

(defn safe-merge
  "Call `deep-merge` but first filter out all the `nil` arguments so that
  we don't let it force the output to `nil`."
  [& vals]
  (apply deep-merge (compact vals)))

(defn ucase
  "Function to convert the string argument to it's upper-case equivalent - but
  do so in a clean manner. This means that if it's a `nil`, you get back a `nil`,
  and if it's a number, you get that number back. This was left out of the spec
  in the original, and it makes for code like this to clean things up. If you
  pass in a collection, this function will call itself on all the values in that
  collection so you can upper-case a collection without worrying about the other
  types of values."
  [s]
  (cond
    (string? s) (cs/upper-case s)
    (coll? s)   (map ucase s)
    :else       s))

(defn rename-keys
  "This is completely compatible with `clojure.set/renme-keys`.
  But in addition to that behavior,
    - If the second arg is a map and the value of a mapping is a function then the
    funnction will be called with the key and the result will be used as the new key.
    - If the second arg is a function then all the keys of `map` will be transfermed
    by it.
    - If the second arg is sequential then only the keys in the sequence will be renamed
    according to the third argument, assumed to be a function (identity is used if
    there is not third argument)."
  [map & [km_f_s f]]
  (let [kmap (cond
               (map? km_f_s) km_f_s
               (fn? km_f_s) (zipmap (keys map) (repeat km_f_s))
               (sequential? km_f_s) (zipmap km_f_s (repeat (or f identity)))
               :else {km_f_s (or f identity)})]
    (reduce
      (fn [m [old new]]
        (let [fnew (if (fn? new) new (fn [_] new))]
          (if (contains? map old)
            (assoc m (fnew old) (get map old))
            m)))
      (apply dissoc map (keys kmap)) kmap)))
