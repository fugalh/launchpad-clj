(ns 'launchpad.test
  (:require '[launchpad.core :as lp]))

;; If you don't have your launchpad handy you can still 
(defn make-test-model []
  (lp/Model. (atom (make-state))
          (reify javax.sound.midi.Receiver
            (close [this])
            (send [this msg ts]))))
