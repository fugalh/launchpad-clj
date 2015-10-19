(ns examples.kitt
  (:require [launchpad.core :as lp])
  (:gen-class))

(defn dim
  "Dim the row, e.g. 3 becomes 2 and 0 stays 0."
  [row]
  (vec (map #(max 0 (dec %)) row)))

(defn next-posdir
  "Returns [pos dir] for the next position and direction."
  [pos dir]
  (let [ops {:right +, :left -}
        pos ((ops dir) pos 1)]
    (cond
      (< pos 0)
      [1 :right]

      (> pos 7)
      [6 :left]

      :else
      [pos dir])))

(defn render
  "Row (vector of red intensity) to launchpad state"
  [row state]
  (assoc state :grid
         (reduce merge {}
                 (map (fn [x v] {[x 0] [v 0]})
                      (range 8) row))))

(defn calc-dur
  "The duration in milliseconds for one step"
  [bpm]
  (* 1000 ; to milliseconds
     60 ; to seconds
     (/ 1.0 ; to minutes
        bpm
        8))) ; per step

(defn run
  [bpm]
  (let [ms (calc-dur bpm)
        pad (lp/get-launchpad)]
    (.reset pad)
    (loop [row (vec (repeat 8 0))
           [pos dir] [0 :right]]
      (let [row (dim row)
            row (assoc row pos 3)]
        (.update pad (partial render row))
        (Thread/sleep ms)
        (recur row (next-posdir pos dir))))))

(defn -main
  ([] (run 100))
  ([bpm] (run (read-string bpm))))

