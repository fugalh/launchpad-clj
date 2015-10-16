(ns launchpad.core-test
  (:require [clojure.test :refer :all]
            [launchpad.core :as lp]))

(deftest decode-midi-test
  (testing "grid"
    (is (= [:grid [2 3] 127]
           (lp/decode-midi (midi/note-on 0x32 127))))
    (testing "out of bounds"
      (is (nil? (lp/decode-midi (midi/note-on 9 127))))
      (is (nil? (lp/decode-midi (midi/note-on 0x29 127))))))
  (testing "top"
    (is (= [:top 4 0]
           (lp/decode-midi (midi/control-change 0x6c 0))))
    (testing "out of bounds"
      (is (nil? (lp/decode-midi (midi/control-change 0x67 0))))
      (is (nil? (lp/decode-midi (midi/control-change 0x70 0))))))
  (testing "side"
    (is (= [:side 7 127]
           (lp/decode-midi (midi/note-on 0x78 127))))))

(defn midi-eq? [a b]
  (is (apply = (map (fn [m] [(.getStatus m)
                             (.getData1 m)
                             (.getData2 m)])
                    [a b]))))

(deftest encode-midi-test
  (testing "grid"
    (midi-eq? (lp/encode-midi :grid [6 2] [3 0])
              (midi/note-on 0x26 0xf))
    (midi-eq? (lp/encode-midi :grid [0 1] [0 3])
              (midi/note-on 0x10 0x3c))
    (testing "top"
      (midi-eq? (lp/encode-midi :top 2 [1 1])
                (midi/control-change 0x6a 0x1d)))
    (testing "side"
      (midi-eq? (lp/encode-midi :side 7 [3 2])
                (midi/note-on 0x78 0x2f)))))

(deftest state-test
  (let [x (rand-int 8)
        y (rand-int 8)
        red (rand-int 4)
        green (rand-int 4)
        coord [x y]
        color [red green]
        state (-> lp/initial-state
                  (.light :grid coord color)
                  (.light :top x color)
                  (.light :side y color))]
    (testing "light"
      (map #(is (= (get-in state %)))
           [[:grid x y] [:top x] [:side y]]))
    (testing "unlight"
      (let [state (-> state
                      (.unlight :grid coord)
                      (.unlight :top x)
                      (.unlight :side y))]
        (map #(is (= [0 0] (get-in state %)))
             [[:grid x y] [:top x] [:side y]])))
    (testing "react"
      (let [f (fn [state what where val])
            state (.react state f)]
        (is (= f (.reactor state)))))
    (testing "state"
      (is (= state (.state state))))))

(defrecord MidiThru [receiver]
  javax.sound.midi.Transmitter
  (setReceiver [this rx] (reset! receiver rx))
  javax.sound.midi.Receiver
  (send [this msg ts]
    (when @receiver
      (.send @receiver msg ts))))

(defn make-midi-thru []
  (MidiThru. (atom nil)))
  
(deftest link-test
  (let [msgs (atom [])
        pad (.react lp/initial-state
                    (fn [state what where val]
                      (swap! msgs conj [what where val])
                      nil))
        midi-thru (make-midi-thru)
        msg (midi/note-on 0x13 0xf)]
    (lp/link-midi->pad midi-thru pad)
    (.send midi-thru msg -1)
    (is (= [(lp/decode-midi msg)] @msgs))))
