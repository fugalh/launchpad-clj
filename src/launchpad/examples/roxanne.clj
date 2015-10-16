; lein run -m launchpad.examples.roxanne

(ns launchpad.examples.roxanne
  (:require [launchpad.core :as lp])
  (:gen-class))

(defn put-on-the-red-light
  [state what where vel]
  (let [red [3 0]]
    (when (= :grid what)
      (let [[x y] where]
        (.light state :grid where red)))))

(defn -main []
  (let [pad (lp/make-launchpad)
        red [3 0]]
    (when-not pad
      (throw (RuntimeException. "No Launchpad found")))
    (.react pad put-on-the-red-light)
    (println "You don't have to put on the red light!")
    ;; sleep forever (almost)
    (Thread/sleep Long/MAX_VALUE)))

