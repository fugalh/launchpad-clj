(ns launchpad.reactor
  (:require midi)
  (:import (javax.sound.midi Receiver Transmitter MidiMessage ShortMessage)))

(defrecord Message [type loc vel])
(defprotocol Reactor
  (react [this msg]
         "React to a Message. Examples:
         [:grid [x y] velocity]
         [:top x velocity]
         [:right y velocity]"))


(defn translate-message [msg]
  (when (instance? ShortMessage msg)
    (let [cmd (.getCommand msg)
          d1 (.getData1 msg)
          x (bit-and d1 0xf)
          y (bit-shift-right d1 4)
          t (- d1 0x68)
          d2 (.getData2 msg)]
        (case cmd
          0x90 ; ShortMessage/NOTE_ON
          (when (< y 8)
            (cond (= x 8)
                  (Message. :right y d2)

                  (< x 8)
                  (Message. :grid [x y] d2)))

          0xb0 ; ShortMessage/CONTROL_CHANGE
          (if (< t 8) (Message. :top t d2)
            ["meh" t d2])

          ["mrr" cmd d1 x y t d2]))))

(defn link [^Transmitter source
            sink]
  (.setReceiver source
     (reify Receiver
       (close [this])
       (send [this mmsg ts]
         (let [msg (translate-message mmsg)]
           (when msg (.react sink msg)))))))
