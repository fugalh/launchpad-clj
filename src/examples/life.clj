;; Under Construction <img src="construction.gif">
;; Not up-to-date, pretend it doesn't exist yet.

(ns examples.life
  (:require [launchpad.core :as lp]))

(def pad (lp/get-launchpad))
(def update-hz 1)
(def paused (atom false))
(def alive [3 3])
(def dead [0 0])

(defn get-cell
  [state cell]
  (get-in state [:grid cell]))

(defn alive?
  [v]
  (= v alive))

(defn alive-in?
  [state cell]
  (alive? (get-in state [:grid cell])))

(defn count-living-neighbors
  [state [x y]]
  (loop [neighbors (for [xx [(- x 1) x (+ x 1)]
                  yy [(- y 1) y (+ y 1)]]
              [xx yy])
         n 0]
    (when-first [[xx yy] neighbors]
      (recur (rest neighbors)
             (if (and (not= [x y] [xx yy])
                      (< -1 xx 8) ; out of bounds is dead
                      (< -1 yy 8)
                      (alive-in? state [xx yy]))
               (+ n 1)
               n)))))
  
(defn tng
  "Life: The Next Generation #pundog"
  [state]
  (loop [state state
         cells (for [x (range 8) y (range 8)] [x y])]
    (when-first [cell cells]
      (case (count-living-neighbors state cell)
        3 alive

        2 (if (alive-in? state cell)
            alive
            dead)
        
        dead))
    (recur state
           (rest cells))))

(defn grid-with
  "Create an x-by-y grid (vector of vectors) by repeatedly calling
  f with the coordinate [x y]"
  [x y f]
  (for [xx (range x) yy (range y)] (f [xx yy])))

(defn random-cells
  "one in freq cells will be alive (statistically speaking)"
  [freq]
  (assoc lp/initial-state :grid
         (grid-with 8 8 #(if (= 0 (rand-int freq))
                           alive
                           dead))))

(defn toggle-cell
  [state where]
  (update-in state [:grid where]
             #(if-not (alive? %)
                alive
                dead)))

(defn toggle!
  [a]
  (swap! a #(not %)))

(defn render-paused
  [state]
  (let [red [3 0]
        green [0 3]]
    (assoc-in state [:top 0] (if @paused
                               red
                               green))))

;; prepare
(doto pad
  (.update render-paused)
  (.render (random-cells 5))
  (.react
   (fn [state what where vel]
     (when (not= 0 vel)
       (cond
         (= [:top 0] [what where])
         (do (toggle! paused)
             (render-paused state))

         (= :grid what)
         (toggle-cell state where))))))

;; run
(defn run []
  (while true
    (Thread/sleep (/ 1000 update-hz))
    (when-not @paused (.update pad tng))))
