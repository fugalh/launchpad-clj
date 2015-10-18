; lein run -m examples.roxanne

(ns examples.roxanne
  (:require [launchpad :as lp])
  (:gen-class))

(defn put-on-the-red-light
  [state what where vel]
  (let [red [3 0]]
        (.light state what where red)))

(defn -main []
  (let [pad (lp/get-launchpad)
        red [3 0]]
    (when-not pad
      (throw (RuntimeException. "No Launchpad found")))
    (.react pad put-on-the-red-light)
    (println "You don't have to put on the red light!")
    ;; sleep forever (almost)
    (Thread/sleep Long/MAX_VALUE)))

