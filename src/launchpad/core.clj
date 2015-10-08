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
  (render [this newstate])
  (reset [this])
  (grid [this [x y] [red green]])
  (top [this x [red green]])
  (side [this y [red green]]))

(declare make-state)

(defrecord State [grid top side])

(defrecord Model [state device]
  ModelBehavior
  (grid [this [x y] [red green]]
    (.render this (assoc-in @(.state this)
                         [:grid x y]
                         [red green])))

  (top [this x [red green]]
    (.render this (assoc-in @(.state this)
                         [:top x]
                         [red green])))

  (side [this y [red green]]
    (.render this (assoc-in @(.state this)
                         [:side y]
                         [red green])))

  (render [this newstate]
    ;; TODO more efficient
    (reset! (.state this) newstate)
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
    (.send (.device this) (midi/control-change 0 0) -1)
    (swap! (.state this) make-state)))

(defn make-state 
  ([] (State. (vec (repeat 8 (vec (repeat 8 [0 0]))))
         (vec (repeat 8 [0 0]))
         (vec (repeat 8 [0 0]))))
  ([_] (make-state)))

(defn make-model []
  (let [model (Model. (atom (make-state))
                     (midi/get-receiver "Launchpad"))]
    (.reset model)
    model))

(defn make-test-model []
  (Model. (atom (make-state))
          (reify javax.sound.midi.Receiver
            (close [this])
            (send [this msg ts]))))

;; wip - grid/top/side in terms of updating state and calling render
;; then make render more efficient (only render differences)
;; and don't forget to reset and render initial state
