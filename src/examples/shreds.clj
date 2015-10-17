;; example of doing Chuck-like shreds
;; http://chuck.cs.princeton.edu/

;; a shred is chuck-speak for a "fiber" or whatever you want to call it
;; (goroutine, go block, etc.) so light-row will return immediately but take
;; period seconds to actually light the row

;; this is a work in progress, a scratch pad while I figure out the most
;; chuck-y and clojure-y pattern to use for working with time

(ns examples.shreds
  (:require [launchpad :refer :all]
            [clojure.core.async :as a]))

;; This has to be a macro for go to stay in scope?
;; defn didn't work, at least. concerning :/
(defmacro wait [millis] `(a/<! (a/timeout ~millis)))

(defn light-row
  "shred to light the given row from left to right over the given period"
  [y color secs]
  (let [ms (* 1000 (/ secs 7))]
    (a/go-loop [x 0]
      (grid [x y] color)
       (when (< x 8)
         (wait ms)
         (recur (inc x))))))

(defn randomly-light-rows
  "Light all rows at the same time, with random time to completion"
  []
  (map #(let [red (rand-int 4)
              green (if red
                      (rand-int 4)
                      (inc (rand-int 3)))
              dur (* 10 (rand))
              y %]
          (light-row y [red green] dur))
       (range 8)))

(defn -main [] (randomly-light-rows))
