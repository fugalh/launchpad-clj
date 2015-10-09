(ns launchpad.core
  (:require midi))

;; helpers
(declare color->velocity coord->note -update!)

(defprotocol Protocol
  (reset 
    [this]
    "Reset the launchpad and update the state to match.")
  (grid 
    [this [x y] [red green]]
    "Update the grid at (x,y) to have the given color.
    0-indexing from the top-right.
    red and green range 0 to 3. Amber is achieved by combining both colors.")
  (top
    [this x [red green]]
    "Update the top row of buttons at x (0-7) to have the given color.")
  (right
    [this y [red green]]
    "Update the right column of buttons at y (0-7) to have the given color.")
  (text
    [this ascii [red green]]
    "Scroll text. Vary the speed by embedding control bytes 1-7 in the string
    (default speed is 4).")
  (loop-text
    [this ascii [red green]]
    "Scroll text looping, until stop-text")
  (stop-text [this]))

;; grid is a vector of vectors (8x8)
;; top is an 8-element vector
;; right is an 8-element vector
;;
;; In all of the above, the value is a color (2-element vector of red and green
;; values, ranging from 0 to 3)
(defrecord State [grid top right])

(def initial-state
  "The quintessentially quiescent Launchpad."
  (let [octet (vec (repeat 8 [0 0]))]
    (State. (vec (repeat 8 octet))
            octet
            octet)))

;; combine the state, the midi device, and the actions on them
(defrecord Launchpad [^clojure.lang.Atom state device]
  Protocol
  (grid [this [x y] [red green]]
    (-update! this (assoc-in @(.state this)
                             [:grid x y]
                             [red green])))

  (top [this x [red green]]
    (-update! this (assoc-in @(.state this)
                             [:top x]
                             [red green])))

  (right [this y [red green]]
    (let [state @(.state this)]
      (-update! this (assoc-in state
                               [:right y]
                               [red green]))))

  (reset [this]
    (.send (.device this)
           (midi/control-change 0 0)
           -1)
    (reset! (.state this) initial-state))

  (text [this ascii [red green]]
    ;; sysex [F0h] 00h, 20h, 29h, 09h, colour, text ..., [F7h]
    (.send
     (.device this)
     (midi/sysex (byte-array (concat [0x00 0x20 0x29 0x09]
                                     [(color->velocity [red green])]
                                     (.getBytes ascii))))
     -1))

  (loop-text [this ascii [red green]]
    ;; like text but add 64 to color (i.e. green|0x4, because green gets <<4)
    (.text this ascii [red (bit-or 0x4 green)]))

  (stop-text [this]
    (.text this "" [0 0])))

(defn new-launchpad 
  ([]
   "Make and reset a Launchpad connected to the first
   Launchpad MIDI device found."
   (let [launchpad
         (new-launchpad initial-state (midi/get-receiver "Launchpad"))]
     (.reset launchpad)
     launchpad))
  ([^State state device]
   "Make a Launchpad with given state,
   connected to the given device. It is not reset."
   (Launchpad. (atom state) device)))

;; helpers
(defn color->velocity
  "Convert the (red,green) pair to the appropriate MIDI velocity"
  [[red green]]
  (bit-or red
          0xC
          (bit-shift-left green 4)))

(defn coord->note [[x y]]
  "Convert the (x,y) coordinate to the appropriate MIDI note"
  (+ x
     (* 0x10 y)))

(defn -update!
  "Update launchpad to pad new state, wholesale."
  [pad newstate]
  ;; The Launchpad Mini, and probably therefore the S, updates quickly enough
  ;; not to warrant double-buffering here, so I haven't implemented it. But if
  ;; flickering becomes an issue, that's an option.
  (let [oldstate (.state pad)
        dev (.device pad)]
    (dotimes [x 8]
      (dotimes [y 8]
                                        ; update the grid
        (when (not= (get-in @oldstate [:grid x y])
                    (get-in newstate [:grid x y]))
          (.send dev
                 (midi/note-on (coord->note [x y])
                               (color->velocity (get-in newstate
                                                        [:grid x y])))
                 -1)))
                                        ; update the top row
      (when (not= (get-in @oldstate [:top x])
                  (get-in newstate [:top x]))
        (.send dev
               (midi/control-change (+ 0x68 x)
                                    (color->velocity (get-in newstate
                                                             [:top x])))
               -1))
                                        ; update the right side column
      (let [y x]
        (when (not= (get-in @oldstate [:right y])
                    (get-in newstate [:right y]))

          (.send dev
                 (midi/note-on (coord->note [8 y])
                               (color->velocity (get-in newstate [:right y])))
                 -1))))
    (reset! oldstate newstate)))
