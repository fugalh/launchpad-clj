(ns launchpad.core
  (:require midi)
  (:import (javax.sound.midi Receiver
                             ShortMessage)))

;;; Helpers
(defn decode-midi [msg]
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
  (bit-or red
          0xC
          (bit-shift-left green 4)))

(defn encode-midi [what where color]
  (let [vel (color->velocity color)]
    (case what
      :grid
      (midi/note-on (coord->note where) vel)
      
      :side
      (midi/note-on (coord->note [8 where]) vel)
      
      :top
      (midi/control-change (+ 0x68 where) vel))))

;; the following docblock is a work in progress. I'm still not happy with how to chain a bunch together. I'm leaning toward making versions of these that are mutating for ILaunchpad, but leaving off the annoying exclamation points... I was excited about -> but in practice the limitations still make it cumbersome.

;;; These state functions all take a GetState thing (e.g. State or Launchpad)
;;; and return a State. They don't mutate anything or cause any MIDI events.
;;; protip: use -> or as->;; (.update! pad (as-> pad $
;;                 ;; light a top button
;;                 (.top $ 3 [2 1])
;;                 ;; X marks the spot
;;                 (reduce #(.grid %1 [%2 %2] [3 0])
;;                         $
;;                         (range 8))
;;                 (reduce #(.grid %1 [%2 (- 7 %2)] [3 0])
;;                         $
;;                         (range 8))
;;                 ;; light a side button
;;                 (.side $ [0 3])))

;; (defn light
;;   "Returns a new state with what-where lit with color"
;;   [^GetState thing what where color]
;;   (let [state (.state thing)]
;;   (case what
;;     :grid
;;     (let [[x y] where]
;;       (assoc-in state [:grid x y] color))

;;     (assoc-in state [what where] color))))

;; (defn unlight
;;   "Returns a new state with what-where unlit"
;;   [thing what where]
;;   (light what where [0 0]))

;; (defn grid [thing [x y] [red green]]
;;   (light thing :grid [x y] [red green]))

;; (defn top [thing x [red green]]
;;   (light thing :top x [red green]))

;; (defn side [thing y [red green]]
;;   (light thing :side y [red green]))

;;; Protocols
(defprotocol GetState
  (state [this]))
(defprotocol IState
  (light [this what where [red green]]
    "Returns a new state with button what (:grid, :top, or :side)
    where ([x y], 0-indexed) lit with color [x y]")
  (unlight [this what where]
    "Convenience for light with color [0 0]")
  (react [this f]
    "Return a new state with the new reactor."))

(defprotocol ILaunchpad
  (update! [this state] "Update and render the state")
  (reset [this] "Reset and update state to initial-state"))

;;; Records
(defrecord State [grid top side reactor]
  GetState
  (state [this] this)
  IState
  (light [this what where color]
    (if (= :grid what)
      (let [[x y] where]
        (assoc-in this [:grid x y] color))
      (assoc-in this [what where] color)))
  (unlight [this what where]
    (.light this what where [0 0]))
  (react [this f]
    (assoc-in this [:reactor] f)))

(def initial-state
  "The quintessentially quiescent Launchpad."
  (let [black [0 0]
        row (vec (repeat 8 black))]
    (map->State {:grid (vec (repeat 8 row))
                 :top row
                 :side row
                 :reactor nil})))

(defrecord Launchpad [state midi-in midi-out]
  IState
  (light [this what where color]
    (.update! this (.light (.state this) what where color)))
  (unlight [this what where]
    (.light this what where [0 0]))
  (react [this f]
    (.update! this (.react (.state this) f)))
  GetState
  (state [this] @state)
  ILaunchpad  
  (update! [this newstate]
    ;; The Launchpad Mini, and probably therefore the S, updates quickly
    ;; enough not to warrant double-buffering here, so I haven't
    ;; implemented it. But if flickering becomes an issue, that's an option.
    (let [oldstate state]

      ;; grid
      (doall
       (for [x (range 8)
             y (range 8)]
         (let [old (get-in @oldstate [:grid x y])
               new (get-in  newstate [:grid x y])]
           (when-not (= old new)
             (.send midi-out (encode-midi :grid [x y] new) -1)))))
      ;; top and side
      (doall
       (for [what [:top :side]
             where (range 8)]
         (let [old (get-in @oldstate [what where])
               new (get-in  newstate [what where])]
           (when-not (= old new)
             (.send midi-out (encode-midi what where new) -1)))))
      (reset! state newstate)))
  (reset [this]
    (.send midi-out (midi/control-change 0 0) -1)
    (reset! state initial-state)))

;;; Factories and Prototypes

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
                 (.update! pad newstate)))))
         (catch Exception e
           (println "Reactor exception: " (.getMessage e))))))))
             


(defn make-launchpad
  "Make a LaunchpadS corresponding to the last real launchpad device listed
  in the system. NB On OSX Java has a bug where the list of MIDI devices
  never changes after the first call, so you may need to restart the JVM
  after changing MIDI configuration."
  ([]
   (make-launchpad initial-state))
  ([state]
   (make-launchpad state nil))
  ([state reactor]
   (when-let [[midi-in midi-out] (midi/open-device-duplex #"Launchpad")]
     (when-let [pad (Launchpad. (atom state) midi-in midi-out)]
       (do
         (link-midi->pad midi-in pad)
         (.reset pad)
         pad)))))
