;;; Conway's Game of Life
;;; The board is initialized randomly and the game is running.
;;; Toggle cells in the obvious way.
;;; The top left round button is the pause button.

;;; If you struggle with thinking functionally, make an exercise of reading
;;; this code. Notice how the problem is broken into pieces and functional
;;; idioms like reduce glue everything together.

(ns examples.life
  (:require [launchpad.core :as lp])
  (:gen-class))

(def paused (atom false))
(def alive [3 3])
(def dead [0 0])
(def all-cells (for [x (range 8)
                     y (range 8)] [x y]))

(defn get-cell
  "get the value of this cell from state"
  [state cell]
  (get-in state [:grid cell]))

(defn alive?
  ([state [x y]]
   "Is this cell alive in this state? Out-of-bounds is dead."
  (and (<= 0 x 7)
       (<= 0 y 7)
       (alive? (get-cell state [x y]))))
  ([[r g]]
   "Is this an alive cell value?"
   (= alive [r g])))

(defn neighbors
  "Return a sequence of my 8 neighbors. Including out-of-bounds neighbors."
  [[x y]]
  (filter #(not= [x y] %)
          (for [xo (range -1 2)
                yo (range -1 2)]
            [(+ x xo) (+ y yo)])))

(defn count-living-neighbors
  [state [x y]]
  (count (filter (partial alive? state)
                 (neighbors [x y]))))

;; This is a good one to grok.
(defn grid-with
  "Create an 8x8 grid {coord -> color} by repeatedly calling
  f with the coordinate [x y]"
  [f]
  (reduce merge {}
          (for [cell all-cells]
            {cell (f cell)})))

(defn tng
  "Life: The Next Generation #pundog"
  [state]
  (assoc state :grid
         (grid-with (fn [cell]
                      (let [c (count-living-neighbors state cell)]
                        (cond
                          (and (alive? state cell)
                               (<= 2 c 3))
                          alive

                          (= c 3)
                          alive

                          :else
                          dead))))))

(defn random-cells
  "one in freq cells will be alive (statistically speaking)"
  [density]
  (assoc lp/initial-state :grid
         (grid-with (fn [_] (if (= 0 (rand-int density))
                              alive
                              dead)))))

(defn toggle-cell
  "Return a new state with this cell toggled."
  [state where]
  (update-in state [:grid where]
             #(if-not (alive? %)
                alive
                dead)))

(defn toggle!
  [a]
  (swap! a #(not %)))

(defn render-paused
  "Return a new state with the pause button rendered."
  [state]
  (let [red [3 0]
        green [0 3]]
    (assoc-in state [:top 0] (if @paused
                               red
                               green))))

(defn reactor
  [state what where vel]
  (when (not= 0 vel)
    (cond
      (= [:top 0] [what where])
      (do (toggle! paused)
          (render-paused state))

      (= :grid what)
      (toggle-cell state where))))

(defn prepare-pad [density]
  (let [pad (lp/get-launchpad)]
    (doto pad
      (.render (random-cells density))      
      (.update render-paused)
      (.react reactor))))  

(defn run [update-hz density]
  (let [pad (prepare-pad density)]
    (while true
      (Thread/sleep (/ 1000 update-hz))
      (when-not @paused (.update pad tng)))))

(defn -main
  ([] (run 3 3))
  ([hz] (run (read-string hz) 3))
  ([hz density] (run (read-string hz) (read-string density))))
   
          
