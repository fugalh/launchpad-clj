(ns launchpad.reactor-test
  (:require [clojure.test :refer :all]
            [launchpad.reactor :as r]
            midi)
  (:import javax.sound.midi.Transmitter))

(deftest translate-message
         (let [x (rand-int 8)
               y (rand-int 8)
               yy (* 0x10 y)
               xy (+ x yy)
               t (+ 0x68 x)
               grid (r/translate-message (midi/note-on xy 127))
               top (r/translate-message (midi/control-change t 127))
               right (r/translate-message (midi/note-on (+ 8 yy) 127))]

           (is grid)
           (is top)
           (is right)

           (is (= :grid (.type grid)))
           (is (= :top (.type top)))
           (is (= :right (.type right)))

           (is (= [x y] (.loc grid)))
           (is (= x (.loc top)))
           (is (= y (.loc right)))))

(defprotocol Finger
  (push [this m]))
(defrecord TestTx [rx]
  Transmitter
  (setReceiver [this rx]
    (reset! (.rx this) rx))
  Finger
  (push [this m]
    (.send @(.rx this) m -1)))

(deftest link
         (let [dev (TestTx. (atom nil))
               a (atom nil)]
           ; make sure the messages are being delivered
           (r/link dev #(reset! a %))
           (.push dev (midi/note-on 0 127))
           (is @a)

           ; grid
           (r/link dev #(is (= [3 2] (:loc %))))
           (.push dev (midi/note-on 0x23 127))

           ; side
           (r/link dev #(is (= [3 2] (:loc %))))
           (r/link dev #(is (= 1 (:loc %))))
           (.push dev (midi/note-on 0x18 127))

           ; top
           (r/link dev #(is (= 6 (:loc %))))
           (.push dev (midi/control-change (+ 6 0x68) 127))))


