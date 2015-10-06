(ns jon1
  (:require [launchpad.core :as launchpad]))
(def lp (launchpad/get-launchpad))
(def red (launchpad/mk-color 3 0))
(def green (launchpad/mk-color 0 3))
(def amber (launchpad/mk-color 3 3))
(launchpad/reset lp)

(defn light [pad x y col] (do
                           (launchpad/light pad x y col)
                           (wait 250)))

(defn wait [ms] (Thread/sleep ms))

(defn box [x y size color]
  (dotimes [xx size]
    (light lp (+ x xx) y color))
  (dotimes [yy size]
    (light lp (+ x size -1) (+ y yy) color))
  (dotimes [xx size]
    (light lp (+ x xx) (+ y size -1) color))
  (dotimes [yy size]
    (light lp x (+ yy y) color)))

(box 1 1 6 amber)
(box 0 0 8 red)
(box 2 2 4 green)
(box 3 3 2 red)
