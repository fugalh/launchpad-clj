(ns launchpad.core
  (:refer-clojure :exclude [update])
  (:require midi)
  (:import (javax.sound.midi Receiver
                             ShortMessage)))

(declare decode-midi coord->note color->velocity encode-midi)

(defprotocol IState
  (state [this]
    "Return the State.")
  (light [this what where [red green]]
    "Returns a new state with button what (:grid, :top, or :side)
    where ([x y], 0-indexed) is lit with color [red green]")
  (unlight [this what where]
    "Convenience for light with color [0 0]")
  (react [this reactor]
    "Return a new state with the new reactor, which is a function that
    takes [state what where velocity]. If it returns an IState, the
    Launchpad will update itself accordingly)."))

(defrecord State [grid top side reactor]
  IState
  ;; All of these are pure functions.
  (state [this] this)
  (light [this what where color]
    (assoc-in this [what where] color))
  (unlight [this what where]
    (.light this what where [0 0]))
  (react [this f]
    (assoc this :reactor f)))

(def initial-state
  "The quintessentially quiescent Launchpad."
  (let [black [0 0]
        row (vec (repeat 8 black))]
    (map->State {:grid {} ; [x y] -> [r g]
                 :top row
                 :side row
                 :reactor nil})))

(defprotocol ILaunchpad
  (render [this state] "Update and render the new state")
  (update [this f]
    "Update with a function which takes a Launchpad and returns a State")
  (reset [this]
    "Reset and update state to initial-state. Equivalent to
    (.render pad initial-state) but more efficient."))

(defrecord Launchpad [state midi-in midi-out]
  IState
  (state [this] @state)
  ;; These are not pure, they perform the actions, and they return the
  ;; Launchpad for easy threading (->)
  (light [this what where color]
    (.render this (.light (.state this) what where color)))
  (unlight [this what where]
    (.light this what where [0 0]))
  (react [this f]
    (swap! state #(assoc % :reactor f)))
  
  ILaunchpad
  (render [this newstate]
    ;; The Launchpad Mini, and probably therefore the S, renders quickly
    ;; enough not to warrant double-buffering here, so I haven't
    ;; implemented it. But if flickering becomes an issue, that's an
    ;; option.
    (let [oldstate (.state this)
          ;; Any IState will do.
          newstate (.state newstate)]
      ;; grid
      (doall ; because lazy seq
       (for [what [:grid]
             where (for [x (range 8) y (range 8)] [x y])]
         (let [old (get-in oldstate [what where])
               new (get-in newstate [what where])]
           (when-not (= old new)
             (.send midi-out (encode-midi what where new) -1)))))
      ;; top and side
      (doall
       (for [what [:grid :top :side]
             where (range 8)]
         (let [old (get-in oldstate [what where])
               new (get-in newstate [what where])]
           (when-not (= old new)
             (.send midi-out (encode-midi what where new) -1)))))
      (reset! state newstate)
      this))
  (update [this f]
    (.render this (f (.state this))))
  (reset [this]
    (.send midi-out (midi/control-change 0 0) -1)
    (reset! state initial-state)
    this))

(declare link-midi->pad)

(defn make-launchpad
  "Find the default launchpad MIDI device and construct a Launchpad object.
  You wouldn't want to call this multiple times; use get-launchpad instead."
  []
  (if-let [[midi-in midi-out] (midi/open-device-duplex #"Launchpad")]
    (let [pad (Launchpad. (atom initial-state) midi-in midi-out)]
      (link-midi->pad midi-in pad)
      (.reset pad)
      pad)
    (throw (RuntimeException. "Unable to open MIDI device for Launchpad"))))

(def default-launchpad
  "You probably don't want to access this directly, rather use
  get-launchpad and forget-launchpad."
  (atom nil))

(defn get-launchpad
  "Get the default Launchpad. Cached in default-launchpad after the initial
  scan. If you need to rescan, first do forget-launchpad."
  []
  (if-let [pad @default-launchpad]
    pad
    (reset! default-launchpad (make-launchpad))))

(defn forget-launchpad
  "Forget the default launchpad, so get-launchpad will rescan."
  []
  (reset! default-launchpad nil))

;;; helpers

(defn link-midi->pad
  [midi-in pad]
  (.setReceiver
   midi-in
   (reify javax.sound.midi.Receiver
     (close [this])
     (send [this msg ts]
       (try
         (let [state (.state pad)
               reactor (.reactor state)
               msg (decode-midi msg)]
           (when (and reactor msg)
             (let [newstate (apply reactor state msg)]
               (when (instance? State newstate)
                 (.render pad newstate)))))
         (catch Exception e
           (println "Reactor exception: " (.getMessage e))))))))

(defn apply-state
  "Update the Launchpad with the IState.
  Provided for convenience at the tail of a -> block, e.g.

    (-> state
        (light :grid [x y] [r g])
        (top x [r g])
        (apply-state pad))

  instead of

    (.render pad (-> (.state pad)
                     (light :grid [x y] [r g])
                     (top x [r g])))

  although usually you would just do this:

    (-> pad
        (.light :grid [x y] [r g])
        (.top x [r g]))

  or even (for the default launchpad) this:

    (do
      (light :grid [x y] [r g])
      (top x [r g]))

  which have the same effect and are only slightly less efficient than
  bulk-update."
  [state pad]
  (.render pad (.state state)))

(defn decode-midi
  "Decode a MIDI message to [what where velocity]"
  [msg]
  (when (instance? ShortMessage msg)
    (case (.getCommand msg)
      0x90 ; ShortMessage/NOTE_ON
      (let [note (.getData1 msg)
            x (bit-and 0xf note)
            y (/ (- note x) 0x10)
            vel (.getData2 msg)]
        (if (= x 8)
          [:side y vel]
          (when (<= 0 x 7)
            [:grid [x y] vel])))

      0xb0 ; ShortMessage/CONTROL_CHANGE
      (let [controller (.getData1 msg)
            vel (.getData2 msg)
            x (- controller 0x68)]
        (when (<= 0 x 7)
          [:top x vel]))

      nil)))

(defn coord->note [[x y]]
  "Convert the (x,y) coordinate to the appropriate MIDI note"
  (+ x (* 0x10 y)))

(defn color->velocity
  "Convert the (red,green) pair to the appropriate MIDI velocity"
  [[red green]]
  (let [red (if (nil? red) 0 red)
        green (if (nil? green) 0 green)]
    (bit-or red
         0xC
         (bit-shift-left green 4))))

(defn encode-midi
  "Encode [what where color] as the corresponding MIDI message"
  [what where color]
  (let [vel (color->velocity color)]
    (case what
      :grid
      (midi/note-on (coord->note where) vel)
      
      :side
      (midi/note-on (coord->note [8 where]) vel)
      
      :top
      (midi/control-change (+ 0x68 where) vel))))
