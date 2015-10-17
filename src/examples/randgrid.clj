(ns examples.randgrid
  (:require [launchpad :refer :all]))

;; I haven't profiled it so I don't know why, but updating lights one
;; at a time is considerably slower than updating all the lights at
;; once with Launchpad's update method. And I haven't even implemented
;; any double-buffering etc. It could be clojure slowness, or an
;; interaction with Java's MIDI implementation. But whatever it is, it
;; means if you care about rapid updates you should dispatch the
;; entire state update at once

(defn rand-grid-state
  []
  (loop [s initial-state
         coords (for [x (range 8) y (range 8)] [x y])]
    (if-let [coord (first coords)]
      (let [c [(rand-int 4) (rand-int 4)]]
        (recur (grid s coord c) (rest coords)))
      s)))

(defn paint-grid-fast
  [pad]
  (let [s (rand-grid-state)]
    (.update pad s)))

(defn paint-grid-slow
  [pad]
  ;; Another danger is forgetting doall. Go ahead, remove it. See what happens.
  (doall                               
   (for [x (range 8) y (range 8)]
     (let [r (rand-int 4)
           g (rand-int 4)]
       (.light pad :grid [x y] [r g])))))

(defn -main []
  (let [pad (get-launchpad)]
    (.reset pad)
    (dotimes [x 4]
      (println "Fast")
      (paint-grid-fast pad)
      (Thread/sleep 500)
      
      (println "Slow")    
      (paint-grid-slow pad)
      (Thread/sleep 500)))
  (println "I guess you should hit ^C now.")) ; I'm not sure why it hangs?
