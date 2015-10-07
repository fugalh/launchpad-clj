(ns launchpad.core
  (:require midi))

(defn- color->velocity [[red green]]
  (bit-or red
          0xC
          (bit-shift-left green 4)))

(defn- coord->note [[x y]]
  (+ x
     (* 0x10 y)))

(defprotocol ModelBehavior
  (render [this])
  (reset [this])
  (grid [this [x y] [red green]])
  (top [this x [red green]])
  (side [this y [red green]]))

(defrecord State [grid top side])
(defrecord Model [state device]
  ModelBehavior
  (grid [this [x y] [red green]]
    (let [state (.state this)]
      (swap! state assoc-in [:grid x y] [red green])))
  (top [this x [red green]]
    (let [state (.state this)]
      (swap! state assoc-in [:top x] [red green])))
  (side [this y [red green]]
    (let [state (.state this)]
      (swap! state assoc-in [:side y] [red green])))
  (render [this]
    ;; TODO more efficient
    (let [state @(.state this)
          dev (.device this)]
      (dotimes [x 8]
        (dotimes [y 8]
          (.send dev
             (midi/note-on (coord->note [x y])
                           (color->velocity (get-in state [:grid x y])))
             -1))
        (.send dev
           (midi/control-change (+ 0x68 x)
                 (color->velocity (get-in state [:top x])))
           -1)
        (.send dev
           (midi/note-on (coord->note [8 x])
                 (color->velocity (get-in state [:side x])))
           -1))))
  (reset [this]
    (do 
      (swap! (.state this) (fn [_] (make-state)))
      (.render this))))

(defn make-state []
  (State. (vec (repeat 8 (vec (repeat 8 [0 0]))))
         (vec (repeat 8 [0 0]))
         (vec (repeat 8 [0 0]))))

(defn make-model []
  (Model. (atom (make-state))
          (midi/get-receiver "Launchpad")))

(defn make-test-model []
  (Model. (atom (make-state))
          (reify javax.sound.midi.Receiver
            (close [this])
            (send [this msg ts]))))

;; wip - grid/top/side in terms of updating state and calling render
;; then make render more efficient (only render differences)
;; and don't forget to reset and render initial state
