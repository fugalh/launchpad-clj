(ns examples.palette
  (:require [launchpad.core :as lp]))

(def current-color (atom [0 3]))

(def pad (lp/get-launchpad))

(.update pad #(assoc %
                     :top
                     [[0 3] [3 0] [3 3] [0 0] [0 0] [0 0] [0 0] [0 0]]))
                    
(.react pad (fn [state what where vel]
              (case what
                :grid
                (case (get-in state [what where])
                  @current-color
                  (assoc-in state [what where] [0 0])
                  
                  (assoc-in state [what where] @current-color))

                :top
                (case where
                  0
                  (reset! current-color [0 3])
                  
                  1
                  (reset! current-color [3 0])
                  
                  2
                  (reset! current-color [3 3])

                  (reset! current-color [0 0]))

                state)))
